package com.dev.mythiccore.library.attributeModifier;

import io.lumine.mythic.lib.api.stat.StatInstance;
import io.lumine.mythic.lib.api.stat.handler.AttributeStatHandler;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BaseMaxHealthStatHandler extends AttributeStatHandler {
    public BaseMaxHealthStatHandler(@NotNull ConfigurationSection config, @NotNull Attribute attribute, @NotNull String stat) {
        super(config, attribute, stat);
    }

    @Override
    public void runUpdate(@NotNull StatInstance instance) {

        Player player = instance.getMap().getPlayerData().getPlayer();
        double percentHP = player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        super.runUpdate(instance);

        player.setHealth(Math.min(percentHP, 1) * player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    }
}