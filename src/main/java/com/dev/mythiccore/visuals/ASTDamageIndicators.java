package com.dev.mythiccore.visuals;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.event.IndicatorDisplayEvent;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamagePacket;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.element.Element;
import io.lumine.mythic.lib.hologram.Hologram;
import io.lumine.mythic.lib.listener.option.GameIndicators;
import io.lumine.mythic.lib.util.CustomFont;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class just use to override MythicLib idiot damage indicator system,
 * so I modified some code
 */
public class ASTDamageIndicators extends GameIndicators {
    public final boolean splitHolograms;
    public final String format;
    public final String crit_format;
    public final boolean enable;
    public final @Nullable CustomFont font;
    public final @Nullable CustomFont fontCrit;
    private final double radialVelocity;
    private final double gravity;
    private final double initialUpwardVelocity;
    private final double entityHeightPercentage;
    private final double yOffset;

    public ASTDamageIndicators(ConfigurationSection config) {
        super(config);
        this.enable = config.getBoolean("enable");
        this.splitHolograms = config.getBoolean("split-holograms");
        this.format = config.getString("format");
        this.crit_format = config.getString("crit-format");
        this.radialVelocity = config.getDouble("radial-velocity", (double)1.0F);
        this.gravity = config.getDouble("gravity", (double)1.0F);
        this.initialUpwardVelocity = config.getDouble("initial-upward-velocity", (double)1.0F);
        this.entityHeightPercentage = config.getDouble("entity-height-percent", (double)0.75F);
        this.yOffset = config.getDouble("y-offset", 0.1);
        if (config.getBoolean("custom-font.enabled")) {
            this.font = new CustomFont(Objects.requireNonNull(config.getConfigurationSection("custom-font.normal")));
            this.fontCrit = new CustomFont(Objects.requireNonNull(config.getConfigurationSection("custom-font.crit")));
        } else {
            this.font = null;
            this.fontCrit = null;
        }
    }

    public @NotNull Vector getDirection(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            Vector dir = event.getEntity().getLocation().toVector().subtract(((EntityDamageByEntityEvent)event).getDamager().getLocation().toVector()).setY(0);
            if (dir.lengthSquared() > 0.0) {
                double a = Math.atan2(dir.getZ(), dir.getX());
                a += 1.5707963267948966 * (random.nextDouble() - 0.5);
                return new Vector(Math.cos(a), 0.0, Math.sin(a));
            }
        }

        double a = random.nextDouble() * Math.PI * 2.0;
        return new Vector(Math.cos(a), 0.0, Math.sin(a));
    }

    public @NotNull Map<IndicatorType, Double> mapDamage(DamageMetadata damageMetadata) {
        Map<IndicatorType, Double> mapped = new HashMap<>();

        for (DamagePacket packet : damageMetadata.getPackets()) {
            IndicatorType type = new IndicatorType(damageMetadata, packet);
            mapped.put(type, mapped.getOrDefault(type, 0.0) + packet.getFinalValue());
        }

        return mapped;
    }

    public static class IndicatorType {
        final boolean physical;
        final @Nullable Element element;
        final boolean crit;

        IndicatorType(DamageMetadata damageMetadata, DamagePacket packet) {
            boolean var10001;
            label22: {
                label24: {
                    this.physical = packet.hasType(DamageType.PHYSICAL);
                    this.element = packet.getElement();
                    if (this.physical) {
                        if (damageMetadata.isWeaponCriticalStrike()) {
                            break label24;
                        }
                    } else if (damageMetadata.isSkillCriticalStrike()) {
                        break label24;
                    }

                    if (this.element == null || !damageMetadata.isElementalCriticalStrike(this.element)) {
                        var10001 = false;
                        break label22;
                    }
                }

                var10001 = true;
            }

            this.crit = var10001;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                IndicatorType that = (IndicatorType)o;
                return this.physical == that.physical && Objects.equals(this.element, that.element);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(this.physical, this.element);
        }
    }

    @NotNull
    public String computeFormat(double damage, boolean crit, String format, Element element) {
        CustomFont indicatorFont = (crit && ASTDamageIndicators.this.fontCrit != null) ? ASTDamageIndicators.this.fontCrit : ASTDamageIndicators.this.font;
        String formattedDamage = indicatorFont == null ? ASTDamageIndicators.this.formatNumber(damage) : indicatorFont.format(ASTDamageIndicators.this.formatNumber(damage));
        return MythicLib.plugin.getPlaceholderParser().parse(null, format.replace("{color}", (element != null) ? element.getColor() : "").replace("{icon}", (element != null) ? element.getLoreIcon() : "").replace("{value}", formattedDamage));
    }

    @Override
    public void displayIndicator(Entity entity, String message, @NotNull Vector dir, IndicatorDisplayEvent.IndicatorType type) {
        IndicatorDisplayEvent called = new IndicatorDisplayEvent(entity, message, type);
        Bukkit.getPluginManager().callEvent(called);
        if (!called.isCancelled()) {
            Location loc = entity.getLocation().add((random.nextDouble() - (double)0.5F) * 1.2, this.yOffset + entity.getHeight() * this.entityHeightPercentage, (random.nextDouble() - (double)0.5F) * 1.2);
            this.displayIndicator(loc, called.getMessage(), dir);
        }
    }

    public void displayIndicator(final Location loc, String message, final @NotNull Vector dir) {
        final Hologram holo = Hologram.create(loc, MythicLib.plugin.parseColors(Collections.singletonList(message)));
        (new BukkitRunnable() {
            double v;
            int i;
            private final double acc;
            private final double dt;

            {
                this.v = (double)6.0F * ASTDamageIndicators.this.initialUpwardVelocity;
                this.i = 0;
                this.acc = (double)-10.0F * ASTDamageIndicators.this.gravity;
                this.dt = 0.15;
            }

            public void run() {
                if (this.i == 0) {
                    dir.multiply((double)2.0F * ASTDamageIndicators.this.radialVelocity);
                }

                if (this.i++ >= 7) {
                    holo.despawn();
                    this.cancel();
                } else {
                    this.v += this.acc * 0.15;
                    loc.add(dir.getX() * 0.15, this.v * 0.15, dir.getZ() * 0.15);
                    holo.updateLocation(loc);
                }
            }
        }).runTaskTimer(MythicLib.plugin, 0L, 3L);
    }
}
