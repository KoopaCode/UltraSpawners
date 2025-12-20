package com.modrinth.statsapi;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Privacy-first metrics client for Bukkit plugins.
 * 
 * Usage:
 * <pre>
 * StatsAPI metrics = new StatsAPI(this, "YourPluginName");
 * metrics.start();
 * </pre>
 */
public class StatsAPI implements Listener {
    
    private static final String API_URL = "https://stats.hadescfx.studio/v1/metrics";
    private static final int REPORT_INTERVAL_TICKS = 36105; // 30 minutes 5 seconds (1805 seconds * 20 ticks/sec)
    private static final Gson GSON = new Gson();
    
    private final JavaPlugin plugin;
    private final String productName;
    private final File installIdFile;
    private final AtomicInteger playersSeen;
    
    private String installId;
    private int taskId = -1;
    
    /**
     * Creates a new StatsAPI instance.
     *
     * @param plugin Your plugin instance
     * @param productName Your product name (alphanumeric, underscores, hyphens only)
     */
    public StatsAPI(JavaPlugin plugin, String productName) {
        this.plugin = plugin;
        this.productName = productName;
        this.installIdFile = new File(plugin.getDataFolder(), "statsapi-install-id.txt");
        this.playersSeen = new AtomicInteger(0);
    }
    
    /**
     * Starts the metrics collection.
     * Call this in your onEnable() method.
     */
    public void start() {
        // Load or generate install ID
        this.installId = loadOrGenerateInstallId();
        
        // Register player join listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Send initial metrics
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::sendMetrics);
        
        // Schedule periodic metrics
        this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin, 
            this::sendMetrics, 
            REPORT_INTERVAL_TICKS, 
            REPORT_INTERVAL_TICKS
        ).getTaskId();
        
        plugin.getLogger().info("StatsAPI started for " + productName);
    }
    
    /**
     * Stops the metrics collection.
     * Call this in your onDisable() method.
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        
        // Send final metrics on shutdown
        if (installId != null) {
            sendMetricsSync();
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playersSeen.incrementAndGet();
    }
    
    private String loadOrGenerateInstallId() {
        // Try to load existing install ID
        if (installIdFile.exists()) {
            try {
                String id = new String(Files.readAllBytes(installIdFile.toPath()), StandardCharsets.UTF_8).trim();
                if (isValidUUID(id)) {
                    plugin.getLogger().info("Loaded existing install ID");
                    return id;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to read install ID file", e);
            }
        }
        
        // Generate new install ID
        String newId = UUID.randomUUID().toString();
        
        try {
            plugin.getDataFolder().mkdirs();
            Files.write(installIdFile.toPath(), newId.getBytes(StandardCharsets.UTF_8));
            plugin.getLogger().info("Generated new install ID");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save install ID", e);
        }
        
        return newId;
    }
    
    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private void sendMetrics() {
        try {
            int players = playersSeen.get();
            
            MetricsPayload payload = new MetricsPayload();
            payload.installId = installId;
            payload.product = productName;
            payload.productVersion = plugin.getDescription().getVersion();
            payload.playersSeen = players;
            
            String json = GSON.toJson(payload);
            
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "StatsAPI-Bukkit/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                // Success - reset counter
                playersSeen.set(0);
                plugin.getLogger().fine("Metrics sent successfully");
            } else if (responseCode == 429) {
                // Rate limited - keep counter
                plugin.getLogger().fine("Rate limited by metrics server");
            } else {
                // Error - keep counter for retry
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    String errorMsg = reader.readLine();
                    plugin.getLogger().warning("Metrics error: " + errorMsg);
                }
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send metrics", e);
        }
    }
    
    private void sendMetricsSync() {
        sendMetrics();
    }
    
    private static class MetricsPayload {
        String installId;
        String product;
        String productVersion;
        int playersSeen;
    }
}
