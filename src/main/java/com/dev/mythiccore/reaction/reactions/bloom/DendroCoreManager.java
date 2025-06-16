package com.dev.mythiccore.reaction.reactions.bloom;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Display.CMIBillboard;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.enums.MobType;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DendroCoreManager {

    public static final HashMap<Chunk, List<DendroCore>> dendroCoreInChunk = new HashMap<>();
    public static final HashMap<UUID, DendroCore> dendroCoreIds = new HashMap<>();

    public static void spawnDendroCore(Bloom instance, Location location, @Nullable LivingEntity owner, StatProvider statProvider, long life_time, EntityDamageEvent.DamageCause damage_cause, MobType mob_type) {

        if (location.getWorld() == null) return;

        UUID dendroCoreUUID = UUID.randomUUID();

        CMIHologram hologram = new CMIHologram("DENDRO_CORE_"+dendroCoreUUID, location);
        for (String line : instance.getConfig().getStringList("dendro-core.lines")) {
            hologram.addLine(line);
        }

        String billboard = instance.getConfig().getString("dendro-core.following-type");
        if (billboard.equalsIgnoreCase("ITEM")) {
            hologram.setNewDisplayMethod(false);
        } else {
            hologram.setNewDisplayMethod(true);
            hologram.setBillboard(CMIBillboard.valueOf(billboard));
        }

        hologram.setIconScale(instance.getConfig().getDouble("dendro-core.icon-scale"));

        hologram.setSkyLevel(15);
        hologram.setBlockLevel(15);
        hologram.setBackgroundAlpha(0);

        CMI.getInstance().getHologramManager().addHologram(hologram);
        hologram.update();

//        Entity entity = location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND, false);
//        ArmorStand entity_dendro_core = (ArmorStand) entity;
//
//        ItemStack equipment = new ItemStack(Material.valueOf(instance.getConfig().getString("dendro-core.material")));
//        ItemMeta equipment_meta = equipment.getItemMeta();
//        assert equipment_meta != null;
//        equipment_meta.setCustomModelData(instance.getConfig().getInt("dendro-core.model-data"));
//        equipment.setItemMeta(equipment_meta);
//
//        entity_dendro_core.setPersistent(false);
//        entity_dendro_core.setInvisible(true);
//        entity_dendro_core.setSmall(true);
//        entity_dendro_core.setInvulnerable(true);
//        if (entity_dendro_core.getEquipment() != null) entity_dendro_core.getEquipment().setHelmet(equipment);

        DendroCore dendroCore = new DendroCore(instance, dendroCoreUUID, owner, statProvider, life_time, hologram, damage_cause, mob_type);

        DendroCoreManager.dendroCoreIds.put(dendroCoreUUID, dendroCore);

//        entity_dendro_core.setMetadata("AST_DENDRO_CORE_ENTITY", new FixedMetadataValue(MythicCore.getInstance(), dendroCore));

        if (dendroCoreInChunk.containsKey(location.getChunk())) {
            List<DendroCore> dendroCores = new ArrayList<>(dendroCoreInChunk.get(location.getChunk()));
            dendroCores.add(dendroCore);
            dendroCoreInChunk.put(location.getChunk(), dendroCores);
            if (dendroCores.size() > 5) {
                dendroCores.get(0).ignite();
            }
        } else {
            dendroCoreInChunk.put(location.getChunk(), new ArrayList<>(List.of(dendroCore)));
        }
    }

    public static void dendroCoreTick() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(MythicCore.getInstance(), ()->{
            try {
                for (Chunk chunk : dendroCoreInChunk.keySet()) {
                    List<DendroCore> dendroCores = dendroCoreInChunk.get(chunk);
                    for (DendroCore dendroCore : dendroCores) {
                        if (dendroCore.getLifeTime() <= 1) {
                            Bukkit.getScheduler().runTask(MythicCore.getInstance(), dendroCore::ignite);
                        } else {
                            dendroCore.setLifeTime(dendroCore.getLifeTime() - 1);
                        }
                    }
                }
            } catch (ConcurrentModificationException ignored) {}
        }, 1, 1);
    }

}