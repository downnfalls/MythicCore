package com.dev.mythiccore.visuals;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.events.dps_check.DPSCheck;
import com.dev.mythiccore.utils.ConfigLoader;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.dev.mythiccore.visuals.HealthBar.*;

public class HealthBarData {
    public final Entity entity;
    public final String entityType;
    public Hologram healthHolo;
    public String name = "";
    public String name_uncolored = "";
    public String health = "";
    public String max_health = "";
    public String health_bar = "";
    public String aura = "";
    public String buff = "";

    private BukkitRunnable updateTask = null;

    public HealthBarData(Entity entity) {
        this.entity = entity;

        healthHolo = DHAPI.getHologram("DamageIndicator_Health_" + entity.getUniqueId());

        ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
        if (mob != null) {
            entityType = mob.getMobType();
        } else {
            entityType = entity.getType().toString();
        }
    }

    public void reload() {

        if (healthHolo == null || updateTask == null) {
            if (healthHolo != null) healthHolo.delete();
            updateTask = null;

            healthHolo = DHAPI.createHologram("DamageIndicator_Health_" + entity.getUniqueId(), entity.getLocation().clone().add(0, entity.getHeight() + customHealthBars.getOrDefault(entityType, defaultHealthBar).heightOffset(), 0));
            healthHolo.showAll();

            if (!customHealthBars.getOrDefault(entityType, defaultHealthBar).display()) healthHolo.setEnabled(false);

            initializeHealthHologram(healthHolo, entityType);
            updateHealthHologram(healthHolo);

            // Assign entity to the DamageIndicatorBars team to hide name tag
            damageIndicatorTeam.addEntry(entity.getUniqueId().toString());

            Hologram finalHealthHolo = healthHolo;
            String finalMythicMobsType = entityType;
            updateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid() && !entity.isDead() && ((!Objects.equals(name, "") && !Objects.equals(name_uncolored, "") && !Objects.equals(health, "") && !Objects.equals(max_health, "") && !Objects.equals(health_bar, "")) || !Objects.equals(aura, "")) || !Objects.equals(buff, "")) {
                        aura = MythicCore.getAuraManager().getAura(entity.getUniqueId()).getAuraIcon();
                        buff = MythicCore.getBuffManager().getBuff(entity.getUniqueId()).getBuffIcon();
                        updateHealthHologram(healthHolo);
                        finalHealthHolo.setLocation(entity.getLocation().clone().add(0, entity.getHeight() + customHealthBars.getOrDefault(finalMythicMobsType, defaultHealthBar).heightOffset(), 0));
                        finalHealthHolo.realignLines();
                    } else {
                        finalHealthHolo.delete();
                        damageIndicatorTeam.removeEntry(entity.getUniqueId().toString());
                        healthBarMap.remove(entity);
                        this.cancel();
                    }
                }
            };

            updateTask.runTaskTimer(MythicCore.getInstance(), ConfigLoader.getHealthBarUpdateRate(), ConfigLoader.getHealthBarUpdateRate());

        } else {
            updateHealthHologram(healthHolo);
        }
    }

    public String createHealthBar(double currentHealth, double maxHealth, double damaged) {

        if (customHealthBars.getOrDefault(entityType, defaultHealthBar).width() > 0) {

            int filledBars = (int) (currentHealth / maxHealth * customHealthBars.getOrDefault(entityType, defaultHealthBar).width());
            int damagedBars = (int) (Math.min(damaged, maxHealth) / maxHealth * customHealthBars.getOrDefault(entityType, defaultHealthBar).width());
            int emptyBars = customHealthBars.getOrDefault(entityType, defaultHealthBar).width() - filledBars - damagedBars;

            HealthBarSettings settings = customHealthBars.getOrDefault(entityType, defaultHealthBar);

            return settings.prefix() +
                    settings.healthColor() + settings.healthFiller().repeat(Math.max(0, filledBars)) +
                    settings.separator() +
                    settings.damagedColor() + settings.damagedFiller().repeat(Math.max(0, damagedBars)) +
                    settings.separator() +
                    settings.fillerColor() + settings.filler().repeat(Math.max(0, emptyBars)) +
                    settings.suffix();
        } else {
            return "";
        }
    }

    private void initializeHealthHologram(Hologram holo, @Nullable String mobType) {
        for (String ignored : customHealthBars.getOrDefault(mobType, defaultHealthBar).lines()) {
            DHAPI.addHologramLine(holo, "");
        }
    }

    private String replace(String string) {
        for (String key : textReplace.keySet()) {
            string = string.replaceAll(key, textReplace.getOrDefault(key, key));
        }
        return string;
    }

    @SuppressWarnings("DataFlowIssue")
    private void updateHealthHologram(Hologram holo) {
        for (int i = 0; i < customHealthBars.getOrDefault(entityType, defaultHealthBar).lines().length; i++) {
            String line = customHealthBars.getOrDefault(entityType, defaultHealthBar).lines()[i]
                    .replace("<dps>", DPSCheck.getDPS(entity.getUniqueId()))
                    .replace("<total_damage>", DPSCheck.getTotal(entity.getUniqueId()))
                    .replace("<aura>", aura)
                    .replace("<buff>", buff)
                    .replace("<name>", replace(name))
                    .replace("<name_uncolored>", replace(name_uncolored))
                    .replace("<health>", replace(health))
                    .replace("<max_health>", replace(max_health))
                    .replace("<bar>", health_bar);
            DHAPI.setHologramLine(holo, i, line);
        }
    }
}














