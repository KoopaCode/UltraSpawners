package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.service.SpawnerService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoadListener implements Listener {
    private final UltraSpawners plugin;
    private final SpawnerService spawnerService;
    private final DatabaseManager databaseManager;

    public ChunkLoadListener(UltraSpawners plugin, SpawnerService spawnerService, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        
        // Recreate holograms for spawners in this chunk
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof CreatureSpawner spawner && state.getBlock().getType() == Material.SPAWNER) {
                try {
                    DatabaseManager.StoredSpawner data = databaseManager.getSpawner(
                        state.getWorld().getUID().toString(),
                        state.getX(), state.getY(), state.getZ()
                    );
                    
                    if (data != null) {
                        plugin.getHologramManager().createSpawnerHologram(
                            state.getLocation(), data.type, data.stack, data.tier
                        );
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading hologram for spawner at " + 
                        state.getX() + ", " + state.getY() + ", " + state.getZ());
                }
            }
        }
    }
}
