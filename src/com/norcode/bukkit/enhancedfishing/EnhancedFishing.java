package com.norcode.bukkit.enhancedfishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EnhancedFishing extends JavaPlugin {

    private List<Permission> loadedPermissions = null;
    private double baseCatchChance = 1/500.0;
    private boolean crowdingLowersChance = true;
    private boolean mobsLowerChance = true;
    private boolean rainRaisesChance = true;
    private boolean efficiencyEnabled = true; // Better Odds
    private boolean lootingEnabled = true;    // Catch treasure instead of fish
    private boolean fortuneEnabled = true;    // catch multiple fish at once
    private boolean fireAspectEnabled = true; // catch cooked fish
    private boolean thornsEnabled = true;      // deal damage when hook hits mobs.
    private double chancePerEfficiencyLevel = 0.0015;
    
    private LootTable lootTable;
    
    public double getBaseCatchChance() {
        return baseCatchChance;
    }

    public boolean isCrowdingLowersChance() {
        return crowdingLowersChance;
    }

    public boolean isMobsLowerChance() {
        return mobsLowerChance;
    }

    public boolean isRainRaisesChance() {
        return rainRaisesChance;
    }

    public boolean isEfficiencyEnabled() {
        return efficiencyEnabled;
    }

    public boolean isLootingEnabled() {
        return lootingEnabled;
    }

    public boolean isFortuneEnabled() {
        return fortuneEnabled;
    }

    public boolean isFireAspectEnabled() {
        return fireAspectEnabled;
    }

    public boolean isThornsEnabled() {
        return thornsEnabled;
    }

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        lootTable = new LootTable(this);
        loadedPermissions = new ArrayList<Permission>();
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        loadConfig();
    }

    private void loadConfig() {
        PluginManager pm = getServer().getPluginManager();
        for (Permission p: loadedPermissions) {
            pm.removePermission(p);
        }
        loadedPermissions.clear();
        baseCatchChance = getConfig().getDouble("bite-chance.default", 0.002);
        for (String nodename: getConfig().getConfigurationSection("bite-chance").getKeys(false)) {
            if (nodename != "default") {
                Permission node = new Permission("enhancedfishing.bite-chance." + nodename.toLowerCase(),PermissionDefault.FALSE);
                pm.addPermission(node);
                loadedPermissions.add(node);
                node.recalculatePermissibles();
            }
        }
        crowdingLowersChance = getConfig().getBoolean("environmental.crowding", true);
        mobsLowerChance = getConfig().getBoolean("environmental.mobs", true);
        rainRaisesChance = getConfig().getBoolean("environmental.rain", true);
        efficiencyEnabled = getConfig().getBoolean("enchantments.efficiency", true);
        lootingEnabled = getConfig().getBoolean("enchantments.looting", true);
        fortuneEnabled = getConfig().getBoolean("enchantments.fortune", true);
        fireAspectEnabled = getConfig().getBoolean("enchantments.fire-aspect", true);
        thornsEnabled = getConfig().getBoolean("enchantments.thorns", true);
        chancePerEfficiencyLevel = getConfig().getDouble("chance-per-efficiency-level", 0.0015);
    }

    public List<Permission> getLoadedPermissions() {
        return loadedPermissions;
    }

    public void setLoadedPermissions(List<Permission> loadedPermissions) {
        this.loadedPermissions = loadedPermissions;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public double getChancePerEfficiencyLevel() {
        return chancePerEfficiencyLevel;
    }
}
