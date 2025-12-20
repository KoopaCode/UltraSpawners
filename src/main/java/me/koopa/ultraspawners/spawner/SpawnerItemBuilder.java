package me.koopa.ultraspawners.spawner;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpawnerItemBuilder {
    private final UltraSpawners plugin;
    private final ConfigManager configManager;

    public SpawnerItemBuilder(UltraSpawners plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public ItemStack buildSpawnerItem(EntityType entityType, int stack, int tier) {
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&f" + formatEntityName(entityType)));
            
            List<String> lore = new ArrayList<>();
            
            ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
            if (tierConfig != null) {
                lore.add(ColorUtil.color("&7Tier: &r" + tierConfig.displayName));
            } else {
                lore.add(ColorUtil.color("&7Tier: &rUnknown"));
            }
            
            lore.add(ColorUtil.color("&7Stack: &r" + stack));
            lore.add(ColorUtil.color("&7Mob Type: &r" + entityType.name()));
            
            meta.setLore(lore);
            
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(plugin.getNamespacedKey("entityType"), PersistentDataType.STRING, entityType.name());
            data.set(plugin.getNamespacedKey("stack"), PersistentDataType.INTEGER, stack);
            data.set(plugin.getNamespacedKey("tier"), PersistentDataType.INTEGER, tier);
            data.set(plugin.getNamespacedKey("dataVersion"), PersistentDataType.INTEGER, 1);
            
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack buildSpawnerItem(EntityType entityType, int stack, int tier, String owner) {
        ItemStack item = buildSpawnerItem(entityType, stack, tier);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore());
            if (owner != null && !owner.isEmpty()) {
                lore.add(ColorUtil.color("&7Owner: &r" + owner));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isSpawnerItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(plugin.getNamespacedKey("entityType"), PersistentDataType.STRING);
    }

    public EntityType getEntityType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("entityType"), PersistentDataType.STRING);
            if (name != null) {
                try {
                    return EntityType.valueOf(name);
                } catch (IllegalArgumentException e) {
                    return EntityType.ZOMBIE;
                }
            }
        }
        return EntityType.ZOMBIE;
    }

    public int getStack(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Integer stack = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("stack"), PersistentDataType.INTEGER);
            if (stack != null) {
                return stack;
            }
        }
        return 1;
    }

    public int getTier(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Integer tier = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("tier"), PersistentDataType.INTEGER);
            if (tier != null) {
                return tier;
            }
        }
        return 0;
    }

    private String formatEntityName(EntityType type) {
        String name = type.name().replace('_', ' ');
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
}
