package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerInteractListener implements Listener {
    private final UltraSpawners plugin;

    public SpawnerInteractListener(UltraSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if right-clicking a spawner block
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.SPAWNER) {
                event.setCancelled(true);

                try {
                    DatabaseManager.StoredSpawner spawner = plugin.getDatabaseManager().getSpawner(
                        block.getWorld().getUID().toString(), 
                        block.getX(), block.getY(), block.getZ()
                    );

                    if (spawner != null) {
                        plugin.getSpawnerUpgradeGUI().open(player, block, spawner);
                    } else {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cNo spawner data found!"));
                    }
                } catch (Exception e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cError opening spawner menu!"));
                    e.printStackTrace();
                }
                return;
            }
        }
        
        // Check if right-clicking air/block with spawner item in hand
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (plugin.getSpawnerService().getItemBuilder().isSpawnerItem(item)) {
                // Only cancel and open menu if not clicking a spawner block (already handled above)
                // AND if player is sneaking (shift+right-click) to prevent opening when placing
                if ((event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER) && player.isSneaking()) {
                    event.setCancelled(true);
                    
                    // Create a temporary stored spawner from the item data
                    DatabaseManager.StoredSpawner tempSpawner = new DatabaseManager.StoredSpawner(
                        "temporary",
                        0, 0, 0,
                        plugin.getSpawnerService().getItemBuilder().getEntityType(item).name(),
                        plugin.getSpawnerService().getItemBuilder().getStack(item),
                        plugin.getSpawnerService().getItemBuilder().getTier(item),
                        player.getName()
                    );
                    
                    plugin.getSpawnerUpgradeGUI().open(player, null, tempSpawner);
                }
            }
        }
    }
}
