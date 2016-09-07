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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Set;
import java.util.logging.Logger;

public class HyperSlaveTemplate extends AbstractDescribableImpl<HyperSlaveTemplate> {

    private static final Logger LOGGER = Logger.getLogger(HyperSlaveTemplate.class.getName());

    /** White-space separated list of {@link hudson.model.Node} labels. */
    private final String label;

    /** Docker (jenkins jnlp slave) image */
    private final String image;

    /** Hyper_ Container size */
    private final String size;

    /** Slave remote FS */
    private final String remoteFSRoot;

    @DataBoundConstructor
    public HyperSlaveTemplate(String label, String image, String size, String remoteFSRoot) {
        this.label = label;
        this.image = image;
        this.size = size;
        this.remoteFSRoot = remoteFSRoot;
    }

    public String getLabel() {
        return label;
    }

    public String getImage() {
        return image;
    }

    public String getSize() {return size; }

    public String getRemoteFSRoot() {
        return remoteFSRoot;
    }

    public String getDisplayName() {
        return "Hyper_ Slave " + label;
    }

    public Set<LabelAtom> getLabelSet() {
        return Label.parse(label);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HyperSlaveTemplate> {

        @Override
        public String getDisplayName() {
            return "Hyper_ container template";
        }

        public ListBoxModel doFillSizeItems(@QueryParameter String size) {
            return new ListBoxModel(
                    new Option("S1 $0.0000004/sec ($0.00144/hour): 64MB Mem, 1 CPU Core, 10GB Disk", "s1", size.matches("s1")),
                    new Option("S2 $0.0000006/sec ($0.00216/hour): 128MB Mem, 1 CPU Core, 10GB Disk", "s2", size.matches("s2")),
                    new Option("S3 $0.000001/sec ($0.0036/hour): 256MB Mem, 1 CPU Core, 10GB Disk", "s3", size.matches("s3")),
                    new Option("S4 $0.000002/sec ($0.0072/hour): 512MB Mem, 1 CPU Core, 10GB Disk", "s4", size.matches("s4")),
                    new Option("M1 $0.000004/sec ($0.0144/hour): 1GB Mem, 1 CPU Core, 10GB Disk", "m1", size.matches("m1")),
                    new Option("M2 $0.000008/sec ($0.0288/hour): 2GB Mem, 2 CPU Core, 10GB Disk", "m2", size.matches("m2")),
                    new Option("M3 $0.000015/sec ($0.054/hour): 4GB Mem, 2 CPU Core, 10GB Disk", "m3", size.matches("m3")),
                    new Option("L1 $0.00003/sec ($0.108/hour): 4GB Mem, 4 CPU Core, 10GB Disk", "l1", size.matches("l1")),
                    new Option("L2 $0.00006/sec ($0.216/hour): 8GB Mem, 4 CPU Core, 10GB Disk", "l2", size.matches("l2")),
                    new Option("L3 $0.00012/sec ($0.432/hour): 16GB Mem, 8 CPU Core, 10GB Disk", "l3", size.matches("l3"))
            );
        }
    }
}
