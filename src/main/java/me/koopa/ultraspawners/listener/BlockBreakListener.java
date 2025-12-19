package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.service.SpawnerService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final UltraSpawners plugin;
    private final SpawnerService spawnerService;
    private final ConfigManager configManager;

    public BlockBreakListener(UltraSpawners plugin, SpawnerService spawnerService, ConfigManager configManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.configManager = configManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        try {
            spawnerService.breakSpawner(event.getBlock());
            plugin.getHologramManager().removeHologram(event.getBlock().getLocation());
        } catch (Exception e) {
            plugin.getLogger().severe("Error breaking spawner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
