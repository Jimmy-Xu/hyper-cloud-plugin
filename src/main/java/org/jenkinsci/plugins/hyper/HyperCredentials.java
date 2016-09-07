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

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class HyperCredentials extends BaseStandardCredentials {


    private final String accessKey;

    private final Secret secretKey;

    @DataBoundConstructor
    public HyperCredentials(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, String accessKey, Secret secretKey) {
        super(scope, id, description);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Secret getSecretKey() {
        return secretKey;
    }

    public static @Nonnull HyperConfigFile toConfigFile(String server, String credentialsId, ItemGroup context) throws IOException, InterruptedException {

        final HyperCredentials c = getCredentials(credentialsId, context);
        if (c != null) {
            File f = File.createTempFile("hyper", "cfg");
            f.delete();
            f.mkdir();

            ArgumentListBuilder args = new ArgumentListBuilder()
                    .add("hyper") // TODO path to Hyper CLI
                    .add("config")
                    .add("--accesskey", c.getAccessKey())
                    .add("--secretkey", Secret.toString(c.getSecretKey()))
                    .add(server);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int status = new Launcher.LocalLauncher(TaskListener.NULL).launch()
                    .envs("HYPER_CONFIG="+f.getCanonicalPath())
                    .cmds(args)
                    .join();

            if (status != 0) {
                throw new IOException("Failed to create Hyper_ configuration file. Status code "+status);
            }
            return new HyperConfigFile(f);
        }

        // Fall back to system configuration
        return new HyperConfigFile(null);
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Hyper_ Access and Secret Keys";
        }
    }

    @CheckForNull
    public static HyperCredentials getCredentials(@Nullable String credentialsId, ItemGroup context) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return (HyperCredentials) CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(HyperCredentials.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(credentialsId));
    }

}
