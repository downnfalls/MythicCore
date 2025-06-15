package com.dev.mythiccore.events.attack_handle;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.buff.buffs.ElementalShield;
import com.dev.mythiccore.library.attackMetadata.AstAttackMeta;
import com.dev.mythiccore.listener.events.ShieldHitEvent;
import com.dev.mythiccore.visuals.ASTDamageIndicators;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.damage.DamagePacket;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ShieldRefutation implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void attack(AttackEvent event) {

        if (event.getAttack() instanceof AstAttackMeta astAttackMeta) {
            if (!astAttackMeta.isShieldRefuse()) {
                return;
            }
        }

        for (DamagePacket packet : event.getDamage().getPackets()) {
            if (packet.getElement() == null) continue;

            ElementalShield shield = MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).getActivateBuff(ElementalShield.class, new String[]{"element"}, new String[]{packet.getElement().getId()});
            double shieldRefuseReduction = 0;
            if (shield != null) {
                shieldRefuseReduction = MythicCore.getInstance().getConfig().getDouble("General.shield-refuse-reduction");
            } else {
                shield = MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).getActivateBuff(ElementalShield.class, new String[]{}, new String[]{});
            }

            if (shield == null) continue;

            double shield_attack;
            double damageReduce = shieldRefuseReduction/100 * packet.getValue();

            Bukkit.getPluginManager().callEvent(new ShieldHitEvent(event.getEntity(), shield, packet.getValue()-damageReduce));

            if (shield.getAmount() > packet.getValue()-damageReduce) {
                shield_attack = packet.getValue()-damageReduce;
                shield.setAmount(shield.getAmount() - (packet.getValue()-damageReduce));
                packet.setValue(0);
            } else if (shield.getAmount() < packet.getValue()-damageReduce) {
                shield_attack = shield.getAmount();
                MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).removeBuff(shield.getUniqueId());
                packet.setValue((packet.getValue()-damageReduce) - shield.getAmount());
            } else {
                shield_attack = shield.getAmount()-damageReduce;
                MythicCore.getBuffManager().getBuff(event.getEntity().getUniqueId()).removeBuff(shield.getUniqueId());
                packet.setValue(0);
            }

            Entity entity = event.getEntity();

            if (!(entity instanceof Player) || !UtilityMethods.isVanished((Player)entity)) {
                ConfigurationSection config = MythicCore.getInstance().getConfig().getConfigurationSection("Indicators");
                ASTDamageIndicators indicators = new ASTDamageIndicators(config);
                assert config != null;
                String format = config.getString("shield-attack-format");
                assert format != null;
                indicators.displayIndicator(entity, indicators.computeFormat(shield_attack, false, format, packet.getElement()), indicators.getDirection(event.toBukkit()), io.lumine.mythic.lib.api.event.IndicatorDisplayEvent.IndicatorType.DAMAGE);
            }
        }
    }
}
