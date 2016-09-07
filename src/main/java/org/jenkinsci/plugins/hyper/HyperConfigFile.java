package org.jenkinsci.plugins.hyper;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class HyperConfigFile implements AutoCloseable {


    private final File config;

    public HyperConfigFile(File config) {
        this.config = config;
    }

    @Override
    public void close() throws IOException {
        if (config != null) {
            boolean ok = config.delete();
            if (!ok) throw new IOException("Failed to delete Hyper_ config file "+config.getPath());
        }
    }

    public String getPath() {
        if (config == null)
            return System.getProperty("user.home") + "/.hyper/config.json";

        return config.getPath();
    }
}
