package com.dev.mythiccore;

import com.dev.mythiccore.api.PlaceholderHook;
import com.dev.mythiccore.aura.Aura;
import com.dev.mythiccore.buff.Buff;
import com.dev.mythiccore.combat.Combat;
import com.dev.mythiccore.commands.main.MythicCoreCommand;
import com.dev.mythiccore.cooldown.InternalCooldown;
import com.dev.mythiccore.events.*;
import com.dev.mythiccore.events.attack_handle.*;
import com.dev.mythiccore.events.attack_handle.deal_damage.MiscAttack;
import com.dev.mythiccore.events.attack_handle.deal_damage.MobAttack;
import com.dev.mythiccore.events.attack_handle.deal_damage.PlayerAttack;
import com.dev.mythiccore.events.dps_check.DPSCheck;
import com.dev.mythiccore.library.attributeModifier.BaseMaxHealthStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthPercentStatHandler;
import com.dev.mythiccore.library.attributeModifier.MaxHealthStatHandler;
import com.dev.mythiccore.listener.AttackEventListener;
import com.dev.mythiccore.reaction.ReactionManager;
import com.dev.mythiccore.reaction.reactions.bloom.DendroCore;
import com.dev.mythiccore.reaction.reactions.bloom.DendroCoreManager;
import com.dev.mythiccore.reaction.reactions.bloom.DendroCoreTrigger;
import com.dev.mythiccore.reaction.reactions.frozen.FreezeEffect;
import com.dev.mythiccore.statistic.DamagePerHitStatistic;
import com.dev.mythiccore.statistic.DendroCoreReactionStatistic;
import com.dev.mythiccore.statistic.ReactionStatistic;
import com.dev.mythiccore.stats.*;
import com.dev.mythiccore.stats.elemental_stat.ASTElements;
import com.dev.mythiccore.utils.ConfigLoader;
import com.dev.mythiccore.visuals.DamageIndicatorEvent;
import com.dev.mythiccore.visuals.HealthBar;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import io.lumine.mythic.lib.util.ConfigFile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Objects;

public final class MythicCore extends JavaPlugin {

    public static ASTElements ELEMENT_STAT;
    public static UnsocketAll UNSOCKET_ALL;
    public static RemoveGemstone REMOVE_GEMSTONE;
    public static RandomUnsocket RANDOM_UNSOCKET;

