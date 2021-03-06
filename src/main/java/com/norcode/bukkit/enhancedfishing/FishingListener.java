package com.norcode.bukkit.enhancedfishing;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.Biome;
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

public class FishingListener implements Listener {
    EnhancedFishing plugin;
    private Random random = new Random();
    private HashMap<String, Fish> playerHooks = new HashMap<String, Fish>();
    public FishingListener(EnhancedFishing plugin) {
        this.plugin = plugin;
        
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onLightningStrike(LightningStrikeEvent event) {
        WorldConfiguration cfg = plugin.getWorldConfiguration(event.getWorld());
        if (!cfg.isEnabled()) return;
        for (Entity e: event.getLightning().getNearbyEntities(cfg.getLightningRadius(), 4, cfg.getLightningRadius())) {
            double chance;
            if (e instanceof Fish) {
                chance = cfg.getLightningModifier().apply(((Fish) e).getBiteChance());
                if (chance > 1.0) chance = 1.0;
                ((Fish) e).setBiteChance(chance);
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fish) {
            Player player = (Player) ((Fish) event.getDamager()).getShooter();
            WorldConfiguration cfg = plugin.getWorldConfiguration(player.getWorld());
            if (!cfg.isEnabled()) return;
            if (!cfg.isThornsEnabled()) return;
            if (player.getItemInHand() != null && player.getItemInHand().getType().equals(Material.FISHING_ROD)) {
                int thorns = player.getItemInHand().getEnchantmentLevel(Enchantment.THORNS);
                if (thorns > 0) {
                    event.setDamage(thorns+2);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        final Player player = event.getPlayer();
        if (!plugin.getWorldConfiguration(player.getWorld()).isEnabled()) {
            return;
        }
        ItemStack rod = player.getItemInHand();
        if (event.getState().equals(PlayerFishEvent.State.FISHING)) {
            WorldConfiguration cfg = plugin.getWorldConfiguration(event.getHook().getWorld());
            if (cfg.isPowerEnabled() && player.hasPermission("enhancedfishing.enchantment.power")) {
                int power = rod.getEnchantmentLevel(Enchantment.ARROW_DAMAGE); 
                if (power > 0) {
                    event.getHook().setVelocity(event.getHook().getVelocity().multiply(1.0 + Math.max(0.99, (0.25*power))));
                }
            }
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
                 WorldConfiguration cfg = plugin.getWorldConfiguration(event.getHook().getWorld());
                 if (looting > 0) {
                     if (cfg.isLootingEnabled() && player.hasPermission("enhancedfishing.enchantment.looting")) {
                         if (random.nextDouble() < Math.min(looting * cfg.getLootingLevelChance(), 1.0)) {
                            item.setItemStack(cfg.getLootTable().get(looting).getStack().clone());
                         }
                     }
                 } else if (random.nextDouble() < cfg.getUnenchantedLootingChance()) {
                     item.setItemStack(cfg.getLootTable().get(1).getStack().clone());
                 }
                 if (fortune > 0 && cfg.isFortuneEnabled() && random.nextDouble() < Math.min(fortune * cfg.getFortuneLevelChance(), 1.0)
                         && player.hasPermission("enhancedfishing.enchantment.fortune")) {
                     ItemStack stack = item.getItemStack().clone();
                     stack.setAmount(random.nextInt(Math.max(1, fortune-1))+2);
                     item.setItemStack(stack);
                 }
                 if (fireaspect > 0 && item.getItemStack().getType().equals(Material.RAW_FISH) &&
                         cfg.isFireAspectEnabled() && player.hasPermission("enhancedfishing.enchantment.fireaspect")) {
                     item.setItemStack(new ItemStack(Material.COOKED_FISH, item.getItemStack().getAmount()));
                 }
             }
        } 
    }

    protected double calculateBiteChance(Fish hook) {
        Player p = (Player) hook.getShooter();
        ItemStack rod = p.getItemInHand();
        WorldConfiguration cfg = plugin.getWorldConfiguration(p.getWorld());
        double chance = cfg.getBaseCatchChance(p);

        if (hook.getBiteChance() == 1/300.0) {
            chance = cfg.getRainModifier().apply(chance);
        } else if (hook.getWorld().getTime() > cfg.getSunriseStart() && hook.getWorld().getTime() < cfg.getSunriseEnd()) {
            chance = cfg.getSunriseModifier().apply(chance);
        }

        if (p.getVehicle() != null && p.getVehicle() instanceof Boat) {
            chance = cfg.getBoatModifier().apply(chance);
        }

        if (rod != null && rod.getType().equals(Material.FISHING_ROD) && p.hasPermission("enhancedfishing.enchantment.efficiency")) {
            Map<Enchantment, Integer> enchantments = rod.getEnchantments();
            if (enchantments.containsKey(Enchantment.DIG_SPEED) && cfg.isEfficiencyEnabled()) {
                for (int i=0;i<enchantments.get(Enchantment.DIG_SPEED);i++) {
                    chance = cfg.getEfficiencyLevelModifier().apply(chance);
                }
            }
        }

        double r = Math.max(cfg.getCrowdingRadius(), cfg.getMobRadius());
        for (Entity e: hook.getNearbyEntities(r, 4, r)) {
            if (e instanceof Fish && e.getLocation().distance(hook.getLocation()) <= cfg.getCrowdingRadius()) {
                chance = cfg.getCrowdingModifier().apply(chance);
            } else if (e instanceof LivingEntity && !e.equals(hook.getShooter()) && e.getLocation().distance(hook.getLocation()) <= cfg.getMobRadius()) {
                chance = cfg.getMobsModifier().apply(chance);
            }
        }
        Biome biome = hook.getWorld().getBiome(hook.getLocation().getBlockX(), hook.getLocation().getBlockY());
        chance = cfg.getBiomeModifier(biome).apply(chance);
        return chance;
    }
}
