package me.koopa.ultraspawners.util;

import me.koopa.ultraspawners.UltraSpawners;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VersionChecker {
    private static final String VERSION_URL = "https://raw.githubusercontent.com/KoopaCode/UltraSpawners/refs/heads/main/versions/modrinth";
    private final UltraSpawners plugin;
    private final String currentVersion;

    public VersionChecker(UltraSpawners plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<VersionEntry> versions = fetchVersions();
                if (versions.isEmpty()) {
                    plugin.getLogger().warning("Failed to check for updates - no versions found");
                    return;
                }

                VersionEntry latestVersion = versions.get(0);
                
                if (!currentVersion.equals(latestVersion.version)) {
                    plugin.getLogger().warning("═══════════════════════════════════════════════");
                    plugin.getLogger().warning("  UPDATE AVAILABLE!");
                    plugin.getLogger().warning("  Current: " + currentVersion + " → Latest: " + latestVersion.version);
                    plugin.getLogger().warning("  " + latestVersion.changelog);
                    plugin.getLogger().warning("  Download: https://modrinth.com/plugin/spawners");
                    plugin.getLogger().warning("═══════════════════════════════════════════════");
                } else {
                    plugin.getLogger().info("You are running the latest version!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private List<VersionEntry> fetchVersions() throws Exception {
        List<VersionEntry> versions = new ArrayList<>();
        
        URL url = new URL(VERSION_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "UltraSpawners/" + currentVersion);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            String currentVersionNumber = null;
            StringBuilder changelogBuilder = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    // If we have a version and changelog, add it
                    if (currentVersionNumber != null && changelogBuilder.length() > 0) {
                        versions.add(new VersionEntry(currentVersionNumber, changelogBuilder.toString().trim()));
                        currentVersionNumber = null;
                        changelogBuilder = new StringBuilder();
                    }
                    continue;
                }
                
                // Check if this is a version number (format: X.X.X)
                if (line.matches("^\\d+\\.\\d+\\.\\d+.*")) {
                    // Save previous version if exists
                    if (currentVersionNumber != null && changelogBuilder.length() > 0) {
                        versions.add(new VersionEntry(currentVersionNumber, changelogBuilder.toString().trim()));
                    }
                    
                    // Extract version number and optional changelog on same line
                    String[] parts = line.split("\\s+", 2);
                    currentVersionNumber = parts[0];
                    changelogBuilder = new StringBuilder();
                    
                    if (parts.length > 1) {
                        // Remove parentheses if present
                        String changelog = parts[1];
                        if (changelog.startsWith("(") && changelog.endsWith(")")) {
                            changelog = changelog.substring(1, changelog.length() - 1);
                        }
                        changelogBuilder.append(changelog);
                    }
                } else if (currentVersionNumber != null) {
                    // This is a changelog line for the current version
                    if (changelogBuilder.length() > 0) {
                        changelogBuilder.append(" ");
                    }
                    changelogBuilder.append(line);
                }
            }
            
            // Add last version
            if (currentVersionNumber != null && changelogBuilder.length() > 0) {
                versions.add(new VersionEntry(currentVersionNumber, changelogBuilder.toString().trim()));
            }
        }

        return versions;
    }

    public UpdateInfo getUpdateInfo() {
        try {
            List<VersionEntry> versions = fetchVersions();
            if (versions.isEmpty()) {
                return null;
            }

            VersionEntry latestVersion = versions.get(0);
            
            if (!currentVersion.equals(latestVersion.version)) {
                return new UpdateInfo(
                    plugin.getDescription().getName(),
                    currentVersion,
                    latestVersion.version,
                    latestVersion.changelog
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
        }
        return null;
    }

    public static class UpdateInfo {
        public final String pluginName;
        public final String currentVersion;
        public final String latestVersion;
        public final String changelog;

        public UpdateInfo(String pluginName, String currentVersion, String latestVersion, String changelog) {
            this.pluginName = pluginName;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.changelog = changelog;
        }
    }

    private static class VersionEntry {
        final String version;
        final String changelog;

        VersionEntry(String version, String changelog) {
            this.version = version;
            this.changelog = changelog;
        }
    }
}
