package com.samistine.mcplugins.vpnblock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Samuel Seidel
 */
public abstract class Config {

    public final String getAPIAddress() {
        return getString("API Address", "http://check.getipintel.net/check.php");
    }

    public final String getAPIKey() {
        return getString("API Key", null);
    }

    /**
     * Should we allow players to join if we are unable to get a response from
     * the api?
     *
     * @return true if we should
     */
    public final boolean getFallbackJoinBehavior() {
        return getBoolean("Fallback Join Behavior", true);
    }

    public final String getKickMessage() {
        return getString("Kick Message", null);
    }

    public String getMemCacheSpec() {
        return getString("Mem Cache Spec", "concurrencyLevel=2, initialCapacity=500, maximumSize=10000, expireAfterAccess=7d");
    }

    public final boolean isSavingCacheEnabled() {
        return getBoolean("Save Lookups To File", true);
    }

    public File getSavingCacheFile() {
        return new File(getString("Cache File", "plugins" + File.separator + "VPNBlock" + File.separator + "iplookup.cache"));
    }

    public final long getSaveInterval() {
        return getLong("Save Interval", 600);
    }

    public final List<String> getWhitelistedIPs() {
        return getStringList("IP Whitelist");
    }

    public final List<String> getWhitelistedHosts() {
        return getStringList("Host Whitelist");
    }

    public boolean isIPWhitelisted(String IP) {
        return getWhitelistedIPs().contains(IP);
    }

    public boolean isHostWhitelisted(String organization) {
        return getWhitelistedHosts().stream().anyMatch(allowed -> organization.contains(allowed));
    }

    public abstract boolean getBoolean(String path, boolean def);

    public abstract int getInt(String path, int def);

    public abstract double getDouble(String path, double def);

    public abstract long getLong(String path, long def);

    public abstract String getString(String path, String def);

    public abstract List<String> getStringList(String path);

    public abstract void set(String path, Object value);

    public abstract void reload();

    public static class BukkitConfig extends Config {

        private final org.bukkit.plugin.Plugin plugin;

        public BukkitConfig(org.bukkit.plugin.Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            return plugin.getConfig().getBoolean(path, def);
        }

        @Override
        public int getInt(String path, int def) {
            return plugin.getConfig().getInt(path, def);
        }

        @Override
        public double getDouble(String path, double def) {
            return plugin.getConfig().getDouble(path, def);
        }

        @Override
        public long getLong(String path, long def) {
            return plugin.getConfig().getLong(path, def);
        }

        @Override
        public String getString(String path, String def) {
            return plugin.getConfig().getString(path, def);
        }

        @Override
        public List<String> getStringList(String path) {
            return plugin.getConfig().getStringList(path);
        }

        @Override
        public void set(String path, Object value) {
            plugin.getConfig().set(path, value);
        }

        @Override
        public void reload() {
            plugin.reloadConfig();
            Logger.getLogger(getClass().getName()).log(Level.INFO, "Config reloaded");
        }

    }

    public static class BungeeConfig extends Config {

        private final net.md_5.bungee.api.plugin.Plugin plugin;
        private net.md_5.bungee.config.Configuration configuration;

        public BungeeConfig(net.md_5.bungee.api.plugin.Plugin plugin) throws IOException {
            this.plugin = plugin;
            configuration = net.md_5.bungee.config.ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(loadResource(plugin, "config.yml"));
        }

        private File loadResource(net.md_5.bungee.api.plugin.Plugin plugin, String resource) {
            File folder = plugin.getDataFolder();
            if (!folder.exists()) {
                folder.mkdir();
            }
            File resourceFile = new File(folder, resource);
            try {
                if (!resourceFile.exists()) {
                    resourceFile.createNewFile();
                    try (InputStream in = plugin.getResourceAsStream(resource); OutputStream out = new FileOutputStream(resourceFile)) {
                        com.google.common.io.ByteStreams.copy(in, out);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
            return resourceFile;
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            return configuration.getBoolean(path, def);
        }

        @Override
        public int getInt(String path, int def) {
            return configuration.getInt(path, def);
        }

        @Override
        public double getDouble(String path, double def) {
            return configuration.getDouble(path, def);
        }

        @Override
        public long getLong(String path, long def) {
            return configuration.getLong(path, def);
        }

        @Override
        public String getString(String path, String def) {
            return configuration.getString(path, def);
        }

        @Override
        public List<String> getStringList(String path) {
            return configuration.getStringList(path);
        }

        @Override
        public void set(String path, Object value) {
            configuration.set(path, value);
        }

        @Override
        public void reload() {
            try {
                configuration = net.md_5.bungee.config.ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(loadResource(plugin, "config.yml"));
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Config reloaded");
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error occurred while reloaded config", ex);
            }
        }

    }

}
