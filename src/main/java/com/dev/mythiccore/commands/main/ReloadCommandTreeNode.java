package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.commands.CommandTreeNode;
import com.dev.mythiccore.library.attributeModifier.BaseMaxHealthStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthPercentStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthStatHandler;
import com.dev.mythiccore.reaction.ReactionManager;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.visuals.HealthBar;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.util.ConfigFile;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;

public class ReloadCommandTreeNode extends CommandTreeNode {
    public ReloadCommandTreeNode(CommandTreeNode parent) {
        super(parent, "reload");
    }

    @Override
    public void execute(CommandSender sender, String command, String[] args) {

        MythicCore.getReactionManager().clearReactionMap();
        MythicCore.getReactionManager().clearDendroCoreReactionMap();
        MythicCore.getInstance().reloadConfig();
        ConfigLoader.loadConfig();
        ReactionManager.registerDefaultReactions();

        MythicLib.inst().getStats().registerStat(new BaseMaxHealthStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "MAX_HEALTH"));
        MythicLib.inst().getStats().registerStat(new MaxHealthPercentStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "AST_MAX_HEALTH_BUFF_PERCENT"));
        MythicLib.inst().getStats().registerStat(new MaxHealthStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "AST_MAX_HEALTH_BUFF"));

        HealthBar.defaultHealthBar = ConfigLoader.getDefaultHealthBar();
        HealthBar.customHealthBars = ConfigLoader.getCustomHealthBars();
        HealthBar.textReplace = ConfigLoader.getTextReplace();

        sender.sendMessage(ConfigLoader.getMessage("reload-success", true));

    }
}
