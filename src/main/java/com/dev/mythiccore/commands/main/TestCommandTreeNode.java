package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.commands.CommandTreeNode;
import com.dev.mythiccore.utils.Utils;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCommandTreeNode extends CommandTreeNode {
    public TestCommandTreeNode(CommandTreeNode parent) {
        super(parent, "test");
    }

    @Override
    public void execute(CommandSender sender, String command, String[] args) {
        if (sender instanceof Player player) {

            Utils.generateParticles(Particle.END_ROD, 3, 360, 0.2, player.getLocation(), 45, 0);

        }
    }
}
