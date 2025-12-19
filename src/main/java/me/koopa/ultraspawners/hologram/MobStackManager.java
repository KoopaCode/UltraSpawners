package me.koopa.ultraspawners.hologram;

import me.koopa.ultraspawners.UltraSpawners;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class MobStackManager implements Listener {
    private final UltraSpawners plugin;
    private final Map<UUID, MobStack> stacks = new HashMap<>();

    public MobStackManager(UltraSpawners plugin) {
        this.plugin = plugin;
    }

    public void stackMob(LivingEntity entity) {
        if (!shouldStack(entity)) {
            return;
        }

        // Check for nearby similar mobs to stack with
        entity.getNearbyEntities(5, 5, 5).stream()
            .filter(e -> e instanceof LivingEntity)
            .map(e -> (LivingEntity) e)
            .filter(e -> canStackWith(entity, e))
            .findFirst()
            .ifPresentOrElse(
                nearby -> mergeIntoStack(entity, nearby),
                () -> createNewStack(entity)
            );
    }

    private void createNewStack(LivingEntity entity) {
        if (stacks.containsKey(entity.getUniqueId())) {
            return;
        }

        MobStack stack = new MobStack(entity, 1);
        stacks.put(entity.getUniqueId(), stack);
        updateStackDisplay(entity, 1);
        
        entity.setMetadata("ultraspawners_stacked", new FixedMetadataValue(plugin, true));
    }

    private void mergeIntoStack(LivingEntity entity, LivingEntity target) {
        MobStack targetStack = stacks.get(target.getUniqueId());
        if (targetStack == null) {
            targetStack = new MobStack(target, 1);
            stacks.put(target.getUniqueId(), targetStack);
        }

        targetStack.amount++;
        updateStackDisplay(target, targetStack.amount);
        entity.remove();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) {
            return;
        }

        MobStack stack = stacks.get(mob.getUniqueId());
        if (stack != null && stack.amount > 1) {
            // Calculate damage
            double damage = event.getFinalDamage();
            double newHealth = mob.getHealth() - damage;
            
            if (newHealth <= 0) {
                event.setCancelled(true);
                
                // Kill one from the stack
                stack.amount--;
                updateStackDisplay(mob, stack.amount);
                
                // Drop loot
                dropStackLoot(mob);
                
                // Reset health
                mob.setHealth(mob.getMaxHealth());
                
                if (stack.amount <= 0) {
                    stacks.remove(mob.getUniqueId());
                    mob.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        MobStack stack = stacks.remove(event.getEntity().getUniqueId());
        if (stack != null && stack.amount > 1) {
            event.setDroppedExp(event.getDroppedExp() * stack.amount);
        }
    }

    private void updateStackDisplay(LivingEntity entity, int amount) {
        if (amount <= 1) {
            entity.setCustomNameVisible(false);
            return;
        }

        String name = String.format("§e%s §7x%d", formatEntityName(entity.getType()), amount);
        entity.setCustomName(name);
        entity.setCustomNameVisible(true);
    }

    private void dropStackLoot(LivingEntity entity) {
        Location loc = entity.getLocation();
        Collection<ItemStack> drops = entity.getEquipment() != null 
            ? Arrays.asList(entity.getEquipment().getArmorContents()) 
            : Collections.emptyList();
        
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType().isItem()) {
                loc.getWorld().dropItemNaturally(loc, drop);
            }
        }
    }

    private boolean shouldStack(LivingEntity entity) {
        if (entity instanceof Player || entity instanceof ArmorStand) {
            return false;
        }
        
        if (entity.hasMetadata("ultraspawners_stacked")) {
            return false;
        }

        return !entity.isCustomNameVisible() || entity.getCustomName() == null;
    }

    private boolean canStackWith(LivingEntity entity1, LivingEntity entity2) {
        if (entity1.getType() != entity2.getType()) {
            return false;
        }

        if (!stacks.containsKey(entity2.getUniqueId())) {
            return false;
        }

        MobStack stack = stacks.get(entity2.getUniqueId());
        return stack.amount < 100; // Max stack size
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

    public void cleanup() {
        stacks.clear();
    }

    private static class MobStack {
        LivingEntity entity;
        int amount;

        MobStack(LivingEntity entity, int amount) {
            this.entity = entity;
            this.amount = amount;
        }
    }
}
