package com.dev.mythiccore.stats;

import com.dev.mythiccore.MythicCore;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.util.ui.SilentNumbers;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.UnsocketGemStoneEvent;
import net.Indyuce.mmoitems.api.interaction.Consumable;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ConsumableItemInteraction;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class RandomUnsocket extends DoubleStat implements ConsumableItemInteraction {
    public RandomUnsocket() {
        super("AST_RANDOM_UNSOCKET", Material.BOWL, "Random Unsocket (MythicCore)", new String[]{"Number of gems (rounded down)", "that will pop out of an item when", "this is applied."}, new String[]{"consumable"}, new Material[0]);
    }

    public boolean handleConsumableEffect(@NotNull InventoryClickEvent event, @NotNull PlayerData playerData, @NotNull Consumable consumable, @NotNull NBTItem target, Type targetType) {
        VolatileMMOItem consumableVol = consumable.getMMOItem();
        if (!consumableVol.hasData(MythicCore.RANDOM_UNSOCKET)) {
            return false;
        } else if (targetType == null) {
            return false;
        } else {
            MMOItem mmoVol = new VolatileMMOItem(target);
            if (!mmoVol.hasData(ItemStats.GEM_SOCKETS)) {
                return false;
            } else {
                GemSocketsData mmoGems = (GemSocketsData)mmoVol.getData(ItemStats.GEM_SOCKETS);
                if (mmoGems != null && mmoGems.getGemstones().size() != 0) {
                    Player player = playerData.getPlayer();
                    MMOItem mmo = new LiveMMOItem(target);
                    List<Pair<GemstoneData, MMOItem>> mmoGemStones = extractGemstones(mmo);
                    if (mmoGemStones.isEmpty()) {
                        Message.RANDOM_UNSOCKET_GEM_TOO_OLD.format(ChatColor.YELLOW, new String[]{"#item#", MMOUtils.getDisplayName(event.getCurrentItem())}).send(player);
                        return false;
                    } else {
                        DoubleData unsocket = (DoubleData)consumable.getMMOItem().getData(MythicCore.RANDOM_UNSOCKET);
                        int s = 1;
                        if (unsocket != null) {
                            s = SilentNumbers.floor(unsocket.getValue());
                        }

                        UnsocketGemStoneEvent unsocketGemStoneEvent = new UnsocketGemStoneEvent(playerData, consumableVol, mmo);
                        Bukkit.getServer().getPluginManager().callEvent(unsocketGemStoneEvent);
                        if (unsocketGemStoneEvent.isCancelled()) {
                            return false;
                        } else {
                            ArrayList<ItemStack> items2Drop = new ArrayList();

                            while(s > 0 && mmoGemStones.size() > 0) {
                                int randomGem = SilentNumbers.floor(SilentNumbers.randomRange((double)0.0F, (double)mmoGemStones.size()));
                                if (randomGem >= mmoGemStones.size()) {
                                    randomGem = mmoGemStones.size() - 1;
                                }

                                Pair<GemstoneData, MMOItem> pair = (Pair)mmoGemStones.get(randomGem);
                                MMOItem gem = (MMOItem)pair.getValue();
                                GemstoneData gemData = (GemstoneData)pair.getKey();
                                mmoGemStones.remove(randomGem);

                                try {
                                    ItemStack builtGem = gem.newBuilder().build();
                                    if (!SilentNumbers.isAir(builtGem)) {
                                        items2Drop.add(builtGem);
                                        String chosenColor;
                                        if (gemData.getSocketColor() != null) {
                                            chosenColor = gemData.getSocketColor();
                                        } else {
                                            chosenColor = GemSocketsData.getUncoloredGemSlot();
                                        }

                                        mmo.removeGemStone(gemData.getHistoricUUID(), chosenColor);
                                        --s;
                                        Message.RANDOM_UNSOCKET_SUCCESS.format(ChatColor.YELLOW, new String[]{"#item#", MMOUtils.getDisplayName(event.getCurrentItem()), "#gem#", MMOUtils.getDisplayName(builtGem)}).send(player);
                                    }
                                } catch (Throwable e) {
                                    MMOItems.print(Level.WARNING, "Could not unsocket gem from item $u{0}$b: $f{1}", "Stat §eRandom Unsocket", new String[]{SilentNumbers.getItemName(event.getCurrentItem()), e.getMessage()});
                                }
                            }

                            mmo.setData(ItemStats.GEM_SOCKETS, StatHistory.from(mmo, ItemStats.GEM_SOCKETS).recalculate(mmo.getUpgradeLevel()));
                            event.setCurrentItem(mmo.newBuilder().build());

                            for(ItemStack drop : player.getInventory().addItem((ItemStack[])items2Drop.toArray(new ItemStack[0])).values()) {
                                player.getWorld().dropItem(player.getLocation(), drop);
                            }

                            player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0F, 2.0F);
                            return true;
                        }
                    }
                } else {
                    return false;
                }
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

                    pairs.add(Pair.of(gem, restored));
                }
            }


            for(StatHistory hist : item.getStatHistories()) {
                for(Pair<GemstoneData, MMOItem> gem : pairs) {
                    StatData historicGemData = hist.getGemstoneData(gem.getKey().getHistoricUUID());
                    if (historicGemData != null) {
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

