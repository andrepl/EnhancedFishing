package com.norcode.bukkit.enhancedfishing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class LootTable {
    
    public static class Loot {
        String name;
        ItemStack stack;
        double weight;
        public Loot(String name, ItemStack stack, double weight) {
            this.name = name;
            this.stack = stack;
            this.weight = weight;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public ItemStack getStack() {
            return stack;
        }
        public void setStack(ItemStack stack) {
            this.stack = stack;
        }
        public double getWeight() {
            return weight;
        }
        public void setWeight(double weight) {
            this.weight = weight;
        }
        
    }

    private ConfigAccessor configAccessor;
    private final NavigableMap<Double, Loot> map = new TreeMap<Double, Loot>();
    private final Random random;
    private double total = 0;
    private EnhancedFishing plugin;
    
    public LootTable(EnhancedFishing plugin) {
        this.random = new Random();
        this.plugin = plugin;
        this.configAccessor = new ConfigAccessor(plugin, "treasure.yml");
        this.configAccessor.getConfig();
        this.configAccessor.saveDefaultConfig();
    }

    public void clear() {
        this.map.clear();
        this.total = 0;
    }

    public void add(String name, ItemStack stack, double weight) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, new Loot(name, stack, weight));
        plugin.getLogger().info("Added " + stack + " w/ key + " + total);
    }

    public Loot get(int lootingLevel) {
        double max = 0;
        double score = 0;
        for (int i=0;i<random.nextInt(lootingLevel/2)+1;i++) {
            score = random.nextDouble() * total;
            if (score > max) max = score;
        }
        if (max >= total) { 
            max = total-1;
        }
        return map.ceilingEntry(max).getValue();
    }

    public void reload() {
        configAccessor.reloadConfig();
        this.clear();
        ConfigurationSection cfg = null;
        for (String key: getLootConfig().getKeys(false)) {
            cfg = getLootConfig().getConfigurationSection(key);
            add(key, cfg.getItemStack("item"), cfg.getDouble("weight"));
        }
    }

    private ConfigurationSection getLootConfig() {
        return configAccessor.getConfig();
    }

    public void test() {
        for (int l=1;l<=5;l++) {
            HashMap<String, Integer> results = new HashMap<String, Integer>();
            Loot prize = null;
            for (int i=0;i<1000;i++) {
                prize = get(l);
                if (!results.containsKey(prize.getName())) {
                    results.put(prize.getName(), 0);
                }
                results.put(prize.getName(), results.get(prize.getName())+1);
            }
            plugin.getLogger().info("LOOTING " + l);
            for (Entry<String, Integer> e: results.entrySet()) {
                plugin.getLogger().info(e.getKey() + "\t\t==> " + e.getValue());
            }
        }
    }
}
