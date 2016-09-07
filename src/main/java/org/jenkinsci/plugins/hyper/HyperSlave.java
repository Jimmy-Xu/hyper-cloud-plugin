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

import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

public class HyperSlave extends AbstractCloudSlave {

    private static final Logger LOGGER = Logger.getLogger(HyperSlave.class.getName());

    private static final long serialVersionUID = 1L;
    private String containerId;

    public HyperSlave(@Nonnull String name, @Nullable String remoteFS, @Nullable String labelString, @Nonnull ComputerLauncher launcher) throws Descriptor.FormException, IOException {
        super(name, "ECS slave", remoteFS, 1, Mode.EXCLUSIVE, labelString, launcher, RetentionStrategy.NOOP, Collections.EMPTY_LIST);
    }


    @Override
    public AbstractCloudComputer createComputer() {
        return new HyperComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        if (containerId != null) {
            ArgumentListBuilder args = new ArgumentListBuilder()
                    .add("hyper") // TODO path to Hyper CLI
                    // .add("--config", "...")
                    .add("rm", "-v", "-f")
                    .add(containerId);
            int status = new Launcher.LocalLauncher(listener).launch()
                    .cmds(args)
                    .stdout(listener.getLogger())
                    .join();

            if (status != 0) {
                throw new IOException("Failed to remove Hyper_ slave container "+ containerId +". Status code " + status);
            }
        }
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }
}
