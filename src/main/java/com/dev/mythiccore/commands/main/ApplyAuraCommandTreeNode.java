package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.commands.CommandTreeNode;
import com.dev.mythiccore.events.attack_handle.TriggerReaction;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.utils.Utils;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.damage.DamagePacket;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.element.Element;
import io.lumine.mythic.lib.player.PlayerMetadata;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApplyAuraCommandTreeNode extends CommandTreeNode {
    public ApplyAuraCommandTreeNode(CommandTreeNode parent) {
        super(parent, "apply-aura");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String command, String[] args) {
        List<String> output = new ArrayList<>();

        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            output = Utils.tabComplete(args[0], players);
        }
        else if (args.length == 2) {
            List<String> auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
            for (Element element : MythicLib.plugin.getElements().getAll()) {
                auras.add(element.getId());
            }
            output = Utils.tabComplete(args[1], auras);
        }
        else if (args.length == 3) {
            output = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("General.decay-rate")).getKeys(false));
        }
        return output;
    }

    @Override
    public void execute(CommandSender sender, String command, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 3) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    List<String> auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
                    List<String> elements = new ArrayList<>();
                    for (Element element : MythicLib.plugin.getElements().getAll()) {
                        auras.add(element.getId());
                        elements.add(element.getId());
                    }
                    if (auras.contains(args[1])) {
                        double gauge = Double.parseDouble(Utils.splitTextAndNumber(args[2])[0]);
                        String decay_rate = Utils.splitTextAndNumber(args[2])[1];

                        if (Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("General.decay-rate")).getKeys(false).contains(decay_rate)) {

                            List<String> allow_auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
                            allow_auras.addAll(ConfigLoader.getAuraWhitelist());

                            if (allow_auras.contains(args[1]))
                                MythicCore.getAuraManager().getAura(target.getUniqueId()).addAura(args[1], gauge, decay_rate);
                            if (gauge > 0) {
                                if (elements.contains(args[1]))
                                    TriggerReaction.triggerReactions(null, new DamagePacket(0, Element.valueOf(args[1]), DamageType.SKILL), gauge, decay_rate, target, player, new PlayerMetadata(PlayerData.get(player).getStats().getMap(), EquipmentSlot.MAIN_HAND), EntityDamageEvent.DamageCause.CUSTOM);
                            }
                            sender.sendMessage(ConfigLoader.getMessage("apply-aura-success", true)
                                    .replace("{aura}", args[1])
                                    .replace("{player}", args[0])
                                    .replace("{gauge}", args[2])
                            );
                        } else {
                            sender.sendMessage(ConfigLoader.getMessage("invalid-decay-rate", true));
                        }
                    } else {
                        sender.sendMessage(ConfigLoader.getMessage("invalid-aura", true));
                    }
                } else {
                    sender.sendMessage(ConfigLoader.getMessage("player-not-found", true));
                }
            } else {
                sender.sendMessage(ConfigLoader.getMessage("invalid-syntax", true));
            }
        } else {
            sender.sendMessage(ConfigLoader.getMessage("must-be-player", true));
        }
    }
}
