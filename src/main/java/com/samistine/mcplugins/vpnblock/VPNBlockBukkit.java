package com.samistine.mcplugins.vpnblock;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Samuel Seidel
 */
public class VPNBlockBukkit extends JavaPlugin implements Listener {

    private HostLookup lookup;
    private Config config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new Config.BukkitConfig(this);
        this.lookup = new HostLookup(config);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("vpnblock").setExecutor(this);

        getServer().getScheduler().runTaskTimer(this, () -> lookup.saveCacheToFile(), config.getSaveInterval() * 20, config.getSaveInterval() * 20);
    }

    @Override
    public void onDisable() {
        if (config.isSavingCacheEnabled()) {
            lookup.saveCacheToFile();
        }
    }

    @EventHandler
    protected void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) { //Only check for players that are allowed to join
            try {
                String ip = event.getAddress().getHostAddress();
                boolean allowJoin = lookup.isAllowedCached(ip);
                if (!allowJoin) {
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage(config.getKickMessage());
                    getLogger().log(Level.INFO, "{0} is attempting to connect from a VPN, BLOCKING! IP:{1}", new Object[]{event.getName(), ip});
                } else {
                    Global.debug(event.getName() + " has been allowed to connect from " + ip);
                }
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadConfig();
                    sender.sendMessage(ChatColor.GOLD + "Config Reloaded!");
                    break;
                case "whitelist":
                    switch (args[1].toLowerCase()) {
                        case "list":
                            sender.sendMessage(ChatColor.AQUA + "Whitelisted IPs:");
                            sender.sendMessage(Arrays.toString(config.getWhitelistedIPs().toArray()));
                            sender.sendMessage(ChatColor.AQUA + "Whitelisted Hostss:");
                            sender.sendMessage(Arrays.toString(config.getWhitelistedHosts().toArray()));
                            break;
                        case "add":
                            if (Global.validIP(args[2])) {
                                //Handle IP Address
                                List<String> ips = config.getWhitelistedIPs();
                                ips.add(args[2]);
                                config.set("IP Whitelist", ips);
                                lookup.getCache().refresh(args[2]);
                            } else {
                                //Handle Host
                                List<String> hosts = config.getWhitelistedHosts();
                                hosts.add(args[2]);
                                config.set("Host Whitelist", hosts);
                            }
                            break;
                        case "remove":
                            if (Global.validIP(args[2])) {
                                //Handle IP Address
                                List<String> ips = config.getWhitelistedIPs();
                                ips.remove(args[2]);
                                config.set("IP Whitelist", ips);
                                lookup.getCache().refresh(args[2]);
                            } else {
                                //Handle Host
                                List<String> hosts = config.getWhitelistedHosts();
                                hosts.remove(args[2]);
                                config.set("IP Whitelist", hosts);
                            }
                            break;
                    }
                    break;
            }
        } else {
            sender.sendMessage(new String[]{
                ChatColor.AQUA + "VPNBlock Commands:",
                ChatColor.GOLD + " - reload - reloads data from config",
                ChatColor.GOLD + " - whitelist - whitelist ips/hosts"
            });
        }
        return true;
    }

}
