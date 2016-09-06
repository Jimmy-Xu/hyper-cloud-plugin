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
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Set;

public class HyperSlaveTemplate extends AbstractDescribableImpl<HyperSlaveTemplate> {

    private static final Logger LOGGER = Logger.getLogger(HyperSlaveTemplate.class.getName());

    /** White-space separated list of {@link hudson.model.Node} labels. */
    private final String label;

    /** Docker (jenkins jnlp slave) image */
    private final String image;

    /** Slave remote FS */
    private final String remoteFSRoot;

    @DataBoundConstructor
    public HyperSlaveTemplate(String label, String image, String remoteFSRoot) {
        this.label = label;
        this.image = image;
        this.remoteFSRoot = remoteFSRoot;
    }

    public String getLabel() {
        return label;
    }

    public String getImage() {
        return image;
    }

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
    }
}
