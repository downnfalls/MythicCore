package com.dev.mythiccore.events;

import com.dev.mythiccore.mythic.conditions.DoReactionCondition;
import com.dev.mythiccore.mythic.mechanics.apply.*;
import com.dev.mythiccore.mythic.mechanics.modify.SetAuraBarFormat;
import com.dev.mythiccore.mythic.mechanics.modify.SetDefense;
import com.dev.mythiccore.mythic.mechanics.modify.SetElementalDamage;
import com.dev.mythiccore.mythic.mechanics.modify.SetResistance;
import com.dev.mythiccore.mythic.placeholders.SnapshotPlaceholder;
import com.dev.mythiccore.mythic.targeters.EntitiesNearLocationTargeter;
import com.dev.mythiccore.mythic.targeters.EntitiesNearOriginOffsetTargeter;
import com.dev.mythiccore.mythic.targeters.OriginOffsetTargeter;
import com.dev.mythiccore.mythic.targeters.SnapshotTargeter;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicLoad implements Listener {

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("elemental_damage")) {
            event.register(new ElementalDamage(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("reduce_defense"))	{
            event.register(new ReduceDefense(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("reduce_resistance"))	{
            event.register(new ReduceResistance(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("elemental_shield"))	{
            event.register(new ElementalShield(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("set_elemental_damage"))	{
            event.register(new SetElementalDamage(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("set_defense"))	{
            event.register(new SetDefense(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("set_resistance"))	{
            event.register(new SetResistance(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("snapshot"))	{
            event.register(new Snapshot(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("sudoSnapshot"))	{
            event.register(new SudoSnapshot(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("clearAura"))	{
            event.register(new ClearAura(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("applyAura"))	{
            event.register(new ApplyAura(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("pullTo"))	{
            event.register(new PullToLocation(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("setAuraBar"))	{
            event.register(new SetAuraBarFormat(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("setMana"))	{
            event.register(new SetMana(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("addMana"))	{
            event.register(new AddMana(event.getConfig()));
        }
        else if (event.getMechanicName().equalsIgnoreCase("reduceMana"))	{
            event.register(new ReduceMana(event.getConfig()));
        }
    }

    @EventHandler
    public void onTargeterLoad(MythicTargeterLoadEvent event) {
        if (event.getTargeterName().equalsIgnoreCase("snapshot")) {
            event.register(new SnapshotTargeter(event.getContainer().getManager(), event.getConfig()));
        }
        else if (event.getTargeterName().equalsIgnoreCase("originOffset")) {
            event.register(new OriginOffsetTargeter(event.getContainer().getManager(), event.getConfig()));
        }
        else if (event.getTargeterName().equalsIgnoreCase("EntitiesNearOriginOffset") || event.getTargeterName().equalsIgnoreCase("ENOO")) {
            event.register(new EntitiesNearOriginOffsetTargeter(event.getContainer().getManager(), event.getConfig()));
        }
        else if (event.getTargeterName().equalsIgnoreCase("EntitiesNearLocation") || event.getTargeterName().equalsIgnoreCase("ENL")) {
            event.register(new EntitiesNearLocationTargeter(event.getContainer().getManager(), event.getConfig()));
        }
    }

    @EventHandler
    public void mythicReload(MythicReloadedEvent event) {
        SnapshotPlaceholder.register();
    }

    @EventHandler
    public void onConditionLoad(MythicConditionLoadEvent event) {
        if (event.getConditionName().equalsIgnoreCase("doReaction")) {
            event.register(new DoReactionCondition(event.getConfig()));
        }
    }
}
