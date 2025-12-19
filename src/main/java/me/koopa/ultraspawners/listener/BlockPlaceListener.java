package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.service.SpawnerService;
import me.koopa.ultraspawners.spawner.SpawnerItemBuilder;
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

        // Check if placing on existing spawner
        Block blockAgainst = event.getBlockAgainst();
        if (blockAgainst != null && blockAgainst.getType() == Material.SPAWNER) {
            event.setCancelled(true);
            
            // Try to merge with existing spawner
            try {
                DatabaseManager.StoredSpawner existing = plugin.getDatabaseManager().getSpawner(
                    blockAgainst.getWorld().getUID().toString(),
                    blockAgainst.getX(), blockAgainst.getY(), blockAgainst.getZ()
                );
                
                if (existing != null) {
                    EntityType placingType = itemBuilder.getEntityType(item);
                    int placingStack = itemBuilder.getStack(item);
                    int placingTier = itemBuilder.getTier(item);
                    
                    // Only merge if same mob type
                    if (existing.type.equals(placingType.name())) {
                        int maxStack = configManager.getMaxStackPerBlock();
                        int newStack = maxStack > 0 ? Math.min(existing.stack + placingStack, maxStack) : existing.stack + placingStack;
                        int newTier = Math.max(existing.tier, placingTier);
                        
                        // Update database
                        plugin.getDatabaseManager().saveSpawner(new DatabaseManager.StoredSpawner(
                            existing.world, existing.x, existing.y, existing.z,
                            existing.type, newStack, newTier, existing.owner
                        ));
                        
                        // Update hologram
                        plugin.getHologramManager().updateSpawnerHologram(
                            blockAgainst.getLocation(), existing.type, newStack, newTier
                        );
                        
                        // Remove item from inventory
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                        } else {
                            event.getPlayer().getInventory().setItemInMainHand(null);
                        }
                        
                        event.getPlayer().sendMessage("§aStacked spawner! New stack: §e" + newStack);
                    } else {
                        event.getPlayer().sendMessage("§cCannot stack different mob types!");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error merging spawner: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        event.setCancelled(false);

        if (block.getType() != Material.SPAWNER) {
            block.setType(Material.SPAWNER);
        }

        try {
            spawnerService.placeSpawner(block, item, event.getPlayer());
            
            // Create hologram
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
