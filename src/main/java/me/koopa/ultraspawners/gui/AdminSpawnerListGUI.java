package me.koopa.ultraspawners.gui;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminSpawnerListGUI implements Listener {
    private final UltraSpawners plugin;

    public AdminSpawnerListGUI(UltraSpawners plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§c§lAdmin: All Spawners (Page " + (page + 1) + ")");

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT world, x, y, z, type, stack, tier, owner FROM ultraspawners_spawners " +
                 "ORDER BY stack DESC, tier DESC LIMIT " + (page * 45) + ", 45")) {

            int slot = 0;
            while (rs.next() && slot < 45) {
                String world = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String type = rs.getString("type");
                int stack = rs.getInt("stack");
                int tier = rs.getInt("tier");
                String owner = rs.getString("owner");

                ItemStack item = createSpawnerItem(world, x, y, z, type, stack, tier, owner);
                inv.setItem(slot++, item);
            }

            // Navigation
            if (page > 0) {
                inv.setItem(45, createPreviousButton());
            }
            
            // Check if there are more pages
            try (ResultSet countRs = stmt.executeQuery(
                "SELECT COUNT(*) as total FROM ultraspawners_spawners")) {
                if (countRs.next()) {
                    int total = countRs.getInt("total");
                    if ((page + 1) * 45 < total) {
                        inv.setItem(53, createNextButton());
                    }
                }
            }

            // Stats
            inv.setItem(49, createStatsItem());

            // Close button
            inv.setItem(48, createCloseButton());

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading spawner list: " + e.getMessage());
            e.printStackTrace();
        }

        player.openInventory(inv);
    }

    private ItemStack createSpawnerItem(String world, int x, int y, int z, 
                                       String type, int stack, int tier, String owner) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6" + formatMobName(type) + " Spawner");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Location: §f" + x + ", " + y + ", " + z);
        lore.add("§7World: §f" + getWorldName(world));
        lore.add("§7Stack: §e" + String.format("%,d", stack));
        lore.add("§7Tier: §b" + tier);
        if (owner != null) {
            lore.add("§7Owner: §f" + owner);
        }
        lore.add("");
        lore.add("§8Click to teleport");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§e§lSpawner Statistics");

        List<String> lore = new ArrayList<>();
        lore.add("");

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {

            // Total spawners
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM ultraspawners_spawners")) {
                if (rs.next()) {
                    lore.add("§7Total Spawners: §f" + rs.getInt("total"));
                }
            }

            // Total stack
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(stack) as total FROM ultraspawners_spawners")) {
                if (rs.next()) {
                    lore.add("§7Total Stack: §e" + String.format("%,d", rs.getInt("total")));
                }
            }

            // Top mob type
            try (ResultSet rs = stmt.executeQuery(
                "SELECT type, COUNT(*) as count FROM ultraspawners_spawners GROUP BY type ORDER BY count DESC LIMIT 1")) {
                if (rs.next()) {
                    lore.add("§7Most Common: §f" + formatMobName(rs.getString("type")));
                }
            }

        } catch (Exception e) {
            lore.add("§cError loading stats");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreviousButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l← Previous Page");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lNext Page →");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lClose");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§c§lAdmin: All Spawners")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Extract page number from title
        int page = 0;
        try {
            String pageStr = title.substring(title.lastIndexOf("(Page ") + 6, title.lastIndexOf(")"));
            page = Integer.parseInt(pageStr) - 1;
        } catch (Exception e) {
            // Ignore
        }

        // Navigation
        if (slot == 45 && page > 0) {
            open(player, page - 1);
            return;
        }
        
        if (slot == 53) {
            open(player, page + 1);
            return;
        }

        if (slot == 48) {
            player.closeInventory();
            return;
        }

        // Teleport to spawner
        if (slot < 45 && clicked.getType() == Material.SPAWNER) {
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore != null && lore.size() > 1) {
                try {
                    String locLine = lore.get(1).replace("§7Location: §f", "");
                    String worldLine = lore.get(2).replace("§7World: §f", "");
                    
                    String[] coords = locLine.split(", ");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);

                    org.bukkit.World world = Bukkit.getWorlds().stream()
                        .filter(w -> w.getName().equals(worldLine))
                        .findFirst()
                        .orElse(null);

                    if (world != null) {
                        player.teleport(new org.bukkit.Location(world, x + 0.5, y + 1, z + 0.5));
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§aTeleported to spawner!");
                        player.closeInventory();
                    } else {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§cWorld not found!");
                    }
                } catch (Exception e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cError teleporting!");
                }
            }
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

    private String getWorldName(String uuid) {
        return Bukkit.getWorlds().stream()
            .filter(w -> w.getUID().toString().equals(uuid))
            .map(org.bukkit.World::getName)
            .findFirst()
            .orElse("Unknown");
    }
}
