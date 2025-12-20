package me.koopa.ultraspawners.service;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.spawner.SpawnerItemBuilder;
import me.koopa.ultraspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class SpawnerService {
    private final UltraSpawners plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final VaultHook vaultHook;
    private final SpawnerItemBuilder itemBuilder;

    public SpawnerService(UltraSpawners plugin, DatabaseManager databaseManager, 
                         ConfigManager configManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.vaultHook = vaultHook;
        this.itemBuilder = new SpawnerItemBuilder(plugin, configManager);
    }

    public void placeSpawner(Block block, ItemStack item, Player player) throws SQLException {
        if (!itemBuilder.isSpawnerItem(item)) {
            return;
        }

        EntityType type = itemBuilder.getEntityType(item);
        int stack = itemBuilder.getStack(item);
        int tier = itemBuilder.getTier(item);

        if (block.getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(type);
            spawner.setDelay(getSpawnDelay(tier));
            spawner.update();
        }

        DatabaseManager.StoredSpawner existing = databaseManager.getSpawner(
            block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ()
        );
        if (existing != null) {
            // Merge logic
            if (configManager.isStackingEnabled()) {
                String existingType = existing.type;
                boolean sameType = existingType.equals(type.name());

                if (sameType || configManager.canMergeDifferentTypes()) {
                    int existingTier = existing.tier;
                    int placingTier = tier;

                    // Apply configured merge rgules
                    if (configManager.isMergeSameTierOnly() && existingTier != placingTier) {
                        // Do not merge if tie must match
                        return;
                    }

                    int maxStack = configManager.getMaxStackPerBlock();
                    int newStack = maxStack > 0 ? Math.min(existing.stack + stack, maxStack) : existing.stack + stack;

                    int newTier = existingTier;
                    if (configManager.isPreferHigherTierOnMerge()) {
                        newTier = Math.max(existingTier, placingTier);
                    }

                    databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                        existing.world, existing.x, existing.y, existing.z,
                        sameType ? existingType : type.name(), newStack, newTier, existing.owner
                    ));

                    return;
                }
            }
        } else {
            databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ(),
                type.name(), stack, tier, player != null ? player.getName() : null
            ));
        }
    }

    public boolean mergeSpawnerAt(Block existingBlock, ItemStack item, Player player) {
        try {
            if (!itemBuilder.isSpawnerItem(item)) return false;

            DatabaseManager.StoredSpawner existing = databaseManager.getSpawner(
                existingBlock.getWorld().getUID().toString(),
                existingBlock.getX(), existingBlock.getY(), existingBlock.getZ()
            );
            if (existing == null) return false;

            EntityType placingType = itemBuilder.getEntityType(item);
            int placingStack = itemBuilder.getStack(item);
            int placingTier = itemBuilder.getTier(item);

            String existingType = existing.type;
            boolean sameType = existingType.equals(placingType.name());

            if (!sameType && !configManager.canMergeDifferentTypes()) {
                return false;
            }

            if (configManager.isMergeSameTierOnly() && existing.tier != placingTier) {
                return false;
            }

            int maxStack = configManager.getMaxStackPerBlock();
            int newStack = maxStack > 0 ? Math.min(existing.stack + placingStack, maxStack) : existing.stack + placingStack;
            int newTier = configManager.isPreferHigherTierOnMerge() ? Math.max(existing.tier, placingTier) : existing.tier;

            databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                existing.world, existing.x, existing.y, existing.z,
                sameType ? existingType : placingType.name(), newStack, newTier, existing.owner
            ));

            // Update hologram
            plugin.getHologramManager().updateSpawnerHologram(existingBlock.getLocation(), sameType ? existingType : placingType.name(), newStack, newTier);

            // Consume one item from player's hand
            if (player != null) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null) {
                    if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                    else player.getInventory().setItemInMainHand(null);
                }
                player.sendMessage(ColorUtil.color("&aStacked spawner! New stack: &e" + newStack));
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error merging spawner: " + e.getMessage());
            return false;
        }
    }

    public Block findNearbySpawnerForMerge(org.bukkit.Location placeLoc, ItemStack item) {
        if (!itemBuilder.isSpawnerItem(item)) return null;
        
        EntityType placingType = itemBuilder.getEntityType(item);
        int searchRadius = 2;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    Block checkBlock = placeLoc.clone().add(x, y, z).getBlock();
                    if (checkBlock.getType() == Material.SPAWNER) {
                        try {
                            DatabaseManager.StoredSpawner stored = databaseManager.getSpawner(
                                checkBlock.getWorld().getUID().toString(),
                                checkBlock.getX(), checkBlock.getY(), checkBlock.getZ()
                            );
                            if (stored != null && stored.type.equals(placingType.name())) {
                                return checkBlock;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return null;
    }

    public void breakSpawner(Block block) throws SQLException {
        if (block.getType() != Material.SPAWNER) {
            return;
        }

        if (!configManager.isDropOnBreak()) {
            databaseManager.deleteSpawner(block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ());
            return;
        }

        DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
            block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ()
        );

        EntityType spawnedType = EntityType.ZOMBIE;
        if (spawner != null) {
            try {
                spawnedType = EntityType.valueOf(spawner.type);
            } catch (IllegalArgumentException e) {
                spawnedType = EntityType.ZOMBIE;
            }
            ItemStack item = itemBuilder.buildSpawnerItem(spawnedType, spawner.stack, spawner.tier, spawner.owner);
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        } else {
            // Try to get type from the spawner block itself
            if (block.getState() instanceof CreatureSpawner cs) {
                spawnedType = cs.getSpawnedType();
            }
            ItemStack item = itemBuilder.buildSpawnerItem(spawnedType, 1, 0);
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }

        databaseManager.deleteSpawner(block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ());
    }

    public int upgradeSpawner(Block block, Player player) throws SQLException {
        DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
            block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ()
        );

        if (spawner == null) {
            return -1;
        }

        int currentTier = spawner.tier;
        int maxTier = configManager.getMaxTier();

        if (currentTier >= maxTier) {
            return -2;
        }

        int nextTier = currentTier + 1;
        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(nextTier);

        if (tierConfig == null) {
            return -3;
        }

        if (configManager.isVaultEnabled() && vaultHook.isEnabled()) {
            if (!vaultHook.canAfford(player, tierConfig.vaultCost)) {
                return -4;
            }
            vaultHook.withdraw(player, tierConfig.vaultCost);
        } else if ("VAULT".equals(configManager.getPaymentMode())) {
            player.sendMessage(ColorUtil.color("&cVault is not enabled. Use item payment instead."));
            return -5;
        }

        databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
            spawner.world, spawner.x, spawner.y, spawner.z,
            spawner.type, spawner.stack, nextTier, spawner.owner
        ));

        return nextTier;
    }

    public int getSpawnDelay(int tier) {
        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
        if (tierConfig != null) {
            return tierConfig.spawnDelay;
        }
        return 10;
    }

    public int getSpawnCount(int tier) {
        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
        if (tierConfig != null) {
            return tierConfig.spawnCount;
        }
        return 1;
    }

    public int getNearbyEntityLimit(int tier) {
        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
        if (tierConfig != null) {
            return tierConfig.nearbyEntityLimit;
        }
        return 16;
    }

    public int getPlayerRange(int tier) {
        ConfigManager.TierConfig tierConfig = configManager.getTierConfig(tier);
        if (tierConfig != null) {
            return tierConfig.playerRange;
        }
        return 16;
    }

    public SpawnerItemBuilder getItemBuilder() {
        return itemBuilder;
    }

    public boolean shouldScaleSpawns() {
        if (!configManager.isTpsGuardEnabled()) {
            return false;
        }
        double[] tps = Bukkit.getServer().getTPS();
        return tps[0] < configManager.getTpsThreshold();
    }

    public int getScaledSpawnCount(int baseCount) {
        if (!shouldScaleSpawns()) {
            return baseCount;
        }
        int scaled = Math.max(configManager.getMinimumSpawnAtLowTps(), baseCount / 2);
        return Math.min(scaled, configManager.getSpawnCapPerTrigger());
    }

    public int getSpawnCapForWorld(String worldName) {
        int worldCap = configManager.getWorldSpawnCap(worldName);
        return worldCap >= 0 ? worldCap : configManager.getSpawnCapPerTrigger();
    }

    public int getScaledSpawnCount(int baseCount, String worldName) {
        int cap = getSpawnCapForWorld(worldName);
        if (!shouldScaleSpawns()) {
            return Math.min(baseCount, cap);
        }
        int scaled = Math.max(configManager.getMinimumSpawnAtLowTps(), baseCount / 2);
        return Math.min(scaled, cap);
    }
}
