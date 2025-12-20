package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.service.SpawnerService;
import me.koopa.ultraspawners.spawner.SpawnerItemBuilder;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {
    private final UltraSpawners plugin;
    private final SpawnerService spawnerService;
    private final ConfigManager configManager;

    public BlockPlaceListener(UltraSpawners plugin, SpawnerService spawnerService, ConfigManager configManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.configManager = configManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Block block = event.getBlockPlaced();

        SpawnerItemBuilder itemBuilder = spawnerService.getItemBuilder();
        if (!itemBuilder.isSpawnerItem(item)) {
            return;
        }

        // Check if placing directly on spawner
        Block blockAgainst = event.getBlockAgainst();
        if (blockAgainst != null && blockAgainst.getType() == Material.SPAWNER) {
            event.setCancelled(true);
            boolean merged = spawnerService.mergeSpawnerAt(blockAgainst, item, event.getPlayer());
            if (!merged) {
                event.getPlayer().sendMessage(ColorUtil.color("&cCould not stack spawner here."));
            }
            return;
        }

        // Check within 2 blocks for same-type spawners to auto-merge
        Block nearbySpawner = spawnerService.findNearbySpawnerForMerge(block.getLocation(), item);
        if (nearbySpawner != null) {
            event.setCancelled(true);
            boolean merged = spawnerService.mergeSpawnerAt(nearbySpawner, item, event.getPlayer());
            if (!merged) {
                event.getPlayer().sendMessage(ColorUtil.color("&cCould not auto-merge with nearby spawner."));
            }
            return;
        }

        event.setCancelled(false);

        if (block.getType() != Material.SPAWNER) {
            block.setType(Material.SPAWNER);
        }

        try {
            spawnerService.placeSpawner(block, item, event.getPlayer());
            
            EntityType type = itemBuilder.getEntityType(item);
            int stack = itemBuilder.getStack(item);
            int tier = itemBuilder.getTier(item);
            
            plugin.getHologramManager().createSpawnerHologram(
                block.getLocation(), type.name(), stack, tier
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Error placing spawner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
