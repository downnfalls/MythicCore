package com.dev.mythiccore.reaction.reactions.bloom;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import com.dev.mythiccore.combat.Combat;
import com.dev.mythiccore.enums.MobType;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.utils.StatCalculation;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

public class DendroCore {

    private final Bloom instance;
    private final LivingEntity owner;
    private final StatProvider stat_provider;
    private long life_time;
    private final CMIHologram hologram;
    private final UUID uuid;
    private final EntityDamageEvent.DamageCause damage_cause;
    private MobType mob_type;
    private boolean ignited = false;

    public DendroCore(Bloom instance, UUID uuid, @Nullable LivingEntity owner, StatProvider statProvider, long life_time, CMIHologram hologram, EntityDamageEvent.DamageCause damage_cause, MobType mob_type) {
        this.instance = instance;
        this.owner = owner;
        this.stat_provider = statProvider;
        this.life_time = life_time;
        this.hologram = hologram;
        this.damage_cause = damage_cause;
        this.mob_type = mob_type;
        this.uuid = uuid;
    }

    public void setIgnited(boolean value) { this.ignited = value; }

    public boolean isIgnited() { return ignited; }

    public LivingEntity getOwner() {
        return owner;
    }

    public StatProvider getStatProvider() {
        return stat_provider;
    }

    public long getLifeTime() {
        return life_time;
    }

    public void setLifeTime(long life_time) {
        this.life_time = life_time;
    }

    public CMIHologram getHologram() {
        return hologram;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Bloom getInstance() {
        return instance;
    }

    public void setMobType(MobType mob_type) {
        this.mob_type = mob_type;
    }

    public void ignite() {

        double explode_radius = instance.getConfig().getDouble("dendro-core-explode-radius");

        int attacker_level = 1;
        double elemental_mastery = 0;
        double bloom_bonus = 0;

        if (owner != null) {
            if (owner instanceof Player player) {
                PlayerData playerData = PlayerData.get(player);

                elemental_mastery = stat_provider.getStat("AST_ELEMENTAL_MASTERY");
                bloom_bonus = stat_provider.getStat("AST_BLOOM_BONUS");
                attacker_level = playerData.getLevel();
            } else {
                ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(owner.getUniqueId()).orElse(null);
                attacker_level = (mythicMob != null) ? (int) mythicMob.getLevel() : 1;
            }
        }

        try {

            for (Entity entity : hologram.getWorld().getNearbyEntities(hologram.getLocation(), explode_radius, explode_radius, explode_radius)) {
                boolean mob_type_filter = owner != null && ConfigLoader.aoeDamageFilterEnable() && mob_type != Combat.getMobType(entity);
                if (entity == owner || entity.isInvulnerable() || entity.hasMetadata("NPC") || mob_type_filter || (entity instanceof Player player && (player.getGameMode().equals(GameMode.CREATIVE) || player.getGameMode().equals(GameMode.SPECTATOR))))
                    continue;
                if (entity instanceof LivingEntity livingEntity && !livingEntity.isInvulnerable()) {

                    double resistance_multiplier = StatCalculation.getResistanceMultiplier(livingEntity.getUniqueId(), instance.getConfig().getString("damage-element"));

                    String formula = instance.getConfig().getString("dendro-core-explode-damage");
                    assert formula != null;
                    Expression expression = new ExpressionBuilder(formula)
                            .variables("attacker_level", "elemental_mastery", "resistance_multiplier", "bloom_bonus")
                            .build()
                            .setVariable("attacker_level", attacker_level)
                            .setVariable("elemental_mastery", elemental_mastery)
                            .setVariable("resistance_multiplier", resistance_multiplier)
                            .setVariable("bloom_bonus", bloom_bonus);

                    double final_damage = expression.evaluate();

                    instance.damage(final_damage, owner, livingEntity, instance.getConfig().getString("damage-element"), false, false, true, false, damage_cause);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // visual
        try {
            for (String s : instance.getConfig().getStringList("dendro-core.explode-sound")) {
                String[] raw_sound = s.split(":");
                String sound = raw_sound[0];
                int volume = Integer.parseInt(raw_sound[1]);
                int pitch = Integer.parseInt(raw_sound[2]);

                hologram.getWorld().playSound(hologram.getLocation(), Sound.valueOf(sound), volume, pitch);
            }

            for (String p : instance.getConfig().getStringList("dendro-core.explode-particle")) {
                String[] raw_particle = p.split(":");
                String particle = raw_particle[0];
                int speed = Integer.parseInt(raw_particle[1]);
                int count = Integer.parseInt(raw_particle[2]);

                hologram.getWorld().spawnParticle(Particle.valueOf(particle), hologram.getLocation(), count, 0, 0, 0, speed);
            }

        } catch (NumberFormatException ignored) {}

        this.remove();
    }

    public void remove() {

        this.hologram.remove();
        DendroCoreManager.dendroCoreIds.remove(this.uuid);

        try {
            for (Chunk chunk : DendroCoreManager.dendroCoreInChunk.keySet()) {
                List<DendroCore> dendroCores = DendroCoreManager.dendroCoreInChunk.get(chunk);
                for (DendroCore dendroCore : dendroCores) {
                    if (dendroCore.getUUID().equals(this.uuid)) {
                        dendroCores.remove(dendroCore);
                        if (dendroCores.isEmpty()) {
                            DendroCoreManager.dendroCoreInChunk.remove(chunk);
                        }
                    }
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }

}
