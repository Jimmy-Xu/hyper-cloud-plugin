/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package org.jenkinsci.plugins.hyper;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.SlaveComputer;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(HyperCloud.class.getName());

    private final String server;

    /** Credentials to connect to Hyper_ infrastructure */
    private final String credentialsId;

    private final List<HyperSlaveTemplate> templates;

    @DataBoundConstructor
    public HyperCloud(String name, @Nonnull String server, @Nonnull String credentialsId, List<HyperSlaveTemplate> templates) {
        super(name);
        this.server = StringUtils.isNotBlank(server) ? server : "tcp://us-west-1.hyper.sh:443";
        this.credentialsId = credentialsId;
        this.templates = templates;
    }

    public String getServer() {
        return server;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public List<HyperSlaveTemplate> getTemplates() {
        return templates;
    }

    @Override
    public boolean canProvision(Label label) {
        return getTemplate(label) != null;
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        try {

            List<NodeProvisioner.PlannedNode> r = new ArrayList<NodeProvisioner.PlannedNode>();

            HyperSlaveTemplate template = getTemplate(label);
            if (template == null) throw new IllegalStateException("no slave template for label "+label);

            // Provision the requested node(s) asynchronously
            for (int i = 1; i <= excessWorkload; i++) {
                r.add(new NodeProvisioner.PlannedNode(template.getDisplayName(), Computer.threadPoolForRemoting
                        .submit(new ProvisioningCallback(template, label)), 1));
            }
            return r;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to provision Hyper_ slave", e);
            return Collections.EMPTY_LIST;
        }
    }

    private @CheckForNull HyperSlaveTemplate getTemplate(Label label) {
        for (HyperSlaveTemplate t : templates) {
            if (label == null || label.matches(t.getLabelSet())) {
                return t;
            }
        }
        return null;
    }


    private class ProvisioningCallback implements Callable<Node> {

        private final HyperSlaveTemplate template;
        private final Label label;

        public ProvisioningCallback(HyperSlaveTemplate template, Label label) {
            this.template = template;
            this.label = label;
        }

        @Override
        public Node call() throws Exception {

            final String labelString = label == null ? null : label.toString();
            final String name = (label == null ? "" : labelString+"-") + Long.toHexString(System.nanoTime());

            HyperSlave slave = new HyperSlave(HyperCloud.this, name, template.getRemoteFSRoot(), labelString, new ComputerLauncher() {
                @Override
                public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
                    String rootUrl = Jenkins.getInstance().getRootUrl();

                    try (HyperConfigFile config = HyperCredentials.toConfigFile(server, credentialsId, Jenkins.getInstance())) {

                        ArgumentListBuilder args = new ArgumentListBuilder()
                                .add("hyper") // TODO path to Hyper CLI
                                .add("--config", config.getPath())
                                .add("run", "-d")
                                .add("--workdir", template.getRemoteFSRoot())
                                .add("--label", "org.jenkinsci.plugins.hyper.HyperCloud=" + labelString)
                                .add("-e", "JENKINS_URL=" + rootUrl);

                        args.add("--size",template.getSize());
                        args.add(template.getImage())
                                .add(computer.getJnlpMac())
                                .add(name);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        int status = new Launcher.LocalLauncher(listener).launch()
                                .cmds(args)
                                .stdout(out).stderr(listener.getLogger())
                                .join();

                        ((HyperSlave) computer.getNode()).setContainerId(out.toString("UTF-8"));

                        if (status != 0) {
                            throw new IOException("Failed to create Hyper_ slave container. Status code " + status);
                        }
                    }
                }

            });

            Jenkins.getInstance().addNode(slave);

            // now wait for slave to be online
            Date now = new Date();
            Date timeout = new Date(now.getTime() + 1000 * 900); // TODO make timeout configurable
            while (timeout.after(new Date())) {
                if (slave.getComputer() == null) {
                    throw new IllegalStateException(
                            "Slave " + slave.getNodeName() + " - Node was deleted, computer is null");
                }
                if (slave.getComputer().isOnline()) {
                    break;
                }
                LOGGER.log(Level.FINE, "Waiting for slave {0} to connect since {1}.",
                        new Object[] { slave.getNodeName(), now });
                Thread.sleep(1000);
            }
            if (!slave.getComputer().isOnline()) {
                LOGGER.log(Level.WARNING, "Hyper_ Slave {0} not connected since {1} seconds",
                        new Object[] { slave.getNodeName(), now });
                Jenkins.getInstance().removeNode(slave);
                throw new IllegalStateException("Timeout waiting for Hyper_ slave to connect");
            }

            return slave;
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Hyper_ Cloud";
        }

        public ListBoxModel doFillCredentialsIdItems() {
            return new StandardListBoxModel()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(HyperCredentials.class,
                                    Jenkins.getInstance(),
                                    ACL.SYSTEM,
                                    Collections.EMPTY_LIST));
        }
    }
}