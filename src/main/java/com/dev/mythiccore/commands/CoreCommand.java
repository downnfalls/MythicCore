package com.dev.mythiccore.commands;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.events.attack_handle.TriggerReaction;
import com.dev.mythiccore.library.attributeModifier.BaseMaxHealthStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthPercentStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthStatHandler;
import com.dev.mythiccore.reaction.ReactionManager;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.utils.Utils;
import com.dev.mythiccore.visuals.HealthBar;
import de.tr7zw.nbtapi.NBTItem;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.damage.DamagePacket;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.element.Element;
import io.lumine.mythic.lib.player.PlayerMetadata;
import io.lumine.mythic.lib.util.ConfigFile;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.type.Mergeable;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class CoreCommand implements CommandExecutor, TabExecutor {

    public static Entity entity;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload")) {
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
                else if (args[0].equals("apply-aura")) {
                    if (args.length == 4) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target != null) {
                            List<String> auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
                            List<String> elements = new ArrayList<>();
                            for (Element element : MythicLib.plugin.getElements().getAll()) {
                                auras.add(element.getId());
                                elements.add(element.getId());
                            }
                            if (auras.contains(args[2])) {
                                double gauge = Double.parseDouble(Utils.splitTextAndNumber(args[3])[0]);
                                String decay_rate = Utils.splitTextAndNumber(args[3])[1];

                                if (Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("General.decay-rate")).getKeys(false).contains(decay_rate)) {

                                    List<String> allow_auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
                                    allow_auras.addAll(ConfigLoader.getAuraWhitelist());

                                    if (allow_auras.contains(args[2]))
                                        MythicCore.getAuraManager().getAura(target.getUniqueId()).addAura(args[2], gauge, decay_rate);
                                    if (gauge > 0) {
                                        if (elements.contains(args[2]))
                                            TriggerReaction.triggerReactions(null, new DamagePacket(0, Element.valueOf(args[2]), DamageType.SKILL), gauge, decay_rate, target, player, new PlayerMetadata(PlayerData.get(player).getStats().getMap(), EquipmentSlot.MAIN_HAND), EntityDamageEvent.DamageCause.CUSTOM);
                                    }
                                    sender.sendMessage(ConfigLoader.getMessage("apply-aura-success", true)
                                            .replace("{aura}", args[2])
                                            .replace("{player}", args[1])
                                            .replace("{gauge}", args[3])
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
                        sender.sendMessage(ConfigLoader.getMessage("syntax-error", true));
                    }
                } else if (args[0].equals("nbt")) {
                    NBTItem nbtItem = new NBTItem(player.getInventory().getItemInMainHand());
                    Bukkit.broadcastMessage(String.valueOf(nbtItem.getLong("MMOITEMS_AST_INTERNAL_COOLDOWN")));
                } else if (args[0].equals("remove-entity")) {
                    if (args.length >= 2) {
                        int radius = Integer.parseInt(args[1]);
                        try {
                            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                                if (entity.hasMetadata("NPC")) continue;
                                entity.remove();
                            }
                        } catch (UnsupportedOperationException ignored) {}
                    } else {
                        sender.sendMessage(ConfigLoader.getMessage("syntax-error", true));
                    }
                } else if (args[0].equals("test")) {

                    /*
                    double radius = 5;
                    int points = 360;

                    Vector direction = (player.getLocation().clone().add(0, -1, 0).subtract(player.getLocation())).toVector();
                    double thetaX = Math.acos(direction.getX() / direction.length());
                    double thetaY = Math.acos(direction.getY() / direction.length());
                    double thetaZ = Math.acos(direction.getZ() / direction.length());

                    double angleIncrement = 360.0 / points;
                    for (int i = 0; i < points; i++) {
                        double phi = Math.PI * i / points; // Random polar angle
                        double theta = 2 * Math.PI * i / points; // Random azimuthal angle

                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.sin(phi) * Math.sin(theta);
                        double z = radius * Math.cos(phi);


                        player.getLocation().getWorld().spawnParticle(
                                Particle.END_ROD, // particle
                                player.getLocation().clone().add(0, 0.15, 0), // location
                                0, // count
                                x, y, z, // offset
                                0.1, // speed
                                null, // Object: data
                                true // force
                        );
                    }

                     */
                } else if (args[0].equalsIgnoreCase("extract-gemstones")) {

                    if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                        LiveMMOItem item = new LiveMMOItem(player.getInventory().getItemInMainHand());

                        //MMOItems.plugin.getTypes().register(new Type());

                        // item.setData(ItemStats.LORE_FORMAT, new StringData(""));

                        List<Pair<GemstoneData, MMOItem>> gemstones = item.extractGemstones();
                        for (Pair<GemstoneData, MMOItem> gemstoneData : gemstones) {
                            player.getInventory().setItemInMainHand(
                                    addGemStone(item, gemstoneData.getValue())
                            );
                            break;
                        }

//                        Bukkit.broadcastMessage(String.valueOf(((DoubleData) item.getData(ItemStats.MAX_HEALTH)).getValue()));
//                        item.setData(ItemStats.MAX_HEALTH, new DoubleData(100));
//                        Bukkit.broadcastMessage(String.valueOf(((DoubleData) item.getData(ItemStats.MAX_HEALTH)).getValue()));


//                        for (GemstoneData gemstoneData : gemstones) {
//                            MMOItem mmoItem = item.extractGemstone(gemstoneData);
//                            player.getInventory().addItem(
//                                    mmoItem.newBuilder().build()
//                            );
//                        }

//                        player.getInventory().setItemInMainHand(
//                                new LiveMMOItem(item.newBuilder().build()).newBuilder().build()
//                        );

                    }
                } else if (args[0].equalsIgnoreCase("test2")) {

                }

                else {
                    sender.sendMessage(ConfigLoader.getMessage("syntax-error", true));
                }
            } else {
                sender.sendMessage(ConfigLoader.getMessage("syntax-error", true));
            }
        } else {
            sender.sendMessage(ChatColor.RED+"You must be player to execute this command!");
        }

        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        List<String> output = new ArrayList<>();
        if (args.length == 1) {
            List<String> list = List.of("apply-aura", "nbt", "test2", "remove-entity", "test", "reload", "extract-gemstones");
            output = tabComplete(args[0], list);
        }
        if (args.length > 1) {
            if (args[0].equals("apply-aura")) {
                if (args.length == 2) {
                    List<String> players = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        players.add(player.getName());
                    }
                    output = tabComplete(args[1], players);
                }
                else if (args.length == 3) {
                    List<String> auras = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("Special-Aura")).getKeys(false));
                    for (Element element : MythicLib.plugin.getElements().getAll()) {
                        auras.add(element.getId());
                    }
                    output = tabComplete(args[2], auras);
                }
                else if (args.length == 4) {
                    output = new ArrayList<>(Objects.requireNonNull(MythicCore.getInstance().getConfig().getConfigurationSection("General.decay-rate")).getKeys(false));
                }
            }
        }

        return output;
    }

    public static List<String> tabComplete(String a, List<String> arg) {
        List<String> matches = new ArrayList<>();
        String search = a.toLowerCase(Locale.ROOT);
        for (String s : arg) {
            if (s.toLowerCase(Locale.ROOT).startsWith(search)) {
                matches.add(s);
            }
        }
        return matches;
    }

    public ItemStack addGemStone(MMOItem mmoItem, MMOItem gemstone) {
        if (mmoItem.hasData(ItemStats.GEM_SOCKETS)) {

            GemSocketsData socketsData = (GemSocketsData) mmoItem.getData(ItemStats.GEM_SOCKETS);

            if (!socketsData.getEmptySlots().isEmpty()) {

                io.lumine.mythic.lib.api.item.NBTItem gemNBT = gemstone.newBuilder().buildNBT();
                LiveMMOItem gemMMOItem = new LiveMMOItem(gemNBT);
                String gemType = gemNBT.getString(ItemStats.GEM_COLOR.getNBTPath());
                String foundSocketColor = socketsData.getEmptySocket(gemType);

                GemstoneData gemData = new GemstoneData(gemMMOItem, foundSocketColor);

                socketsData.apply(socketsData.getEmptySlots().get(0), gemData);

                for (ItemStat<?, ?> stat : gemstone.getStats()) {
                    if (!(stat instanceof GemStoneStat)) {

                        // Get the stat data
                        StatData data = gemstone.getData(stat);

                        // If the data is MERGEABLE
                        if (data instanceof Mergeable) {
                            //UPGRD//MMOItems.log("\u00a79>>> \u00a77Gem-Merging \u00a7c" + stat.getNBTPath());
                            // Merge into it
                            //Bukkit.broadcastMessage(stat.getId()+((data instanceof DoubleData doubleData) ? doubleData.getValue() : 0));
                            mmoItem.mergeData(stat, data, gemData.getHistoricUUID());
                        }
                    }
                }
            }


//            StatHistory gemStory = StatHistory.from(mmoItem, ItemStats.GEM_SOCKETS);
//
//            for (UUID uuid : gemStory.getAllGemstones()) {
//                Bukkit.broadcastMessage("E");
//                ((GemSocketsData) gemStory.perGemstoneData.get(uuid)).add(gemstoneData);
//            }
            //((GemSocketsData)gemStory.getOriginalData()).add(gemstoneData);
        }
        return mmoItem.newBuilder().build();
    }

}
