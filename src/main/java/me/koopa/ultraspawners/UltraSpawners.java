package me.koopa.ultraspawners;

import com.modrinth.statsapi.StatsAPI;
import me.koopa.ultraspawners.command.CommandHandler;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.hologram.HologramManager;
import me.koopa.ultraspawners.hologram.MobStackManager;
import me.koopa.ultraspawners.listener.BlockBreakListener;
import me.koopa.ultraspawners.listener.BlockPlaceListener;
import me.koopa.ultraspawners.listener.ChunkLoadListener;
import me.koopa.ultraspawners.listener.PlayerJoinListener;
import me.koopa.ultraspawners.listener.SpawnerListener;
import me.koopa.ultraspawners.listener.SpawnerInteractListener;
import me.koopa.ultraspawners.service.SpawnerService;
import me.koopa.ultraspawners.service.VaultHook;
import me.koopa.ultraspawners.util.VersionChecker;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class UltraSpawners extends JavaPlugin {
    private static UltraSpawners instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private SpawnerService spawnerService;
    private VaultHook vaultHook;
    private HologramManager hologramManager;
    private MobStackManager mobStackManager;
    private VersionChecker versionChecker;
    private StatsAPI metrics;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize metrics - MANDATORY
        try {
            metrics = new StatsAPI(this, "UltraSpawners");
            metrics.start();
        } catch (Exception e) {
            getLogger().warning("Metrics failed: " + e.getMessage());
        }
        
        // Display banner
        displayBanner();
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize database
        try {
            databaseManager = new DatabaseManager(this, configManager);
            databaseManager.initialize();
            getLogger().info("Database initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize Vault hook
        vaultHook = new VaultHook(this, configManager);
        vaultHook.hook();
        
        // Initialize hologram manager
        hologramManager = new HologramManager(this, configManager);
        
        // Initialize mob stack manager
        mobStackManager = new MobStackManager(this);
        Bukkit.getPluginManager().registerEvents(mobStackManager, this);
        
        // Initialize spawner service
        spawnerService = new SpawnerService(this, databaseManager, configManager, vaultHook);
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this, spawnerService, configManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this, spawnerService, configManager), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(this, spawnerService, databaseManager), this);
        Bukkit.getPluginManager().registerEvents(new SpawnerListener(this, spawnerService, configManager), this);
        Bukkit.getPluginManager().registerEvents(new SpawnerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // Register command handler
        CommandHandler commandHandler = new CommandHandler(this, spawnerService, configManager, databaseManager);
        var cmd = getCommand("ultraspawners");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("Failed to register command 'ultraspawners'");
        }
        
        getLogger().info("UltraSpawners enabled successfully");
        
        // Check for updates
        versionChecker = new VersionChecker(this);
        versionChecker.checkForUpdates();
    }

    @Override
    public void onDisable() {
        // Stop metrics
        if (metrics != null) {
            try {
                metrics.stop();
            } catch (Exception ignored) {}
        }
        
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (mobStackManager != null) {
            mobStackManager.cleanup();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("UltraSpawners disabled");
    }

    public static UltraSpawners getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SpawnerService getSpawnerService() {
        return spawnerService;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public MobStackManager getMobStackManager() {
        return mobStackManager;
    }

    public VersionChecker getVersionChecker() {
        return versionChecker;
    }

    public NamespacedKey getNamespacedKey(String key) {
        return new NamespacedKey(this, key);
    }

    private void displayBanner() {
        String version = getDescription().getVersion();
        getLogger().info("╔═════════════════════════════════════════════════════════════════════════╗");
        getLogger().info("║                                                                         ║");
        getLogger().info("║     ██╗   ██╗██╗  ████████╗██████╗  █████╗                              ║");
        getLogger().info("║     ██║   ██║██║  ╚══██╔══╝██╔══██╗██╔══██╗                             ║");
        getLogger().info("║     ██║   ██║██║     ██║   ██████╔╝███████║                             ║");
        getLogger().info("║     ██║   ██║██║     ██║   ██╔══██╗██╔══██║                             ║");
        getLogger().info("║     ╚██████╔╝███████╗██║   ██║  ██║██║  ██║                             ║");
        getLogger().info("║      ╚═════╝ ╚══════╝╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝                             ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("║        ███████╗██████╗  █████╗ ██╗    ██╗███╗   ██╗███████╗██████╗      ║");
        getLogger().info("║        ██╔════╝██╔══██╗██╔══██╗██║    ██║████╗  ██║██╔════╝██╔══██╗     ║");
        getLogger().info("║        ███████╗██████╔╝███████║██║ █╗ ██║██╔██╗ ██║█████╗  ██████╔╝     ║");
        getLogger().info("║        ╚════██║██╔═══╝ ██╔══██║██║███╗██║██║╚██╗██║██╔══╝  ██╔══██╗     ║");
        getLogger().info("║        ███████║██║     ██║  ██║╚███╔███╔╝██║ ╚████║███████╗██║  ██║     ║");
        getLogger().info("║        ╚══════╝╚═╝     ╚═╝  ╚═╝ ╚══╝╚══╝ ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝     ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("║   ═══════════════════════════════════════════════════════════════════   ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("║                 Advanced Spawner Management System                      ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("║                     Version: " + String.format("%-20s", version) + "                       ║");
        getLogger().info("║                     Author: Koopa                                       ║");
        getLogger().info("║                     Modrinth: modrinth.com/plugin/spawners              ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("║   ═══════════════════════════════════════════════════════════════════   ║");
        getLogger().info("║                                                                         ║");
        getLogger().info("╚═════════════════════════════════════════════════════════════════════════╝");
    }
}
