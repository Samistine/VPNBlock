package com.samistine.mcplugins.vpnblock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 *
 * @author Samuel Seidel
 */
public class VPNBlockBungee extends Plugin implements Listener {
    
    private HostLookup lookup;
    private Config config;
    
    @Override
    public void onEnable() {
        try {
            this.config = new Config.BungeeConfig(this);
        } catch (IOException ex) {
            Logger.getLogger(VPNBlockBungee.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.lookup = new HostLookup(config);
        
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new Command("vpnblock") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission("vpnblock.admin")) {
                    sender.sendMessage(ChatColor.RED + "No Permissions");
                    return;
                }
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                if (args.length >= 1) {
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            config.reload();
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
                    sender.sendMessages(
                            ChatColor.AQUA + "VPNBlock Commands:",
                            ChatColor.GOLD + " - reload - reloads data from config",
                            ChatColor.GOLD + " - whitelist - whitelist ips/hosts"
                    );
                }
            }
        });
        
        getProxy().getScheduler().schedule(this, () -> lookup.saveCacheToFile(), config.getSaveInterval(), TimeUnit.SECONDS);
    }
    
    @Override
    public void onDisable() {
        if (config.isSavingCacheEnabled()) {
            lookup.saveCacheToFile();
        }
    }
    
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();
        
        try {
            boolean allowJoin = lookup.isAllowedCached(ip);
            if (!allowJoin) {
                getLogger().log(Level.INFO, "{0} is attempting to connect from a VPN, KICKING! IP:{1}", new Object[]{player.getName(), ip});
                player.disconnect(config.getKickMessage());
            } else {
                Global.debug(player.getName() + " has been allowed to connect from " + ip);
            }
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }
    
}
