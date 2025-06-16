package com.dev.mythiccore.reaction.reactions.bloom.sub_reaction;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.combat.Combat;
import com.dev.mythiccore.reaction.reactions.bloom.DendroCore;
import com.dev.mythiccore.reaction.reactions.bloom.DendroCoreReaction;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.utils.StatCalculation;
import com.dev.mythiccore.utils.Utils;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class HyperBloom extends DendroCoreReaction {
    public HyperBloom(String id, ConfigurationSection config, String display, String trigger) {
        super(id, config, display, trigger);
    }

    @Override
    public void trigger(DendroCore dendro_core, LivingEntity entity, @Nullable Entity damager, StatProvider stats, EntityDamageEvent.DamageCause damage_cause) {

        int check_radius = getConfig().getInt("check-radius");
        Location dendroCoreLocation = dendro_core.getHologram().getLocation();

        LivingEntity target_entity = dendro_core.getHologram().getWorld().getNearbyEntities(dendroCoreLocation, check_radius, check_radius, check_radius).stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> !e.equals(damager))
                .filter(e -> !(ConfigLoader.aoeDamageFilterEnable() && damager != null && !Combat.getLastMobType(damager).equals(Combat.getMobType(e))))
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(dendroCoreLocation)))
                .orElse(null);

        if (target_entity == null) return;

        long life_time = dendro_core.getInstance().getConfig().getLong("sub-reaction.HYPERBLOOM.life-time-after-trigger");
        if (life_time >= 0) dendro_core.setLifeTime(life_time);
        long start_time = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskTimer(MythicCore.getInstance(), (task)->{

            Location startLocation = dendro_core.getHologram().getLocation();
            Location targetLocation = target_entity.getLocation();

            if (!target_entity.isValid()) {
                task.cancel();
                dendro_core.remove();
                return;
            }

            if (target_entity.isDead()) {
                task.cancel();
                dendro_core.remove();
                return;
            }

            if (!dendro_core.getHologram().isEnabled()) {
                task.cancel();
                dendro_core.remove();
                return;
            }

            Vector direction = targetLocation.clone().subtract(startLocation).toVector();
            if (startLocation.distance(targetLocation) <= 1 || direction.isZero()) {
                task.cancel();

                int attacker_level = 1;
                double elemental_mastery = 0;
                double hyperbloom_bonus = 0;

                if (damager != null) {
                    if (damager instanceof Player player) {
                        PlayerData playerData = PlayerData.get(player);

                        elemental_mastery = stats.getStat("AST_ELEMENTAL_MASTERY");
                        hyperbloom_bonus = stats.getStat("AST_HYPERBLOOM_BONUS");
                        attacker_level = playerData.getLevel();
                    } else {
                        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(damager.getUniqueId()).orElse(null);
                        attacker_level = (mythicMob != null) ? (int) mythicMob.getLevel() : 1;
                    }
                }

                double resistance_multiplier = StatCalculation.getResistanceMultiplier(target_entity.getUniqueId(), getConfig().getString("damage-element"));

                String formula = getConfig().getString("damage-formula");
                assert formula != null;
                Expression expression = new ExpressionBuilder(formula)
                        .variables("attacker_level", "elemental_mastery", "resistance_multiplier", "level_multiplier", "hyperbloom_bonus")
                        .build()
                        .setVariable("attacker_level", attacker_level)
                        .setVariable("elemental_mastery", elemental_mastery)
                        .setVariable("resistance_multiplier", resistance_multiplier)
                        .setVariable("hyperbloom_bonus", hyperbloom_bonus);

                double final_damage = expression.evaluate();

                dendro_core.getInstance().damage(final_damage, damager, target_entity, getConfig().getString("damage-element"), false, false, true, false, damage_cause);

                // visual
                try {
                    for (String s : getConfig().getStringList("explode-sound")) {
                        String[] raw_sound = s.split(":");
                        String sound = raw_sound[0];
                        float volume = Float.parseFloat(raw_sound[1]);
                        float pitch = Float.parseFloat(raw_sound[2]);

                        target_entity.getWorld().playSound(target_entity.getLocation(), Sound.valueOf(sound), volume, pitch);
                    }

                    for (String p : getConfig().getStringList("explode-particle")) {
                        String[] raw_particle = p.split(":");
                        String particle = raw_particle[0];
                        double speed = Double.parseDouble(raw_particle[1]);
                        int count = Integer.parseInt(raw_particle[2]);

                        target_entity.getWorld().spawnParticle(Particle.valueOf(particle), target_entity.getLocation(), count, 0, 0, 0, speed);
                    }

                } catch (NumberFormatException ignored) {}

                finally {
                    dendro_core.remove();
                }

            } else {

                double force = dendro_core.getInstance().getConfig().getDouble("sub-reaction.HYPERBLOOM.pulling-force");
                double thetaX = Math.acos(direction.getX() / direction.length());
                double thetaY = Math.acos(direction.getY() / direction.length());
                double thetaZ = Math.acos(direction.getZ() / direction.length());
                Vector force_axis = new Vector(force * Math.cos(thetaX), force * Math.cos(thetaY), force * Math.cos(thetaZ));

                double time = (System.currentTimeMillis() - start_time) / 1000.0;
                double xOffset = (0 * time + 0.5 * force_axis.getX() * Math.pow(time, 2)) * 0.1;
                double yOffset = (dendro_core.getInstance().getConfig().getDouble("sub-reaction.HYPERBLOOM.take-off-velocity") * time + 0.5 * (force_axis.getY() - (-10)) * Math.pow(time, 2)) * 0.1;
                double zOffset = (0 * time + 0.5 * force_axis.getZ() * Math.pow(time, 2)) * 0.1;

                Location newLocation = startLocation.clone().add(xOffset, yOffset, zOffset);
                Location d = new Location(startLocation.getWorld(), 0, 0, 0);
                d.setDirection(direction);
                Utils.generateParticles(Particle.valueOf(getConfig().getString("launch-particle.particle")), getConfig().getDouble("launch-particle.radius"), getConfig().getInt("launch-particle.points"), getConfig().getDouble("launch-particle.speed"), startLocation, d.getPitch(), -d.getYaw());
                dendro_core.getHologram().setLoc(newLocation);
                dendro_core.getHologram().update();
            }


        },0,1);
    }
}
