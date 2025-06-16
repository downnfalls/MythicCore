package com.dev.mythiccore.reaction.reactions.bloom;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.enums.AttackSource;
import com.dev.mythiccore.library.attackMetadata.ASTAttackMetadata;
import com.dev.mythiccore.library.attackMetadata.ASTProjectileAttackMetadata;
import com.dev.mythiccore.listener.events.DendroCoreReactionEvent;
import com.dev.mythiccore.listener.events.attack.MiscAttackEvent;
import com.dev.mythiccore.listener.events.attack.MobAttackEvent;
import com.dev.mythiccore.listener.events.attack.PlayerAttackEvent;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.utils.Utils;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamagePacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DendroCoreTrigger implements Listener {

    @EventHandler
    public void dendroCore(PlayerAttackEvent event) {

        if (event.getAttack() instanceof ASTAttackMetadata astAttack && astAttack.getAttackSource() == AttackSource.REACTION) return;
        if (event.getAttack() instanceof ASTProjectileAttackMetadata astAttack && astAttack.getAttackSource() == AttackSource.REACTION) return;

        trigger(event.getDamage(), event.getEntity(), event.getAttacker().getPlayer(), event.getAttacker(), event.toBukkit().getCause());

    }

    @EventHandler
    public void dendroCore(MobAttackEvent event) {

        if (event.getAttack() instanceof ASTAttackMetadata astAttack && astAttack.getAttackSource() == AttackSource.REACTION) return;
        if (event.getAttack() instanceof ASTProjectileAttackMetadata astAttack && astAttack.getAttackSource() == AttackSource.REACTION) return;

        trigger(event.getDamage(), event.getEntity(), event.getDamager(), event.getAttack().getAttacker(), event.toBukkit().getCause());

    }

    @EventHandler
    public void dendroCore(MiscAttackEvent event) {

        trigger(event.getDamage(), event.getEntity(), null, s -> 0, event.toBukkit().getCause());

    }

    public void trigger(DamageMetadata damage, LivingEntity entity, @Nullable Entity damager, StatProvider stats, EntityDamageEvent.DamageCause damageCause) {

        double trigger_radius = ConfigLoader.getReactionConfig().getDouble("BLOOM.dendro-core-trigger-radius");

        List<DendroCore> dendroCores = getNearbyDendroCore(entity.getLocation(), trigger_radius);
        for (DendroCore dendroCore : dendroCores) {
            assert dendroCore != null;

            for (DamagePacket packet : damage.getPackets()) {
                if (packet.getElement() == null) continue;

                for (DendroCoreReaction reaction : MythicCore.getReactionManager().getDendroCoreReactions().values()) {
                    if (reaction.getTrigger().equals(packet.getElement().getId())) {
                        dendroCore.setIgnited(true);

                        Bukkit.getPluginManager().callEvent(new DendroCoreReactionEvent(damager, reaction));
                        if (!reaction.getDisplay().isEmpty()) Utils.displayIndicator(reaction.getDisplay(), dendroCore.getHologram().getLocation());

                        reaction.trigger(dendroCore, entity, damager, stats, damageCause);
                    }
                }
            }
        }
    }

    private List<DendroCore> getNearbyDendroCore(Location location, double range) {
        List<DendroCore> dendroCores = new ArrayList<>();

        for (DendroCore dendroCore : DendroCoreManager.dendroCoreIds.values()) {
            if (dendroCore.getHologram().getLocation().distance(location) <= range && !dendroCore.isIgnited()) {
                dendroCores.add(dendroCore);
            }
        }

        return dendroCores;
    }
}
