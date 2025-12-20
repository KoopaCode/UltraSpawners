package me.koopa.ultraspawners.gui;

import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.service.SpawnerService;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class UpgradeMenu {
    private final JavaPlugin plugin;
    private final SpawnerService spawnerService;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;

    public UpgradeMenu(JavaPlugin plugin, SpawnerService spawnerService, 
                       ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    public void open(Player player, Block spawnerBlock) throws Exception {
        DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
            spawnerBlock.getWorld().getUID().toString(), spawnerBlock.getX(), 
            spawnerBlock.getY(), spawnerBlock.getZ()
        );

        if (spawner == null) {
            player.sendMessage(configManager.getPrefix() + "&cNo spawner data found!");
            return;
        }

        int maxTier = configManager.getMaxTier();
        int inventorySize = ((maxTier + 2) / 9) * 9;
        if (inventorySize < 9) inventorySize = 9;

        Inventory inv = Bukkit.createInventory(null, inventorySize, ColorUtil.color("&5Spawner Upgrades"));

        for (int tier = 0; tier <= maxTier; tier++) {
            ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
            if (tierConfig == null) continue;

            ItemStack icon = new ItemStack(Material.EMERALD);
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(tierConfig.displayName);

                List<String> lore = new ArrayList<>();
                if (tier <= spawner.tier) {
                    lore.add(ColorUtil.color("&7Status: &aOwned"));
                } else {
                    lore.add(ColorUtil.color("&7Status: &6Available"));
                    if ("VAULT".equals(configManager.getPaymentMode()) || "BOTH".equals(configManager.getPaymentMode())) {
                        if (configManager.isVaultEnabled()) {
                            lore.add(ColorUtil.color("&7Cost: &6$" + tierConfig.vaultCost));
                        }
                    }
                    if ("ITEMS".equals(configManager.getPaymentMode()) || "BOTH".equals(configManager.getPaymentMode())) {
                        for (String item : tierConfig.requiredItems) {
                            lore.add(ColorUtil.color("&7Need: &r" + item));
                        }
                    }
                }

                lore.add("");
                lore.add(ColorUtil.color("&7Spawn Delay: &r" + tierConfig.spawnDelay));
                lore.add(ColorUtil.color("&7Spawn Count: &r" + tierConfig.spawnCount));
                lore.add(ColorUtil.color("&7Entity Limit: &r" + tierConfig.nearbyEntityLimit));
                lore.add(ColorUtil.color("&7Player Range: &r" + tierConfig.playerRange));

                meta.setLore(lore);
                icon.setItemMeta(meta);
            }

            inv.setItem(tier, icon);
        }

        player.openInventory(inv);
    }
}
