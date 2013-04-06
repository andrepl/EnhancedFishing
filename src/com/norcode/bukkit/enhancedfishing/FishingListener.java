package com.norcode.bukkit.enhancedfishing;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

public class FishingListener implements Listener {
    EnhancedFishing plugin;
    private Random random = new Random();
    private HashMap<String, Fish> playerHooks = new HashMap<String, Fish>();
    public FishingListener(EnhancedFishing plugin) {
        this.plugin = plugin;
        
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onLightningString(LightningStrikeEvent event) {
        if (plugin.isLightningRaisesChance()) {
            for (Entity e: event.getLightning().getNearbyEntities(6, 6, 6)) {
                double chance;
                if (e instanceof Fish) {
                    chance = ((Fish) e).getBiteChance()*2.0;
                    if (chance > 1.0) chance = 1.0;
                    ((Fish) e).setBiteChance(chance);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fish) {
            Player player = (Player) ((Fish) event.getDamager()).getShooter(); 
            if (player.getItemInHand() != null && player.getItemInHand().getType().equals(Material.FISHING_ROD)) {
                int thorns = player.getItemInHand().getEnchantmentLevel(Enchantment.THORNS);
                event.setDamage(thorns+2);
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        final Player player = event.getPlayer();
        ItemStack rod = player.getItemInHand();
        if (event.getState().equals(PlayerFishEvent.State.FISHING)) {
            playerHooks.put(player.getName(), event.getHook());
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Fish hook = playerHooks.get(player.getName());
                    if (!hook.isDead() && hook.isValid()) {
                        hook.setBiteChance(calculateBiteChance(hook));
                    }
                }
            }, 40);
        } else if (event.getState().equals(PlayerFishEvent.State.CAUGHT_FISH)) {
             int looting = rod.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
             int fortune = rod.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
             int fireaspect = rod.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
             Item item = (Item) event.getCaught();
             if (item.getItemStack() != null) {
                 if (looting > 0 && plugin.isLootingEnabled() && random.nextDouble() < looting * 0.2
                         && player.hasPermission("enhancedfishing.enchantment.looting")) {
                     item.setItemStack(plugin.getLootTable().get(looting).getStack().clone());
                 }
    
                 if (fortune > 0 && plugin.isFortuneEnabled() && random.nextDouble() < fortune*0.33
                         && player.hasPermission("enhancedfishing.enchantment.fortune")) {
                     ItemStack stack = item.getItemStack().clone();
                     stack.setAmount(random.nextInt(fortune)+1);
                     item.setItemStack(stack);
                 }
    
                 if (fireaspect > 0 && item.getItemStack().getType().equals(Material.RAW_FISH) &&
                         plugin.isFireAspectEnabled() && player.hasPermission("enhancedfishing.enchantment.fireaspect")) {
                     item.setItemStack(new ItemStack(Material.COOKED_FISH, item.getItemStack().getAmount()));
                 }
             }
        } 
    }

    protected double calculateBiteChance(Fish hook) {
        Player p = (Player) hook.getShooter();
        ItemStack rod = p.getItemInHand();
        double chance = plugin.getConfig().getDouble("bite-chance.default", 0.002);
        for (Permission perm: plugin.getLoadedPermissions()) {
            if (p.hasPermission(perm)) {
                chance = plugin.getConfig().getDouble("bite-chance." + perm.getName().substring(28));
            }
        }
        
        if (hook.getBiteChance() == 1/300.0 && plugin.isRainRaisesChance()) {
            chance *= 1.666666666666666;
        } else if (plugin.isSunriseRaisesChance() && hook.getWorld().getTime() > 22300 && hook.getWorld().getTime() < 23400) {
            chance *= 2.5;
        }

        if (plugin.isBoatRaisesChance() && p.getVehicle() != null && p.getVehicle() instanceof Boat) {
            chance *= 1.333;
        }

        if (rod != null && rod.getType().equals(Material.FISHING_ROD) && p.hasPermission("enhancedfishing.enchantment.efficiency")) {
            Map<Enchantment, Integer> enchantments = rod.getEnchantments();
            if (enchantments.containsKey(Enchantment.DIG_SPEED) && plugin.isEfficiencyEnabled()) {
                chance += plugin.getChancePerEfficiencyLevel() * enchantments.get(Enchantment.DIG_SPEED);
            }
        }

        for (Entity e: hook.getNearbyEntities(6, 4, 6)) {
            if (plugin.isCrowdingLowersChance() && e instanceof Fish && e.getLocation().distance(hook.getLocation()) <= 3) {
                chance -= 0.0005;
            } else if (plugin.isMobsLowerChance() && e instanceof LivingEntity && !e.equals(hook.getShooter())) {
                chance -= 0.0005;
            }
        }
        return chance;
    }
}