    private static MythicCore instance;
    private static Aura aura;
    private static Buff buff;
    private static InternalCooldown cooldown;
    private static ReactionManager reactionManager;
    private PlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {


        instance = this;
        aura = new Aura();
        buff = new Buff();
        cooldown = new InternalCooldown();
        reactionManager = new ReactionManager();
        ELEMENT_STAT = new ASTElements();
        UNSOCKET_ALL = new UnsocketAll();
        REMOVE_GEMSTONE = new RemoveGemstone();
        RANDOM_UNSOCKET = new RandomUnsocket();

//        loadResource(this, "config.yml");
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        ConfigLoader.loadConfig();
        aura.startTick();
        buff.startTick();
        cooldown.startTick();
        FreezeEffect.effectApplier();

        DendroCoreManager.dendroCoreTick();

        Objects.requireNonNull(Bukkit.getPluginCommand("mythiccore")).setExecutor(new MythicCoreCommand(null));

        //Register EventListener
        Bukkit.getPluginManager().registerEvents(new MythicLoad(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerAttack(), this);
        Bukkit.getPluginManager().registerEvents(new MobAttack(), this);
        Bukkit.getPluginManager().registerEvents(new MiscAttack(), this);
        Bukkit.getPluginManager().registerEvents(new AttackModifier(), this);
        Bukkit.getPluginManager().registerEvents(new AttackEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new CancelFireTick(), this);
        Bukkit.getPluginManager().registerEvents(new DamageIndicatorEvent(), this);
        Bukkit.getPluginManager().registerEvents(new RemoveVanillaDamage(), this);
        Bukkit.getPluginManager().registerEvents(new Combat(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeath(), this);
        Bukkit.getPluginManager().registerEvents(new ProjectileLaunch(), this);
        Bukkit.getPluginManager().registerEvents(new TriggerReaction(), this);
        Bukkit.getPluginManager().registerEvents(new ShieldRefutation(), this);
        Bukkit.getPluginManager().registerEvents(new DendroCoreTrigger(), this);
        Bukkit.getPluginManager().registerEvents(new ChunkUnload(), this);
        Bukkit.getPluginManager().registerEvents(new AuraApplyLimit(), this);
        Bukkit.getPluginManager().registerEvents(new DPSCheck(), this);
        Bukkit.getPluginManager().registerEvents(new HealthBar(), this);
        Bukkit.getPluginManager().registerEvents(new ShieldHit(), this);
        Bukkit.getPluginManager().registerEvents(new ReactionStatistic(), this);
        Bukkit.getPluginManager().registerEvents(new DamagePerHitStatistic(), this);
        Bukkit.getPluginManager().registerEvents(new DendroCoreReactionStatistic(), this);
        registerMMOItemStat();

        ReactionManager.registerDefaultReactions();

        TriggerType.register(new TriggerType("STATS_CHANGE"));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mm reload");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mi reload all");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mmocore reload");

        placeholderExpansion = new PlaceholderHook();
        placeholderExpansion.register();

        MythicLib.inst().getStats().registerStat(new BaseMaxHealthStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "MAX_HEALTH"));
        MythicLib.inst().getStats().registerStat(new MaxHealthPercentStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "AST_MAX_HEALTH_BUFF_PERCENT"));
        MythicLib.inst().getStats().registerStat(new MaxHealthStatHandler(new ConfigFile("stats").getConfig(), Attribute.GENERIC_MAX_HEALTH, "AST_MAX_HEALTH_BUFF"));

    }

    @Override
    public void onDisable() {


        placeholderExpansion.unregister();

        // Remove holograms
        for (Hologram holo : Hologram.getCachedHolograms()) {
            if (holo.getId().startsWith("DamageIndicator_")) {
                holo.delete();
            }
        }

        // Remove entities from the team
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getMainScoreboard();
        Team damageIndicatorTeam = board.getTeam("DamageIndicatorBars");
        if (damageIndicatorTeam != null) {
            for (String entry : damageIndicatorTeam.getEntries()) {
                damageIndicatorTeam.removeEntry(entry);
            }
        }

        for (List<DendroCore> dendroCores : DendroCoreManager.dendroCoreInChunk.values()) {
            for (DendroCore dendroCore : dendroCores) {
                dendroCore.getDendroCore().remove();
            }
        }
    }

    public static MythicCore getInstance() {
        return instance;
    }
    public static Aura getAuraManager() { return aura; }
    public static Buff getBuffManager() { return buff; }
    public static InternalCooldown getCooldownManager() { return cooldown; }
    public static ReactionManager getReactionManager() { return reactionManager; }
    private void registerMMOItemStat() {
        MMOItems.plugin.getStats().register(new Abilities());
        MMOItems.plugin.getStats().register(ELEMENT_STAT);
        MMOItems.plugin.getStats().register(UNSOCKET_ALL);
        MMOItems.plugin.getStats().register(REMOVE_GEMSTONE);
        MMOItems.plugin.getStats().register(RANDOM_UNSOCKET);
        MMOItems.plugin.getStats().register(new GaugeUnitStat());
        MMOItems.plugin.getStats().register(new InternalCooldownStat());
        MMOItems.plugin.getStats().register(new DamageFormulaStat());
        MMOItems.plugin.getStats().register(new WeaponTypeStat());
        ConfigLoader.registerBooleanStats(getConfig().getConfigurationSection("Stats.BOOLEAN_STAT"));
        ConfigLoader.registerDoubleStats(getConfig().getConfigurationSection("Stats.DOUBLE_STAT"));
    }

}
