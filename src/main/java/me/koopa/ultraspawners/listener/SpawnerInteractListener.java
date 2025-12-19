package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.gui.SpawnerUpgradeGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpawnerInteractListener implements Listener {
    private final UltraSpawners plugin;

    public SpawnerInteractListener(UltraSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }

        Player player = event.getPlayer();
        
        // Don't open menu if player is placing a block
        if (player.getInventory().getItemInMainHand().getType().isBlock() && 
            player.getInventory().getItemInMainHand().getType() != Material.SPAWNER) {
            return;
        }

        event.setCancelled(true);

        try {
            DatabaseManager.StoredSpawner spawner = plugin.getDatabaseManager().getSpawner(
                block.getWorld().getUID().toString(), 
                block.getX(), block.getY(), block.getZ()
            );

            if (spawner != null) {
                SpawnerUpgradeGUI gui = new SpawnerUpgradeGUI(plugin);
                gui.open(player, block, spawner);
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNo spawner data found!");
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cError opening spawner menu!");
            e.printStackTrace();
        }
    }
}
