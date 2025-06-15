package com.dev.mythiccore.api;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.utils.Utils;
import com.dev.mythiccore.visuals.HealthBar;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {
    @NotNull
    @Override
    public String getAuthor() {
        return "downYoutube2548";
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "mythiccore";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("auras")) {
            if (player.isOnline()) {
                return MythicCore.getAuraManager().getAura(player.getUniqueId()).getAuraIcon();
            }
        }
        if (params.equalsIgnoreCase("buffs")) {
            if (player.isOnline()) {
                return MythicCore.getBuffManager().getBuff(player.getUniqueId()).getBuffIcon();
            }
        }
        if (params.equalsIgnoreCase("health_bar")) {
            if (player.isOnline()) {
                if (HealthBar.removeTaskMap.containsKey(player.getUniqueId())) {
                    return HealthBar.removeTaskMap.get(player.getUniqueId()).getValue();
                }
            }
        }
        if (params.startsWith("stat_format_")){
            if (player.isOnline()) {
                String stat = params.substring(12).toUpperCase();
                PlayerData playerData = PlayerData.get(player);
                return Utils.Format(playerData.getStats().getStat(stat), "#,###.#");
            }
        }
        if (params.startsWith("stat_")){
            if (player.isOnline()) {

                String stat = params.substring(5).toUpperCase();
                PlayerData playerData = PlayerData.get(player);

                return String.valueOf(playerData.getStats().getStat(stat));
            }
        }
        if (params.startsWith("placeholder_format_")) {
            String key = params.substring(19);

            String value = MythicCore.getInstance().getConfig().getString("Placeholders." + key);

            String formula = value == null ? "0" : PlaceholderAPI.setPlaceholders(player, value);
            Expression expression = new ExpressionBuilder(formula).build();

            return Utils.Format(expression.evaluate(), "#,###.#");
        }

        if (params.startsWith("placeholder_")) {
            String key = params.substring(12);

            String value = MythicCore.getInstance().getConfig().getString("Placeholders." + key);

            String formula = value == null ? "0" : PlaceholderAPI.setPlaceholders(player, value);
            Expression expression = new ExpressionBuilder(formula).build();

            return Utils.Format(expression.evaluate(), "#.#");
        }

        if (params.startsWith("statistic_reaction_")) {
            String key = params.substring(19);
            NamespacedKey namespacedKey = new NamespacedKey(MythicCore.getInstance(), "reaction-statistic-"+key);

            if (player instanceof Player p) {
                PersistentDataContainer container = p.getPersistentDataContainer();
                Long value = container.get(namespacedKey, PersistentDataType.LONG);
                if (value != null) {
                    return value.toString();
                } else {
                    return "0";
                }
            } else {
                return "0";
            }
        }

        if (params.startsWith("statistic_dendrocore_reaction_")) {
            String key = params.substring(30);
            NamespacedKey namespacedKey = new NamespacedKey(MythicCore.getInstance(), "dendrocore-reaction-statistic-"+key);

            if (player instanceof Player p) {
                PersistentDataContainer container = p.getPersistentDataContainer();
                Long value = container.get(namespacedKey, PersistentDataType.LONG);
                if (value != null) {
                    return value.toString();
                } else {
                    return "0";
                }
            } else {
                return "0";
            }
        }

        if (params.startsWith("statistic_damageperhit")) {
            NamespacedKey namespacedKey = new NamespacedKey(MythicCore.getInstance(), "damageperhit-statistic");

            if (player instanceof Player p) {
                PersistentDataContainer container = p.getPersistentDataContainer();
                Double value = container.get(namespacedKey, PersistentDataType.DOUBLE);
                if (value != null) {
                    return String.valueOf(value.longValue());
                } else {
                    return "0";
                }
            } else {
                return "0";
            }
        }

        if (params.startsWith("class_details_")) {

            int index = Integer.parseInt(params.substring(14));

            PlayerData data = PlayerData.get(player.getUniqueId());
            List<String> description = data.getProfess().getDescription();
            return description.size() > index ? description.get(index) : "";
        }

        return "";
    }
}
