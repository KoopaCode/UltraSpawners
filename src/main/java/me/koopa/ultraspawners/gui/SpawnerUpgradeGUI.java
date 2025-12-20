package me.koopa.ultraspawners.gui;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpawnerUpgradeGUI implements Listener {
    private final UltraSpawners plugin;
    private final Map<UUID, GuiSession> sessions = new HashMap<>();

    public SpawnerUpgradeGUI(UltraSpawners plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, Block spawnerBlock, DatabaseManager.StoredSpawner spawner) {
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.color("&6&lSpawner Upgrade Station"));

        // Info section
        inv.setItem(4, createInfoItem(spawner));
        
        // Current tier indicator
        inv.setItem(13, createCurrentTierItem(spawner));

        // Upgrade options
        int nextTier = spawner.tier + 1;
        if (nextTier <= plugin.getConfigManager().getMaxTier()) {
            inv.setItem(22, createUpgradeItem(nextTier));
        } else {
            inv.setItem(22, createMaxTierItem());
        }

        // Stats display
        inv.setItem(20, createStatsItem(spawner.tier, "§aCurrent Stats"));
        if (nextTier <= plugin.getConfigManager().getMaxTier()) {
            inv.setItem(24, createStatsItem(nextTier, "§6Next Tier Stats"));
        }

        // Item collection slots (bottom two rows for payment items)
        for (int i = 36; i < 54; i++) {
            inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Upgrade button
        if (nextTier <= plugin.getConfigManager().getMaxTier()) {
            inv.setItem(49, createUpgradeButton());
        }

        // Close button
        inv.setItem(45, createCloseButton());

        // Border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i : new int[]{0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,37,38,39,40,41,42,43,44,46,47,48,50,51,52,53}) {
            inv.setItem(i, border);
        }

        // Add session BEFORE opening inventory to prevent race condition
        sessions.put(player.getUniqueId(), new GuiSession(spawnerBlock, spawner));
        player.openInventory(inv);
    }

    private ItemStack createInfoItem(DatabaseManager.StoredSpawner spawner) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtil.color("&6&l" + formatMobName(spawner.type) + " Spawner"));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtil.color("&7Mob Type: &f" + formatMobName(spawner.type)));
        lore.add(ColorUtil.color("&7Stack Amount: &e" + String.format("%,d", spawner.stack)));
        lore.add(ColorUtil.color("&7Current Tier: &b" + spawner.tier));
        if (spawner.owner != null) {
            lore.add(ColorUtil.color("&7Owner: &f" + spawner.owner));
        }
        lore.add("");
        lore.add(ColorUtil.color("&8World: " + spawner.world));
        lore.add(ColorUtil.color("&8Location: " + spawner.x + ", " + spawner.y + ", " + spawner.z));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentTierItem(DatabaseManager.StoredSpawner spawner) {
        ConfigManager.TierConfig config = plugin.getConfigManager().getTierConfig(spawner.tier);
        
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        String tierName = config != null ? config.displayName : "&7Tier " + spawner.tier;
        meta.setDisplayName(ColorUtil.color(tierName + " &7(Current)"));
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeItem(int tier) {
        ConfigManager.TierConfig config = plugin.getConfigManager().getTierConfig(tier);
        
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        String tierName = config != null ? config.displayName : "&7Tier " + tier;
        meta.setDisplayName(ColorUtil.color("&e&lUpgrade to " + tierName));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtil.color("&7Click the button below to upgrade!"));
        lore.add("");
        
        if (config != null && !config.requiredItems.isEmpty()) {
            lore.add(ColorUtil.color("&6Required Items:"));
            for (String itemStr : config.requiredItems) {
                String[] parts = itemStr.split(":");
                if (parts.length == 2) {
                    lore.add(ColorUtil.color("  &7• &f" + parts[1] + "x &e" + formatMaterialName(parts[0])));
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaxTierItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtil.color("&c&lMAX TIER REACHED"));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtil.color("&7This spawner is at maximum tier!"));
        lore.add(ColorUtil.color("&7No further upgrades available."));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(int tier, String title) {
        ConfigManager.TierConfig config = plugin.getConfigManager().getTierConfig(tier);
        
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(title);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (config != null) {
            lore.add(ColorUtil.color("&7Spawn Delay: &f" + config.spawnDelay + " ticks"));
            lore.add(ColorUtil.color("&7Spawn Count: &f" + config.spawnCount + " per cycle"));
            lore.add(ColorUtil.color("&7Entity Limit: &f" + config.nearbyEntityLimit));
            lore.add(ColorUtil.color("&7Player Range: &f" + config.playerRange + " blocks"));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtil.color("&a&lCLICK TO UPGRADE"));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtil.color("&7Place required items in your"));
        lore.add(ColorUtil.color("&7inventory, then click here!"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtil.color("&c&lCLOSE"));
        meta.setLore(Arrays.asList("", ColorUtil.color("&7Click to close this menu")));
        
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(ColorUtil.color("&6&lSpawner Upgrade Station"))) return;

        event.setCancelled(true);

        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        int slot = event.getRawSlot();

        // Close button
        if (slot == 45) {
            player.closeInventory();
            return;
        }

        // Upgrade button
        if (slot == 49) {
            performUpgrade(player, session);
        }
    }

    private void performUpgrade(Player player, GuiSession session) {
        int currentTier = session.spawner.tier;
        int nextTier = currentTier + 1;

        if (nextTier > plugin.getConfigManager().getMaxTier()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cSpawner is already at max tier!"));
            return;
        }

        ConfigManager.TierConfig config = plugin.getConfigManager().getTierConfig(nextTier);
        if (config == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cInvalid tier configuration!"));
            return;
        }

        // Check and consume items
        Map<Material, Integer> required = new HashMap<>();
        for (String itemStr : config.requiredItems) {
            String[] parts = itemStr.split(":");
            if (parts.length == 2) {
                try {
                    Material mat = Material.valueOf(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    required.put(mat, amount);
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid item in config: " + itemStr);
                }
            }
        }

        // Check if player has items
        List<String> missingItems = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                missingItems.add(entry.getValue() + "x " + formatMaterialName(entry.getKey().name()));
            }
        }
        
        if (!missingItems.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cYou need: " + String.join(", ", missingItems)));
            return;
        }

        // Consume items
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            ItemStack toRemove = new ItemStack(entry.getKey(), entry.getValue());
            player.getInventory().removeItem(toRemove);
        }

        // Upgrade spawner
        try {
            // Only save to database if it's a placed spawner (not hand-held)
            if (session.block != null) {
                plugin.getDatabaseManager().saveSpawner(new DatabaseManager.StoredSpawner(
                    session.spawner.world, session.spawner.x, session.spawner.y, session.spawner.z,
                    session.spawner.type, session.spawner.stack, nextTier, session.spawner.owner
                ));

                plugin.getHologramManager().updateSpawnerHologram(
                    session.block.getLocation(), session.spawner.type, session.spawner.stack, nextTier
                );
                
                player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&aSpawner upgraded to tier " + nextTier + "!"));
            } else {
                // Hand-held spawner - can't upgrade, viewing only
                player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cYou can only upgrade placed spawners!"));
                player.closeInventory();
                return;
            }
            
            player.closeInventory();
            
            DatabaseManager.StoredSpawner updated = plugin.getDatabaseManager().getSpawner(
                session.spawner.world, session.spawner.x, session.spawner.y, session.spawner.z
            );
            if (updated != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    open(player, session.block, updated);
                }, 1L);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + ColorUtil.color("&cError upgrading spawner!"));
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ColorUtil.color("&6&lSpawner Upgrade Station"))) {
            sessions.remove(event.getPlayer().getUniqueId());
        }
    }

    private String formatMobName(String mobType) {
        String name = mobType.replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatMaterialName(String material) {
        return formatMobName(material);
    }

    private static class GuiSession {
        Block block;
        DatabaseManager.StoredSpawner spawner;

        GuiSession(Block block, DatabaseManager.StoredSpawner spawner) {
            this.block = block;
            this.spawner = spawner;
        }
    }
}
