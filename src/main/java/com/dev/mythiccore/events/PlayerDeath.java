package com.dev.mythiccore.events;

import com.dev.mythiccore.MythicCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerDeath implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        MythicCore.getAuraManager().getAura(event.getEntity().getUniqueId()).clearAura();
        MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).clearBuff();
        MythicCore.getCooldownManager().getCooldown(event.getEntity().getUniqueId()).clearCooldown();
    }
}
