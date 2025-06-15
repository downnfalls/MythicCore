package com.dev.mythiccore.events;

import com.dev.mythiccore.MythicCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        MythicCore.getAuraManager().getAura(event.getEntity().getUniqueId()).clearAura();
        MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).clearBuff();
        MythicCore.getCooldownManager().getCooldown(event.getEntity().getUniqueId()).clearCooldown();
    }
}
