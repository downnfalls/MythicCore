package com.dev.mythiccore.commands.main;

import com.dev.mythiccore.commands.CommandTreeNode;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import net.Indyuce.mmoitems.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestCommandTreeNode extends CommandTreeNode {
    public TestCommandTreeNode(CommandTreeNode parent) {
        super(parent, "test");
    }

    @Override
    public void execute(CommandSender sender, String command, String[] args) {
        if (sender instanceof Player player) {

            if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                LiveMMOItem item = new LiveMMOItem(player.getInventory().getItemInMainHand());

                for (StatHistory history : item.getStatHistories()) {
                    Bukkit.broadcastMessage(ChatColor.GOLD+history.getItemStat().getId()+": "+history.getAllGemstones());
                }

                List<Pair<GemstoneData, MMOItem>> gemstones = extractGemstones(item);

                for (var gemstone : gemstones) {
                    GemstoneData gemstoneData = gemstone.getKey();
                    MMOItem gemstoneItem = gemstone.getValue();
                    for (StatHistory history : gemstoneItem.getStatHistories()) {
                        Bukkit.broadcastMessage(ChatColor.AQUA+history.getItemStat().getId()+": "+history.getAllModifiers());
//                        for (UUID mod : history.getAllModifiers()) {
//                            //history.removeModifierBonus(mod);
//                            //gemstoneItem.removeData(history.getItemStat());
//                        }
                    }
//
                    gemstoneItem.getStats().forEach(stat -> {
                        if (gemstoneItem.getData(stat) instanceof DoubleData doubleData)
                            Bukkit.broadcastMessage(ChatColor.YELLOW+stat.getId()+": "+doubleData.getValue());
                    });
//
//                    player.getInventory().addItem(gemstoneItem.newBuilder().build());
                    player.getInventory().addItem(gemstoneItem.newBuilder().build());

                    item.removeGemStone(gemstoneData.getHistoricUUID(), gemstoneData.getSocketColor());
                }

                for (StatHistory history : item.getStatHistories()) {
                    for (UUID gemUUID : history.getAllGemstones()) {
                        history.removeGemData(gemUUID);
                    }
                }

                player.getInventory().setItemInMainHand(item.newBuilder().build());

//                for (StatHistory history : item.getStatHistories()) {
//                    Bukkit.broadcastMessage(history.getItemStat().getId()+"; ");
//                    for (UUID mod : history.getAllModifiers()) {
//                        Bukkit.broadcastMessage("  "+mod+": "+history.getModifiersBonus(mod));
//                    }
//                }
            }
        }
    }

    public List<Pair<GemstoneData, MMOItem>> extractGemstones(MMOItem item) {
        GemSocketsData thisSocketsData = (GemSocketsData) item.getData(ItemStats.GEM_SOCKETS);
        if (thisSocketsData == null) {
            return new ArrayList();
        } else {
            List<Pair<GemstoneData, MMOItem>> pairs = new ArrayList();

            for(GemstoneData gem : thisSocketsData.getGemstones()) {

                MMOItem restored = getMMOItem(MMOItems.plugin.getTypes().get(gem.getMMOItemType()), gem.getMMOItemID(), true);
                if (restored != null) {

//                    for (StatHistory history : restored.getStatHistories()) {
//                        history.clearModifiersBonus();
//                    }

                    pairs.add(Pair.of(gem, restored));
                }
            }


            for(StatHistory hist : item.getStatHistories()) {
                for(Pair<GemstoneData, MMOItem> gem : pairs) {
                    StatData historicGemData = hist.getGemstoneData(gem.getKey().getHistoricUUID());
                    if (historicGemData != null) {
                        if (gem.getValue().getStatHistory(hist.getItemStat()) != null)
                            Bukkit.broadcastMessage(ChatColor.GREEN+"Modifiers: "+ gem.getValue().getStatHistory(hist.getItemStat()).getAllModifiers());
                        Bukkit.broadcastMessage(ChatColor.GREEN+hist.getItemStat().getId()+": "+historicGemData);
                        gem.getValue().setData(hist.getItemStat(), historicGemData);
                    }
                }
            }

            return pairs;

        }
    }

    public MMOItem getMMOItem(@Nullable Type type, @Nullable String id, boolean forDisplay) {
        if (type != null && id != null) {
            MMOItemTemplate found = MMOItems.plugin.getTemplates().getTemplate(type, id);
            return found == null ? null : new MMOItemBuilder(found, 0, null, forDisplay).build();
        } else {
            return null;
        }
    }
}
