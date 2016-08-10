package com.samistine.mcplugins.vpnblock;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.samistine.vpnlookup.Response;
import com.samistine.vpnlookup.ResponseHost;
import com.samistine.vpnlookup.apis.xioaxhostcheck.XioaxHostCheck;
import com.samistine.vpnlookup.exception.VPNLookupException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Samuel Seidel
 */
public final class HostLookup {

    private static final Logger logger = Logger.getLogger(HostLookup.class.getName());

    protected final Config config;
    protected final XioaxHostCheck check;
    public LoadingCache<String, Boolean> cache;

    public HostLookup(Config config) {
        this.config = config;
        this.check = new XioaxHostCheck(config.getAPIAddress(), config.getAPIKey());
        this.cache = CacheBuilder.from(config.getMemCacheSpec())
                .build(new CacheLoader<String, Boolean>() {
                    @Override
                    public Boolean load(String key) throws Exception {
                        return isAllowed(key);
                    }
                });
        if (config.isSavingCacheEnabled()) {
            loadCacheFromFile();
        }
    }

    /**
     * Takes into account
     * {@link Config#isHostWhitelisted(java.lang.String)} & {@link Config#isIPWhitelisted(java.lang.String)}
     *
     * @param ip to check
     * @return true if the ip is allowed to join
     * @throws VPNLookupException if the underlying API encounters problems
     */
    private boolean isAllowed(String ip) throws VPNLookupException {
        if (config.isIPWhitelisted(ip)) {
            Global.debug("HostLookup::isAllowed(ip) -> returning true -> " + ip + " is whitelisted");
            return true;
        }

        Response response = check.check(ip);
        boolean isHost = response.isHostIP();

        if (!isHost) {
            Global.debug("HostLookup::isAllowed(ip) -> returning true -> " + ip + " is not a host");
            return true;
        }

        if (config.isHostWhitelisted(ip)) {
            String org = ((ResponseHost) response).getHostOrganization();
            Global.debug("HostLookup::isAllowed(ip) -> returning true -> " + org + " is whitelisted");
            return true;
        }

        Global.debug("HostLookup::isAllowed(ip) -> returning false -> " + ip + " is a host");
        return false;
    }

    public boolean isAllowedCached(String ip) throws ExecutionException {
        return cache.get(ip);
    }

    public void saveCacheToFile() {
        try {
            String data = new Gson().toJson(cache.asMap());

            File file = config.getSavingCacheFile();

            if (!file.exists()) {
                file.createNewFile();
            }

            try (FileWriter fw = new FileWriter(file.getAbsoluteFile())) {
                try (BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(data);
                }
            }

            logger.info("Saved to file!");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public void loadCacheFromFile() {
        File file = config.getSavingCacheFile();
        if (file.exists() && file.isFile()) {
            try {
                String text = Files.toString(file, Charsets.UTF_8);
                Type type = new TypeToken<Map<String, Boolean>>() {
                }.getType();
                Map<String, Boolean> recoveredQuarks = new Gson().fromJson(text, type);
                System.out.println("Adding saved entries to cache.");
                cache.putAll(recoveredQuarks);
                logger.log(Level.INFO, "Loaded {0} entries into cache.", recoveredQuarks.size());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    public LoadingCache<String, Boolean> getCache() {
        return cache;
    }

}
