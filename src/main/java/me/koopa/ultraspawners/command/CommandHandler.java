package me.koopa.ultraspawners.command;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import me.koopa.ultraspawners.database.DatabaseManager;
import me.koopa.ultraspawners.gui.AdminSpawnerListGUI;
import me.koopa.ultraspawners.gui.SpawnerUpgradeGUI;
import me.koopa.ultraspawners.gui.UpgradeMenu;
import me.koopa.ultraspawners.service.SpawnerService;
import me.koopa.ultraspawners.spawner.SpawnerItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final UltraSpawners plugin;
    private final SpawnerService spawnerService;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;

    public CommandHandler(UltraSpawners plugin, SpawnerService spawnerService, 
                         ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.spawnerService = spawnerService;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "reload" -> handleReload(sender, args);
            case "give" -> handleGive(sender, args);
            case "inspect" -> handleInspect(sender, args);
            case "settype" -> handleSetType(sender, args);
            case "setstack" -> handleSetStack(sender, args);
            case "settier" -> handleSetTier(sender, args);
            case "menu" -> handleMenu(sender, args);
            case "list" -> handleList(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "give", "inspect", "settype", "setstack", "settier", "menu", "list")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (args[0].equalsIgnoreCase("settype")) {
                return Arrays.stream(EntityType.values())
                    .filter(EntityType::isSpawnable)
                    .map(EntityType::name)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(EntityType.values())
                .filter(EntityType::isSpawnable)
                .map(EntityType::name)
                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        try {
            configManager.reloadConfig();
            sender.sendMessage(configManager.getPrefix() + "§aConfig reloaded!");
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError reloading config!");
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.give")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /ultraspawners give <player> <type> [stack] [tier]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid entity type!");
            return true;
        }

        int stack = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        int tier = args.length > 4 ? Integer.parseInt(args[4]) : 0;

        stack = Math.min(stack, configManager.getMaxStackPerBlock());
        tier = Math.min(tier, configManager.getMaxTier());

        ItemStack item = spawnerService.getItemBuilder().buildSpawnerItem(type, stack, tier);
        target.getInventory().addItem(item);

        sender.sendMessage(configManager.getPrefix() + "§aGave " + stack + "x " + type.name() + " (Tier " + tier + ") to " + target.getName());
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.use")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            sender.sendMessage("§cYou must look at a spawner!");
            return true;
        }

        try {
            DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
                target.getWorld().getUID().toString(), target.getX(), target.getY(), target.getZ()
            );

            if (spawner == null) {
                sender.sendMessage(configManager.getPrefix() + "§cNo spawner data found!");
                return true;
            }

            sender.sendMessage(configManager.getPrefix() + "§aSpawner Info:");
            sender.sendMessage("  §7Type: §r" + spawner.type);
            sender.sendMessage("  §7Stack: §r" + spawner.stack);
            sender.sendMessage("  §7Tier: §r" + spawner.tier);
            if (spawner.owner != null) {
                sender.sendMessage("  §7Owner: §r" + spawner.owner);
            }
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError inspecting spawner!");
        }
        return true;
    }

    private boolean handleSetType(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.edit")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ultraspawners settype <type>");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            sender.sendMessage("§cYou must look at a spawner!");
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid entity type!");
            return true;
        }

        try {
            DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
                target.getWorld().getUID().toString(), target.getX(), target.getY(), target.getZ()
            );

            if (spawner == null) {
                sender.sendMessage(configManager.getPrefix() + "§cNo spawner found!");
                return true;
            }

            databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                spawner.world, spawner.x, spawner.y, spawner.z,
                type.name(), spawner.stack, spawner.tier, spawner.owner
            ));

            plugin.getHologramManager().updateSpawnerHologram(
                target.getLocation(), type.name(), spawner.stack, spawner.tier
            );

            sender.sendMessage(configManager.getPrefix() + "§aType set to " + type.name());
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError setting type!");
        }
        return true;
    }

    private boolean handleSetStack(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.edit")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ultraspawners setstack <amount>");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            sender.sendMessage("§cYou must look at a spawner!");
            return true;
        }

        int stack;
        try {
            stack = Integer.parseInt(args[1]);
            stack = Math.min(stack, configManager.getMaxStackPerBlock());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number!");
            return true;
        }

        try {
            DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
                target.getWorld().getUID().toString(), target.getX(), target.getY(), target.getZ()
            );

            if (spawner == null) {
                sender.sendMessage(configManager.getPrefix() + "§cNo spawner found!");
                return true;
            }

            databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                spawner.world, spawner.x, spawner.y, spawner.z,
                spawner.type, stack, spawner.tier, spawner.owner
            ));

            plugin.getHologramManager().updateSpawnerHologram(
                target.getLocation(), spawner.type, stack, spawner.tier
            );

            sender.sendMessage(configManager.getPrefix() + "§aStack set to " + stack);
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError setting stack!");
        }
        return true;
    }

    private boolean handleSetTier(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.edit")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ultraspawners settier <tier>");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            sender.sendMessage("§cYou must look at a spawner!");
            return true;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[1]);
            tier = Math.min(tier, configManager.getMaxTier());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number!");
            return true;
        }

        try {
            DatabaseManager.StoredSpawner spawner = databaseManager.getSpawner(
                target.getWorld().getUID().toString(), target.getX(), target.getY(), target.getZ()
            );

            if (spawner == null) {
                sender.sendMessage(configManager.getPrefix() + "§cNo spawner found!");
                return true;
            }

            databaseManager.saveSpawner(new DatabaseManager.StoredSpawner(
                spawner.world, spawner.x, spawner.y, spawner.z,
                spawner.type, spawner.stack, tier, spawner.owner
            ));

            plugin.getHologramManager().updateSpawnerHologram(
                target.getLocation(), spawner.type, spawner.stack, tier
            );

            sender.sendMessage(configManager.getPrefix() + "§aTier set to " + tier);
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError setting tier!");
        }
        return true;
    }

    private boolean handleMenu(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.menu")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        if (!configManager.isUpgradesEnabled()) {
            sender.sendMessage(configManager.getPrefix() + "§cUpgrades are disabled!");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            sender.sendMessage("§cYou must look at a spawner!");
            return true;
        }

        try {
            UpgradeMenu menu = new UpgradeMenu(plugin, spawnerService, configManager, databaseManager);
            menu.open(player, target);
        } catch (Exception e) {
            sender.sendMessage(configManager.getPrefix() + "§cError opening menu!");
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraspawners.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players!");
            return true;
        }

        AdminSpawnerListGUI gui = new AdminSpawnerListGUI(plugin);
        gui.open(player, 0);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8=== §5UltraSpawners §8===");
        sender.sendMessage("§7/ultraspawners reload §8- §7Reload config");
        sender.sendMessage("§7/ultraspawners give <player> <type> [stack] [tier] §8- §7Give spawner");
        sender.sendMessage("§7/ultraspawners inspect §8- §7Inspect spawner");
        sender.sendMessage("§7/ultraspawners settype <type> §8- §7Set mob type");
        sender.sendMessage("§7/ultraspawners setstack <amount> §8- §7Set stack amount");
        sender.sendMessage("§7/ultraspawners settier <tier> §8- §7Set upgrade tier");
        sender.sendMessage("§7/ultraspawners menu §8- §7Open upgrade menu");
        sender.sendMessage("§7/ultraspawners list §8- §7Admin: View all spawners");
    }
}
