package me.koopa.ultraspawners.service;

import me.koopa.ultraspawners.UltraSpawners;
import me.koopa.ultraspawners.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private final UltraSpawners plugin;
    private final ConfigManager configManager;
    private Economy economy;
    private boolean enabled = false;

    public VaultHook(UltraSpawners plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void hook() {
        if (!configManager.isVaultEnabled()) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. Economy features will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
            enabled = true;
            plugin.getLogger().info("Vault hooked successfully. Economy enabled.");
        } else {
            plugin.getLogger().warning("Vault economy provider not found.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canAfford(Player player, double amount) {
        if (!enabled) {
            return false;
        }
        return economy.getBalance(player) >= amount;
    }

    public void withdraw(Player player, double amount) {
        if (enabled) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void deposit(Player player, double amount) {
        if (enabled) {
            economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        if (enabled) {
            return economy.getBalance(player);
        }
        return 0;
    }
}
