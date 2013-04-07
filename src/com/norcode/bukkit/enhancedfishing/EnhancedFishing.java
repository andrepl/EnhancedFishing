package com.norcode.bukkit.enhancedfishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EnhancedFishing extends JavaPlugin {

    private List<Permission> loadedPermissions = null;
    private double baseCatchChance = 1/500.0;
    private DoubleModifier sunriseModifier = new DoubleModifier();
    private double sunriseStart = 22400;
    private double sunriseEnd = 23400;
    private DoubleModifier rainModifier = new DoubleModifier();
    private DoubleModifier crowdingModifier = new DoubleModifier();
    private double crowdingRadius = 6;

    private DoubleModifier mobsModifier = new DoubleModifier();
    private double mobsRadius = 8;

    private DoubleModifier lightningModifier = new DoubleModifier();
    private double lightningRadius = 8;

    private DoubleModifier boatModifier = new DoubleModifier();

    private DoubleModifier biomeOceanModifier = new DoubleModifier();
    private DoubleModifier biomeRiverModifier = new DoubleModifier();
    private boolean efficiencyEnabled = true; // Better Odds
    private DoubleModifier efficiencyLevelModifier = new DoubleModifier("+0.0015");
    private boolean lootingEnabled = true;    // Catch treasure instead of fish
    private double lootingLevelChance = 0.15;
    private boolean fortuneEnabled = true;    // catch multiple fish at once
    private double fortuneLevelChance = 0.15;
    private boolean fireAspectEnabled = true; // catch cooked fish
    private boolean thornsEnabled = true;      // deal damage when hook hits mobs.

    private LootTable lootTable;

    public double getBaseCatchChance() {
        return baseCatchChance;
    }

    public DoubleModifier getSunriseModifier() {
        return sunriseModifier;
    }

    public double getSunriseStart() {
        return sunriseStart;
    }

    public double getSunriseEnd() {
        return sunriseEnd;
    }

    public DoubleModifier getRainModifier() {
        return rainModifier;
    }

    public DoubleModifier getCrowdingModifier() {
        return crowdingModifier;
    }

    public double getCrowdingRadius() {
        return crowdingRadius;
    }

    public DoubleModifier getMobsModifier() {
        return mobsModifier;
    }

    public double getMobRadius() {
        return mobsRadius;
    }

    public DoubleModifier getLightningModifier() {
        return lightningModifier;
    }


    public double getLightningRadius() {
        return lightningRadius;
   }

    public DoubleModifier getBoatModifier() {
        return boatModifier;
    }

    public DoubleModifier getBiomeOceanModifier() {
        return biomeOceanModifier;
    }

    public DoubleModifier getBiomeRiverModifier() {
        return biomeRiverModifier;
    }

    public DoubleModifier getEfficiencyLevelModifier() {
        return efficiencyLevelModifier;
    }

    public boolean isEfficiencyEnabled() {
        return efficiencyEnabled;
    }

    public double getLootingLevelChance() {
        return lootingLevelChance;
    }

    public double getFortuneLevelChance() {
        return fortuneLevelChance;
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
        lootTable.reload();
        baseCatchChance = getConfig().getDouble("bite-chance.default", 0.002);
        for (String nodename: getConfig().getConfigurationSection("bite-chance").getKeys(false)) {
            if (nodename != "default") {
                Permission node = new Permission("enhancedfishing.bite-chance." + nodename.toLowerCase(),PermissionDefault.FALSE);
                pm.addPermission(node);
                loadedPermissions.add(node);
                node.recalculatePermissibles();
            }
        }
        boatModifier = new DoubleModifier(getConfig().getString("environmental.boat-modifier"));
        crowdingModifier = new DoubleModifier(getConfig().getString("environmental.crowding-modifier"));
        crowdingRadius = getConfig().getDouble("environmental.crowding-radius");
        mobsModifier = new DoubleModifier(getConfig().getString("environmental.mobs-modifier"));
        mobsRadius = getConfig().getDouble("environmental.mobs-radius");
        rainModifier = new DoubleModifier(getConfig().getString("environmental.rain-modifier"));
        lightningModifier = new DoubleModifier(getConfig().getString("environmental.lightning-modifier"));
        lightningRadius = getConfig().getDouble("environmental.lightning-radius");
        sunriseModifier = new DoubleModifier(getConfig().getString("environmental.sunrise-modifier"));
        sunriseStart = getConfig().getDouble("environmental.sunrise-start");
        sunriseEnd = getConfig().getDouble("environmental.sunrise-end");
        biomeOceanModifier = new DoubleModifier(getConfig().getString("environmental.biome-ocean-modifier"));
        biomeRiverModifier = new DoubleModifier(getConfig().getString("environmental.biome-river-modifier"));
        efficiencyLevelModifier = new DoubleModifier(getConfig().getString("efficiency-level-modifier"));
        efficiencyEnabled = getConfig().getBoolean("enchantments.efficiency", true);
        lootingEnabled = getConfig().getBoolean("enchantments.looting", true);
        lootingLevelChance = getConfig().getDouble("enchantments.looting-level-chance", 0.15);
        fortuneEnabled = getConfig().getBoolean("enchantments.fortune", true);
        fortuneLevelChance = getConfig().getDouble("enchantments.fortune-level-chance", 0.15);
        fireAspectEnabled = getConfig().getBoolean("enchantments.fire-aspect", true);
        thornsEnabled = getConfig().getBoolean("enchantments.thorns", true);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args[0].toLowerCase().equals("reload")) {
            if (sender.hasPermission("enhancedfishing.admin")) {
                this.loadConfig();
                sender.sendMessage("EnhancedFishing config has been reloaded from disk.");
                return true;
            }
        }
        return false;
    }
}
