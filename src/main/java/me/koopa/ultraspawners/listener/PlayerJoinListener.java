package me.koopa.ultraspawners.listener;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.util.VersionChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final UltraSpawners plugin;

    public PlayerJoinListener(UltraSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is OP or has * permission
        if (!player.isOp() && !player.hasPermission("*")) {
            return;
        }

        // Check for updates asynchronously after 2 seconds
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            VersionChecker versionChecker = plugin.getVersionChecker();
            if (versionChecker == null) {
                return;
            }

            VersionChecker.UpdateInfo updateInfo = versionChecker.getUpdateInfo();
            if (updateInfo != null) {
                // Send update notification to player
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§6⚠ UPDATE AVAILABLE §6⚠");
                    player.sendMessage("");
                    player.sendMessage("§7New update available for §e" + updateInfo.pluginName);
                    player.sendMessage("§7Current Version: §c" + updateInfo.currentVersion + " §7→ Latest: §a" + updateInfo.latestVersion);
                    player.sendMessage("");
                    player.sendMessage("§7What's New:");
                    player.sendMessage("§f  • §e" + updateInfo.changelog);
                    player.sendMessage("");
                    player.sendMessage("§7Download: §b§nmodrinth.com/plugin/spawners");
                    player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                });
            }
        }, 40L); // 2 seconds delay
    }
}
