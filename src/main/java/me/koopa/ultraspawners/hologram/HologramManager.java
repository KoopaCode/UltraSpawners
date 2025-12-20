package me.koopa.ultraspawners.hologram;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramManager {
    private final UltraSpawners plugin;
    private final ConfigManager configManager;
    private final Map<String, UUID> holograms = new HashMap<>();

    public HologramManager(UltraSpawners plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void createSpawnerHologram(Location location, String mobType, int stack, int tier) {
        removeHologram(location);

        String key = getLocationKey(location);
        Location holoLoc = location.clone().add(0.5, 2.0, 0.5);

        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setMarker(true);
        hologram.setCustomNameVisible(true);
        hologram.setInvulnerable(true);
        hologram.setPersistent(true);

        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
        String tierName = tierConfig != null ? tierConfig.displayName : "§7Tier " + tier;

        String displayName = String.format("§6§l%s Spawner\n§7Stack: §e§l%,d §8§l| %s\n§7§oRight click to upgrade",
            formatMobName(mobType), stack, tierName);
        
        hologram.setCustomName(displayName);

        holograms.put(key, hologram.getUniqueId());
    }

    public void updateSpawnerHologram(Location location, String mobType, int stack, int tier) {
        createSpawnerHologram(location, mobType, stack, tier);
    }

    public void removeHologram(Location location) {
        String key = getLocationKey(location);
        UUID holoId = holograms.remove(key);
        
        if (holoId != null && location.getWorld() != null) {
            location.getWorld().getEntities().stream()
                .filter(e -> e.getUniqueId().equals(holoId))
                .forEach(e -> e.remove());
        }
    }

    public void removeAllHolograms() {
        for (UUID holoId : holograms.values()) {
            Bukkit.getWorlds().forEach(world -> 
                world.getEntities().stream()
                    .filter(e -> e.getUniqueId().equals(holoId))
                    .forEach(e -> e.remove())
            );
        }
        holograms.clear();
    }

    private String getLocationKey(Location loc) {
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
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
}
