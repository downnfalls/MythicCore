package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.commands.CommandTreeNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MythicCoreCommand extends CommandTreeNode implements CommandExecutor, TabExecutor {
    public MythicCoreCommand(CommandTreeNode parent) {
        super(parent, "mythiccore");

        addChild(new ReloadCommandTreeNode(this));
        addChild(new ApplyAuraCommandTreeNode(this));
        addChild(new TestCommandTreeNode(this));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command label, @NotNull String command, @NotNull String[] args) {
        super.execute(sender, command, args);
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command label, @NotNull String command, @NotNull String[] args) {
        return super.tabComplete(sender, command, args);
    }
}
