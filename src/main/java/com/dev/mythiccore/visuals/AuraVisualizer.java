package com.dev.mythiccore.visuals;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.events.hp_bar.HpBar;
import com.dev.mythiccore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AuraVisualizer {

    public static final HashMap<UUID, TextDisplay> mapHologram = new HashMap<>();

    public static void start() {

        Bukkit.getScheduler().runTaskTimer(MythicCore.getInstance(), () -> {
            try {
                for (UUID uuid : mapHologram.keySet()) {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity == null || !entity.isValid() || entity.isDead() || !entity.getLocation().getChunk().isLoaded() || (!MythicCore.getAuraManager().getMapEntityAura().containsKey(uuid) && !HpBar.hpBars.containsKey(uuid)/*&& !MythicCore.getCooldownManager().getEntityCooldown().containsKey(uuid)) && !MythicCore.getBuffManager().getMapBuffData().containsKey(uuid)*/)) {
                        TextDisplay textDisplay = mapHologram.get(uuid);
                        textDisplay.remove();
                        mapHologram.remove(uuid);
                    }
                }

                List<UUID> uuids = new ArrayList<>(MythicCore.getAuraManager().getMapEntityAura().keySet());
                uuids.addAll(HpBar.hpBars.keySet());
                for (UUID uuid : uuids) {
                    Entity entity = Bukkit.getEntity(uuid);

                    if (entity instanceof Player) continue;

                    if (entity == null || !entity.isValid() || entity.isDead() || !entity.getLocation().getChunk().isLoaded()) {
                        if (!mapHologram.containsKey(uuid)) continue;

                        TextDisplay textDisplay = mapHologram.get(uuid);
                        textDisplay.remove();

                        mapHologram.remove(uuid);

                        continue;
                    }


                    BoundingBox boundingBox = entity.getBoundingBox();
                    float scale = (float)Math.max(boundingBox.getHeight() * 0.5, 1);
                    double height = boundingBox.getHeight() * 1.3;

                    Location spawnLocation = new Location(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY()+ height, entity.getLocation().getZ());

                    if (!mapHologram.containsKey(uuid)) {

                        TextDisplay textDisplay = entity.getWorld().spawn(spawnLocation, TextDisplay.class);
                        mapHologram.put(uuid, textDisplay);
                            textDisplay.setPersistent(false);
                            textDisplay.setBillboard(Display.Billboard.CENTER);
                            textDisplay.setText(Utils.colorize(MythicCore.getInstance().getConfig().getString("General.aura-visualizer").replace("{aura}", MythicCore.getAuraManager().getAura(uuid).getAuraIcon()).replace("{auraGaugeBar}", MythicCore.getAuraManager().getAura(uuid).getAuraGaugeBar()).replace("{name}", entity.getName()).replace("\\n", "\n").replace("{hpBar}", HpBar.getHpBar(uuid)))/* + " | " + MythicCore.getCooldownManager().getCooldown(uuid).getMapCooldown()*/);
                            textDisplay.setTransformation(new Transformation(textDisplay.getTransformation().getTranslation(), textDisplay.getTransformation().getLeftRotation(), new Vector3f(scale), textDisplay.getTransformation().getRightRotation()));
                            textDisplay.setShadowed(false);
                            textDisplay.setSeeThrough(true);
                            textDisplay.setBrightness(new Display.Brightness(15, 15));
                            textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                            textDisplay.setMetadata("AST_AURA_VISUALIZER", new FixedMetadataValue(MythicCore.getInstance(), true));

                    } else {
                        TextDisplay textDisplay = mapHologram.get(uuid);
                        textDisplay.setText(Utils.colorize(MythicCore.getInstance().getConfig().getString("General.aura-visualizer").replace("{aura}", MythicCore.getAuraManager().getAura(uuid).getAuraIcon()).replace("{auraGaugeBar}", MythicCore.getAuraManager().getAura(uuid).getAuraGaugeBar()).replace("{name}", entity.getName()).replace("\\n", "\n").replace("{hpBar}", HpBar.getHpBar(uuid))));
                        textDisplay.teleport(spawnLocation);
                    }
                }
            } catch (Exception ignored) {
            }
        }, 1, 1);
    }
}