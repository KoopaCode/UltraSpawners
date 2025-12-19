package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.service.SpawnerService;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.block.BlockExplodeEvent;

public class SpawnerListener implements Listener {
    private final UltraSpawners plugin;
    private final SpawnerService spawnerService;
    private final ConfigManager configManager;

    public SpawnerListener(UltraSpawners plugin, SpawnerService spawnerService, ConfigManager configManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.configManager = configManager;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity mob) {
            // Stack the mob after a short delay to allow it to spawn properly
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (mob.isValid() && !mob.isDead()) {
                    plugin.getMobStackManager().stackMob(mob);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!configManager.isDropOnExplosion()) {
            return;
        }

        event.blockList().stream()
            .filter(block -> block.getType() == Material.SPAWNER)
            .forEach(block -> {
                try {
                    spawnerService.breakSpawner(block);
                    plugin.getHologramManager().removeHologram(block.getLocation());
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in explosion handler: " + e.getMessage());
                }
            });
    }
}
