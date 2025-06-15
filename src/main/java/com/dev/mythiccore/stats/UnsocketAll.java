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
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import net.Indyuce.mmoitems.stat.type.ConsumableItemInteraction;
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

public class UnsocketAll extends BooleanStat implements ConsumableItemInteraction {
    public UnsocketAll() {
        super("UNSOCKET_ALL", Material.BOWL, "Unsocket All",
                new String[] { "Toggle weather unsocket gemstones", "that will pop out of an item when", "this is applied." },
                new String[] { "consumable" });
    }

    @Override
    public boolean handleConsumableEffect(@NotNull InventoryClickEvent event, @NotNull PlayerData playerData, @NotNull Consumable consumable, @NotNull NBTItem target, Type targetType) {

        /*
         * Must also check that the consumable itself does have this stat... bruh
         */
        VolatileMMOItem consumableVol = consumable.getMMOItem();
        if (!consumableVol.hasData(MythicCore.UNSOCKET_ALL)) { return false; }

        /*
         * Cancel if the target is just not an MMOItem
         */
        if (targetType == null) { return false; }

        /*
         * No Gemstones? No service
         */
        MMOItem mmoVol = new VolatileMMOItem(target);
        if (!mmoVol.hasData(ItemStats.GEM_SOCKETS)) { return false; }
        GemSocketsData mmoGems = (GemSocketsData) mmoVol.getData(ItemStats.GEM_SOCKETS);
        if (mmoGems == null || mmoGems.getGemstones().size() == 0) { return false; }
        Player player = playerData.getPlayer();

        /*
         * All right do it correctly I guess.
         *
         * Cancel if no gem could be extracted.
         */
        MMOItem mmo = new LiveMMOItem(target);
        List<Pair<GemstoneData, MMOItem>> mmoGemStones = extractGemstones(mmo);
        if (mmoGemStones.isEmpty()) {
            Message.RANDOM_UNSOCKET_GEM_TOO_OLD.format(ChatColor.YELLOW, "#item#", MMOUtils.getDisplayName(event.getCurrentItem())).send(player);
            return false; }

        // Remove when call event successfully
        UnsocketGemStoneEvent unsocketGemStoneEvent = new UnsocketGemStoneEvent(playerData, consumableVol, mmo);
        Bukkit.getServer().getPluginManager().callEvent(unsocketGemStoneEvent);
        if (unsocketGemStoneEvent.isCancelled()) return false;

        // Drop gemstones to the ground :0
        ArrayList<ItemStack> items2Drop = new ArrayList<>();
        while (!mmoGemStones.isEmpty()) {

            /*
             * Choose a gem to drop :)
             */
            int randomGem = SilentNumbers.floor(SilentNumbers.randomRange(0, mmoGemStones.size()));
            if (randomGem >= mmoGemStones.size()) { randomGem = mmoGemStones.size() - 1;}

            // Choose gem
            final Pair<GemstoneData, MMOItem> pair = mmoGemStones.get(randomGem);
            final MMOItem gem = pair.getValue();
            final GemstoneData gemData = pair.getKey();

            mmoGemStones.remove(randomGem);
            //GEM//MMOItems.log("\u00a73   *\u00a77 Chose to remove\u00a7b " + gem.getType() + " " + gem.getId());

            try {

                // Drop?

                ItemStack builtGem = gem.newBuilder().build();
                //GEM//MMOItems.log("\u00a73   *\u00a77 Built " + SilentNumbers.getItemName(builtGem));

                // Valid?
                if (!SilentNumbers.isAir(builtGem)) {

                    // Drop
                    items2Drop.add(builtGem);
                    String chosenColor;
                    if (gemData.getSocketColor() != null) {
                        //GEM//MMOItems.log("\u00a7b   *\u00a77 Restored slot\u00a7e " + gem.getAsGemColor());
                        chosenColor = gemData.getSocketColor(); } else {
                        //GEM//MMOItems.log("\u00a7b   *\u00a77 Restored slot\u00a76 " + GemSocketsData.getUncoloredGemSlot() + " \u00a78(Uncolored Def)");
                        chosenColor = GemSocketsData.getUncoloredGemSlot(); }

                    // Unregister
                    mmo.removeGemStone(gemData.getHistoricUUID(), chosenColor);

                    // Message
                    Message.RANDOM_UNSOCKET_SUCCESS.format(ChatColor.YELLOW, "#item#", MMOUtils.getDisplayName(event.getCurrentItem()), "#gem#", MMOUtils.getDisplayName(builtGem)).send(player);
                }

            } catch (Throwable e) { MMOItems.print(Level.WARNING, "Could not unsocket gem from item $u{0}$b: $f{1}", "Stat \u00a7eRandom Unsocket", SilentNumbers.getItemName(event.getCurrentItem()), e.getMessage()); }
        }

        // Replace
        //HSY//MMOItems.log(" \u00a73-\u00a7a- \u00a77Gem Unsocketing Recalculation \u00a73-\u00a7a-\u00a73-\u00a7a-\u00a73-\u00a7a-\u00a73-\u00a7a-");
        mmo.setData(ItemStats.GEM_SOCKETS, StatHistory.from(mmo, ItemStats.GEM_SOCKETS).recalculate(mmo.getUpgradeLevel()));
        //GEM//MMOItems.log("\u00a7b*\u00a77 Final at \u00a7b" + ((GemSocketsData) mmo.getData(ItemStats.GEM_SOCKETS)).getEmptySlots().size() + " Empty\u00a77 and \u00a7e" + ((GemSocketsData) mmo.getData(ItemStats.GEM_SOCKETS)).getGemstones().size() + " Gems");
        event.setCurrentItem(mmo.newBuilder().build());

        // Give the gems back
        for (ItemStack drop : player.getInventory().addItem(items2Drop.toArray(new ItemStack[0])).values()) player.getWorld().dropItem(player.getLocation(), drop);
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1, 2);
        return true;
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