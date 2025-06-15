package com.dev.mythiccore.stats;

import com.google.gson.*;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackCategory;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackProvider;
import io.lumine.mythic.lib.api.util.ui.PlusMinusPercent;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.api.util.message.FFPMMOItems;
import net.Indyuce.mmoitems.gui.edition.AbilityListEdition;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.skill.RegisteredSkill;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.random.RandomAbilityData;
import net.Indyuce.mmoitems.stat.data.random.RandomAbilityListData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.data.type.UpgradeInfo;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.Upgradable;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Abilities extends ItemStat<RandomAbilityListData, AbilityListData> implements Upgradable {
    public Abilities() {
        super("ABILITY", Material.BLAZE_POWDER, "Item Abilities (MythicCore)", new String[]{"Make your item cast amazing abilities", "to kill monsters or buff yourself."}, new String[]{"!block", "all"}, new Material[0]);
    }

    public RandomAbilityListData whenInitialized(Object object) {
        Validate.isTrue(object instanceof ConfigurationSection, "Must specify a valid config section");
        ConfigurationSection config = (ConfigurationSection)object;
        RandomAbilityListData list = new RandomAbilityListData(new RandomAbilityData[0]);

        for(String key : config.getKeys(false)) {
            list.add(new RandomAbilityData[]{new RandomAbilityData(config.getConfigurationSection(key))});
        }

        return list;
    }

    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull AbilityListData data) {
        List<String> abilityLore = new ArrayList();
        boolean splitter = !MMOItems.plugin.getLanguage().abilitySplitter.equals("");
        String modifierFormat = ItemStat.translate("ability-modifier");
        String abilityFormat = ItemStat.translate("ability-format");
        data.getAbilities().forEach((ability) -> {
            abilityLore.add(abilityFormat.replace("{trigger}", MMOItems.plugin.getLanguage().getCastingModeName(ability.getTrigger())).replace("{ability}", ability.getAbility().getName()));

            for(String modifier : ability.getModifiers()) {
                item.getLore().registerPlaceholder("ability_" + ability.getAbility().getHandler().getId().toLowerCase() + "_" + modifier, MythicLib.plugin.getMMOConfig().decimals.format(ability.getParameter(modifier)));
                abilityLore.add(modifierFormat.replace("{modifier}", ability.getAbility().getParameterName(modifier)).replace("{value}", MythicLib.plugin.getMMOConfig().decimals.format(ability.getParameter(modifier))));
            }

            if (splitter) {
                abilityLore.add(MMOItems.plugin.getLanguage().abilitySplitter);
            }

        });
        if (splitter && abilityLore.size() > 0) {
            abilityLore.remove(abilityLore.size() - 1);
        }

        item.getLore().insert("abilities", abilityLore);
        item.addItemTag(this.getAppliedNBT(data));
    }

    @NotNull
    public ArrayList<ItemTag> getAppliedNBT(@NotNull AbilityListData data) {
        ArrayList<ItemTag> ret = new ArrayList();
        JsonArray jsonArray = new JsonArray();

        for(AbilityData ab : data.getAbilities()) {
            jsonArray.add(ab.toJson());
        }

        ret.add(new ItemTag(this.getNBTPath(), jsonArray.toString()));
        return ret;
    }

    public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
        (new AbilityListEdition(inv.getPlayer(), inv.getEdited())).open(inv.getPage());
    }

    public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
        String configKey = (String)info[0];
        String edited = (String)info[1];
        String format = message.toUpperCase().replace("-", "_").replace(" ", "_").replaceAll("[^A-Z0-9_]", "");
        if (edited.equals("ast-ability")) {
            Validate.isTrue(MMOItems.plugin.getSkills().hasSkill(format), "format is not a valid ability! You may check the ability list using /mi list ability.");
            RegisteredSkill ability = MMOItems.plugin.getSkills().getSkill(format);
            inv.getEditedSection().set("ast-ability." + configKey, (Object)null);
            inv.getEditedSection().set("ast-ability." + configKey + ".type", format);
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully set the ability to " + ChatColor.GOLD + ability.getName() + ChatColor.GRAY + ".");
        } else if (edited.equals("mode")) {
            TriggerType castMode = TriggerType.valueOf(format);
            inv.getEditedSection().set("ast-ability." + configKey + ".mode", castMode.name());
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully set the trigger to " + ChatColor.GOLD + castMode.getName() + ChatColor.GRAY + ".");
        } else {
            (new NumericStatFormula(message)).fillConfigurationSection(inv.getEditedSection(), "ast-ability." + configKey + "." + edited, NumericStatFormula.FormulaSaveOption.NONE);
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GOLD + MMOUtils.caseOnWords(edited.replace("-", " ")) + ChatColor.GRAY + " successfully added.");
        }
    }

    public void whenDisplayed(List<String> lore, Optional<RandomAbilityListData> statData) {
        lore.add(ChatColor.GRAY + "Current Abilities: " + ChatColor.GOLD + (statData.isPresent() ? ((RandomAbilityListData)statData.get()).getAbilities().size() : 0));
        lore.add("");
        lore.add(ChatColor.YELLOW + "►" + " Click to edit the item abilities.");
    }

    @NotNull
    public AbilityListData getClearStatData() {
        return new AbilityListData(new AbilityData[0]);
    }

    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        ArrayList<ItemTag> relevantTags = new ArrayList();
        if (mmoitem.getNBT().hasTag(this.getNBTPath())) {
            relevantTags.add(ItemTag.getTagAtPath(this.getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.STRING));
        }

        AbilityListData data = this.getLoadedNBT(relevantTags);
        if (data != null) {
            mmoitem.setData(this, data);
        }

    }

    @Nullable
    public AbilityListData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {
        ItemTag jsonCompact = ItemTag.getTagAtPath(this.getNBTPath(), storedTags);
        if (jsonCompact != null) {
            try {
                AbilityListData list = new AbilityListData(new AbilityData[0]);

                for(JsonElement e : (new JsonParser()).parse((String)jsonCompact.getValue()).getAsJsonArray()) {
                    if (e.isJsonObject()) {
                        JsonObject obj = e.getAsJsonObject();
                        list.add(new AbilityData[]{new AbilityData(obj)});
                    }
                }

                return list;
            } catch (IllegalStateException | JsonSyntaxException var8) {
            }
        }

        return null;
    }

    @NotNull
    @Override
    public UpgradeInfo loadUpgradeInfo(@Nullable Object obj) throws IllegalArgumentException {
        return AbilitiesUpgradeInfo.GetFrom(obj);
    }

    @NotNull
    @Override
    public StatData apply(@NotNull StatData original, @NotNull UpgradeInfo info, int level) {


        if (original instanceof AbilityListData abilityListData && info instanceof Abilities.AbilitiesUpgradeInfo abilitiesUpgradeInfo) {

            for (AbilityData abilityData : abilityListData.getAbilities()) {
                String ability = abilityData.getAbility().getName().toLowerCase().replace(" ", "-").replace("_", "-");

                for (String modifier : abilityData.getModifiers()) {

                    var pmp = abilitiesUpgradeInfo.getPMP(ability, modifier);
                    if (pmp == null) continue;

                    double value = abilityData.getParameter(modifier);

                    value = pmp.apply(value);

                    abilityData.setModifier(modifier, value);

                }
            }

        }
        return original;
    }

    public static class AbilitiesUpgradeInfo implements UpgradeInfo {
        @NotNull
        Map<String, Map<String, PlusMinusPercent>> pmpMap;

        public AbilitiesUpgradeInfo(@NotNull Map<String, Map<String, PlusMinusPercent>> pmpMap) {
            this.pmpMap = pmpMap;
        }

        public PlusMinusPercent getPMP(String ability, String modifier) {
            return pmpMap.getOrDefault(ability, new HashMap<>()).getOrDefault(modifier, null);
        }

        @NotNull
        public static Abilities.AbilitiesUpgradeInfo GetFrom(@Nullable Object obj) throws IllegalArgumentException {
            Validate.notNull(obj, FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Upgrade operation must not be null"));

            Map<String, Map<String, PlusMinusPercent>> pmpMap = new HashMap<>();

            for (String ability : ((ConfigurationSection) obj).getKeys(false)) {

                Map<String, PlusMinusPercent> modMap = new HashMap<>();
                for (String modifier : ((ConfigurationSection) obj).getConfigurationSection(ability).getKeys(false)) {
                    double d = ((ConfigurationSection) obj).getDouble(ability+"."+modifier);

                    d /= 2.0;

                    String str = String.valueOf(d);

                    char c = str.charAt(0);
                    if (c == 's') {
                        str = str.substring(1);
                    } else if (c != '+' && c != '-' && c != 'n') {
                        str = '+' + str;
                    }

                    FriendlyFeedbackProvider ffp = new FriendlyFeedbackProvider(FFPMMOItems.get());
                    PlusMinusPercent pmpRead = PlusMinusPercent.getFromString(str, ffp);
                    if (pmpRead == null) {
                        throw new IllegalArgumentException(
                                ffp.getFeedbackOf(FriendlyFeedbackCategory.ERROR).get(0).forConsole(ffp.getPalette()));
                    }

                    modMap.put(modifier, pmpRead);
                }

                pmpMap.put(ability.toLowerCase().replace(" ", "-").replace("_", "-"), modMap);
            }

            return new Abilities.AbilitiesUpgradeInfo(pmpMap);
        }
    }
}