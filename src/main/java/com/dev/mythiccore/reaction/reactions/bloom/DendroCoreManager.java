package com.dev.mythiccore.reaction.reactions.bloom;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.enums.MobType;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

public class DendroCoreManager {

    public static final HashMap<Chunk, List<DendroCore>> dendroCoreInChunk = new HashMap<>();

    public static void spawnDendroCore(Bloom instance, Location location, @Nullable LivingEntity owner, StatProvider statProvider, long life_time, EntityDamageEvent.DamageCause damage_cause, MobType mob_type) {

        if (location.getWorld() == null) return;
        Entity entity = location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND, false);
        ArmorStand entity_dendro_core = (ArmorStand) entity;

        ItemStack equipment = new ItemStack(Material.valueOf(instance.getConfig().getString("dendro-core.material")));
        ItemMeta equipment_meta = equipment.getItemMeta();
        assert equipment_meta != null;
        equipment_meta.setCustomModelData(instance.getConfig().getInt("dendro-core.model-data"));
        equipment.setItemMeta(equipment_meta);

        entity_dendro_core.setPersistent(false);
        entity_dendro_core.setInvisible(true);
        entity_dendro_core.setSmall(true);
        entity_dendro_core.setInvulnerable(true);
        if (entity_dendro_core.getEquipment() != null) entity_dendro_core.getEquipment().setHelmet(equipment);

        DendroCore dendroCore = new DendroCore(instance, owner, statProvider, life_time, entity_dendro_core, damage_cause, mob_type);

        entity_dendro_core.setMetadata("AST_DENDRO_CORE_ENTITY", new FixedMetadataValue(MythicCore.getInstance(), dendroCore));

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