package me.koopa.ultraspawners.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPrefix() {
        return config.getString("general.prefix", "&8[&5UltraSpawners&8] &r");
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    public boolean isDropOnBreak() {
        return config.getBoolean("drops.dropOnBreak", true);
    }

    public boolean isDropOnExplosion() {
        return config.getBoolean("drops.dropOnExplosion", true);
    }

    public boolean isStackingEnabled() {
        return config.getBoolean("stacking.enable", true);
    }

    public int getMaxStackPerBlock() {
        return config.getInt("stacking.maxStackPerBlock", 64);
    }

    public boolean canMergeDifferentTypes() {
        return config.getBoolean("stacking.mergeDifferentTypes", false);
    }

    public boolean isUpgradesEnabled() {
        return config.getBoolean("upgrades.enable", true);
    }

    public int getMaxTier() {
        return config.getInt("upgrades.maxTier", 4);
    }

    public String getPaymentMode() {
        return config.getString("upgrades.paymentMode", "ITEMS");
    }

    public TierConfig getTierConfig(int tier) {
        String path = "upgrades.tiers." + tier;
        if (!config.contains(path)) {
            return null;
        }

        return new TierConfig(
            config.getString(path + ".displayName", "Unknown"),
            config.getInt(path + ".vaultCost", 0),
            config.getInt(path + ".spawnDelay", 10),
            config.getInt(path + ".spawnCount", 1),
            config.getInt(path + ".nearbyEntityLimit", 16),
            config.getInt(path + ".playerRange", 16),
            config.getStringList(path + ".requiredItems")
        );
    }

    public boolean isVaultEnabled() {
        return config.getBoolean("economy.vaultEnable", false);
    }

    public String getVaultDisabledMessage() {
        return config.getString("economy.vaultDisabledMessage", 
            "&cVault is disabled or not installed. Use item payment instead.");
    }

    public int getSpawnCapPerTrigger() {
        return config.getInt("performance.spawnCapPerTrigger", 20);
    }

    public boolean isTpsGuardEnabled() {
        return config.getBoolean("performance.enableTpsGuard", true);
    }

    public double getTpsThreshold() {
        return config.getDouble("performance.tpsThreshold", 15.0);
    }

    public int getMinimumSpawnAtLowTps() {
        return config.getInt("performance.minimumSpawnAtLowTps", 1);
    }

    public String getStorageType() {
        return config.getString("storage.type", "SQLITE");
    }

    public String getSqliteFilePath() {
        return config.getString("storage.sqlite.filePath", "plugins/UltraSpawners/database.db");
    }

    public String getMysqlHost() {
        return config.getString("storage.mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("storage.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("storage.mysql.database", "ultraspawners");
    }

    public String getMysqlUsername() {
        return config.getString("storage.mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("storage.mysql.password", "password");
    }

    public boolean isMysqlUseSSL() {
        return config.getBoolean("storage.mysql.useSSL", false);
    }

    public int getMysqlMaxPoolSize() {
        return config.getInt("storage.mysql.maxPoolSize", 10);
    }

    public static class TierConfig {
        public final String displayName;
        public final int vaultCost;
        public final int spawnDelay;
        public final int spawnCount;
        public final int nearbyEntityLimit;
        public final int playerRange;
        public final List<String> requiredItems;

        public TierConfig(String displayName, int vaultCost, int spawnDelay, int spawnCount,
                         int nearbyEntityLimit, int playerRange, List<String> requiredItems) {
            this.displayName = displayName;
            this.vaultCost = vaultCost;
            this.spawnDelay = spawnDelay;
            this.spawnCount = spawnCount;
            this.nearbyEntityLimit = nearbyEntityLimit;
            this.playerRange = playerRange;
            this.requiredItems = requiredItems;
        }
    }
}
