package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.commands.CommandTreeNode;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Test2CommandTreeNode extends CommandTreeNode {
    public Test2CommandTreeNode(CommandTreeNode parent) {
        super(parent, "test2");
    }

    @Override
    public void execute(CommandSender sender, String command, String[] args) {
        if (sender instanceof Player player) {

            if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                LiveMMOItem item = new LiveMMOItem(player.getInventory().getItemInMainHand());

                item.getStats().forEach(stat -> {
                    if (stat instanceof DoubleStat)
                        Bukkit.broadcastMessage(ChatColor.GRAY+stat.getId()+": "+item.getData(stat));
                });

                item.getStatHistories().forEach(his -> {
                    Bukkit.broadcastMessage(ChatColor.WHITE+his.getItemStat().getId()+"; "+his.getAllModifiers());
                    for (var mod : his.getAllModifiers()) {
                        Bukkit.broadcastMessage(ChatColor.DARK_GRAY+his.getModifiersBonus(mod).toString());
                    }
                });

            }
        }
    }
}
