package com.dev.mythiccore.stats.elemental_stat;

import com.dev.mythiccore.MythicCore;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.element.Element;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ASTElementsEdition extends EditionInventory {
    private final List<Element> elements = new ArrayList<>();
    private final int maxPage;
    private final Map<Integer, Pair<Element, ASTElementStatType>> editableStats = new HashMap<>();
    private int page = 1;
    private static final int[] INIT_SLOTS = new int[]{19, 22, 28, 31, 37, 40};

    public ASTElementsEdition(Player player, MMOItemTemplate template) {
        super(player, template);
        this.elements.addAll(MythicLib.plugin.getElements().getAll());
        this.maxPage = 1 + (MythicLib.plugin.getElements().getAll().size() - 1) / 6;
    }

    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(this, 54, "Elements: " + this.template.getId());
        Optional<ASTRandomElementStatData> statData = this.getEventualStatData(MythicCore.ELEMENT_STAT);
        ItemStack prevPage = new ItemStack(Material.ARROW);
        ItemMeta prevPageMeta = prevPage.getItemMeta();
        prevPageMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
        prevPage.setItemMeta(prevPageMeta);
        inv.setItem(25, prevPage);
        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.setDisplayName(ChatColor.GREEN + "Next Page");
        nextPage.setItemMeta(nextPageMeta);
        inv.setItem(43, nextPage);
        this.editableStats.clear();
        int startingIndex = (this.page - 1) * 6;

        for(int i = 0; i < 6; ++i) {
            int index = startingIndex + i;
            if (index >= this.elements.size()) {
                break;
            }

            Element element = this.elements.get(index);
            int k = 0;

            for (ASTElementStatType statType : ASTElementStatType.values()) {
                ItemStack statItem = new ItemStack(element.getIcon());
                ItemMeta statMeta = statItem.getItemMeta();
                statMeta.setDisplayName(ChatColor.GREEN + element.getName() + " " + statType.getName());
                List<String> statLore = new ArrayList();
                statLore.add(ChatColor.GRAY + "Current Value: " + ChatColor.GREEN + (statData.isPresent() && statData.get().hasStat(element, statType) ? statData.get().getStat(element, statType) : "---"));
                statLore.add("");
                statLore.add(ChatColor.YELLOW + "►" + " Click to change this value.");
                statLore.add(ChatColor.YELLOW + "►" + " Right click to remove this value.");
                statMeta.setLore(statLore);
                statItem.setItemMeta(statMeta);
                int slot = INIT_SLOTS[i] + k;
                inv.setItem(slot, statItem);
                this.editableStats.put(slot, Pair.of(element, statType));
                ++k;
            }
        }

        this.addEditionInventoryItems(inv, true);
        return inv;
    }

    public void whenClicked(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        event.setCancelled(true);
        if (event.getInventory() == event.getClickedInventory() && MMOUtils.isMetaItem(item, false)) {
            if (this.page > 1 && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Previous Page")) {
                --this.page;
                this.open();
            } else if (this.page < this.maxPage && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Next Page")) {
                ++this.page;
                this.open();
            } else {
                Pair<Element, ASTElementStatType> edited = this.editableStats.get(event.getSlot());
                if (edited != null) {
                    String elementPath = edited.getValue().getConcatenatedConfigPath(edited.getKey());
                    if (event.getAction() == InventoryAction.PICKUP_ALL) {
                        (new StatEdition(this, MythicCore.ELEMENT_STAT, elementPath)).enable("Write in the value you want.");
                    } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                        this.getEditedSection().set("ast-element." + elementPath, null);
                        String elementName = edited.getKey().getId();
                        if (this.getEditedSection().contains("ast-element." + elementName) && this.getEditedSection().getConfigurationSection("ast-element." + elementName).getKeys(false).isEmpty()) {
                            this.getEditedSection().set("ast-element." + elementName, null);
                            if (this.getEditedSection().getConfigurationSection("ast-element").getKeys(false).isEmpty()) {
                                this.getEditedSection().set("ast-element", null);
                            }
                        }

                        this.registerTemplateEdition();
                        this.player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + edited.getKey().getName() + " " + edited.getValue().getName() + ChatColor.GRAY + " successfully removed.");
                    }
                }
            }
        }
    }
}
