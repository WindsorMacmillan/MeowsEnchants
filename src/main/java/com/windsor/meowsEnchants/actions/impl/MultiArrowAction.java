package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;

/**
 * 在射出的主箭之外额外连射多支箭矢。
 * <p>
 * 配合 BOW_SHOOT 触发器使用，弓与弩均可生效：额外箭矢会继承原箭矢的方向、初速度、暴击、
 * 穿透、着火与药水效果，并继承发射所用的武器，从而保留力量、火矢等附魔带来的加成。
 */
public class MultiArrowAction implements Action {

    private static final double DEFAULT_SPEED = 3.0;
    private static final double MAX_SPREAD_DEGREES = 60.0;

    private final ScalingFunction extraArrows;
    private final ScalingFunction spread;
    private final Random random = new Random();

    public MultiArrowAction(Map<String, Object> params) {
        Object extraObj = params.get("extra_arrows");
        if (extraObj == null) {
            throw new IllegalArgumentException("Missing 'extra_arrows' parameter for multi_arrow action");
        }
        this.extraArrows = ScalingFunctionLoader.load(extraObj);

        Object spreadObj = params.get("spread");
        if (spreadObj == null) {
            throw new IllegalArgumentException("Missing 'spread' parameter for multi_arrow action");
        }
        this.spread = ScalingFunctionLoader.load(spreadObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        int extraCount = (int) Math.max(0, this.extraArrows.getValue(context.getEnchantLevel()));
        if (extraCount <= 0) return false;

        double spreadDegrees = Math.clamp(this.spread.getValue(context.getEnchantLevel()), 0, MAX_SPREAD_DEGREES);

        Plugin plugin = context.getPlugin();
        if (plugin == null) return false;

        // 取原始箭矢（弓/弩射击事件），据此确定基准方向与初速度
        Projectile sourceProjectile = null;
        if (context.getOriginalEvent() instanceof EntityShootBowEvent bowEvent
                && bowEvent.getProjectile() instanceof Projectile shotProjectile) {
            sourceProjectile = shotProjectile;
        }

        Vector baseDirection = player.getEyeLocation().getDirection().clone();
        double speed = DEFAULT_SPEED;
        if (sourceProjectile != null) {
            Vector sourceVelocity = sourceProjectile.getVelocity();
            if (sourceVelocity.lengthSquared() > 1.0E-6) {
                baseDirection = sourceVelocity.clone();
                speed = sourceVelocity.length();
            }
        }
        if (baseDirection.lengthSquared() < 1.0E-6) {
            baseDirection.setX(0).setY(1).setZ(0);
        }
        baseDirection.normalize();

        final Projectile source = sourceProjectile;
        final Vector shotDirection = baseDirection;
        final double shotSpeed = speed;

        // 每 tick 额外射出一支箭矢，形成连射
        for (int i = 0; i < extraCount; i++) {
            SchedulerCompat.runDelayed(player, plugin, _ -> {
                if (!player.isOnline() || player.isDead()) return;

                Vector direction = generateRandomDirection(shotDirection, spreadDegrees);
                Arrow arrow = player.launchProjectile(Arrow.class, direction.multiply(shotSpeed));
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                copyArrowState(source, arrow);
            }, i + 1L);
        }
        return true;
    }

    /**
     * 生成一个与给定方向夹角在 spreadDegrees 度以内的随机单位向量。
     */
    private Vector generateRandomDirection(Vector baseDir, double spreadDegrees) {
        if (spreadDegrees <= 0) {
            return baseDir.clone().normalize();
        }

        double spreadRad = Math.toRadians(spreadDegrees);
        double angle = random.nextDouble() * spreadRad;
        double azimuth = random.nextDouble() * 2 * Math.PI;

        Vector axis = baseDir.clone().crossProduct(new Vector(0, 1, 0));
        if (axis.lengthSquared() < 1.0E-10) {
            axis = baseDir.clone().crossProduct(new Vector(1, 0, 0));
        }
        axis.normalize();

        Vector rotated = baseDir.clone().rotateAroundAxis(axis, angle);
        rotated.rotateAroundAxis(baseDir, azimuth);
        return rotated.normalize();
    }

    /**
     * 让额外箭矢继承原始箭矢的状态，使其表现与玩家射出的箭矢一致（弓与弩均适用）。
     */
    private void copyArrowState(Projectile source, Arrow target) {
        if (!(source instanceof Arrow sourceArrow)) return;

        target.setCritical(sourceArrow.isCritical());
        target.setDamage(sourceArrow.getDamage());
        target.setPierceLevel(sourceArrow.getPierceLevel());
        target.setFireTicks(sourceArrow.getFireTicks());

        // 继承发射武器，保留力量、火矢等依附于武器的附魔效果
        ItemStack weapon = sourceArrow.getWeapon();
        if (weapon != null && !weapon.getType().isAir()) {
            target.setWeapon(weapon.clone());
        }

        target.setBasePotionType(sourceArrow.getBasePotionType());
        target.setColor(sourceArrow.getColor());
        if (sourceArrow.hasCustomEffects()) {
            for (PotionEffect effect : sourceArrow.getCustomEffects()) {
                target.addCustomEffect(effect, true);
            }
        }
    }
}
