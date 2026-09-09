package com.basefinder.module.modules;

import com.basefinder.module.Module;
import com.basefinder.module.settings.BoolSetting;
import com.basefinder.module.settings.ModeSetting;
import com.basefinder.module.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class KillAura extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Исправлено: добавлено описание, порядок аргументов верный
    private final ModeSetting mode = new ModeSetting("Mode", "Режим атаки", "Normal", "Normal", "Legit");
    private final NumberSetting range = new NumberSetting("Range", "Дистанция", 4.5, 3.0, 6.0, 0.1);
    private final NumberSetting angle = new NumberSetting("Angle", "Угол", 80.0, 0.0, 180.0, 1.0);
    private final NumberSetting cps = new NumberSetting("CPS", "Ударов в секунду", 12.0, 1.0, 20.0, 1.0);
    private final BoolSetting antiCheat = new BoolSetting("Anti-Cheat", "Обход HolyWorld", true);
    private final NumberSetting changeTime = new NumberSetting("ChangeTime", "Смена паттерна (сек)", 20.0, 5.0, 60.0, 1.0);

    private long lastPatternChange = System.currentTimeMillis();
    private int currentAttackPattern = 0;
    private int attackTimer = 0;

    public KillAura() {
        super("KillAura", "Автоматическая атака игроков", Category.COMBAT);
        // Добавляем настройки через метод базового класса
        addSetting(mode);
        addSetting(range);
        addSetting(angle);
        addSetting(cps);
        addSetting(antiCheat);
        addSetting(changeTime);
    }

    @Override
    public void onTick() {
        if (!isToggled()) return; // Теперь метод существует
        if (mc.player == null || mc.world == null) return;

        // Логика античита
        if (antiCheat.value) { // Доступ к полю value напрямую
            long now = System.currentTimeMillis();
            double changeInterval = changeTime.value * 1000;
            if (now - lastPatternChange > changeInterval) {
                currentAttackPattern = (int)(Math.random() * 3);
                lastPatternChange = now;
                if (mc.options.forwardKey.isPressed()) {
                    mc.player.setYaw(mc.player.getYaw() + (float)(Math.random() * 4 - 2));
                }
            }
        }

        Entity target = findTarget();
        if (target != null) {
            attackTarget((LivingEntity) target);
        }
    }

    private Entity findTarget() {
        List<Entity> entities = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity && e != mc.player && !e.isRemoved()) {
                if (mc.player.squaredDistanceTo(e) <= (range.value * range.value)) {
                    if (isLookingAt(e)) {
                        entities.add(e);
                    }
                }
            }
        }
        return entities.isEmpty() ? null : entities.get(0);
    }

    private boolean isLookingAt(Entity e) {
        Vec3d diff = e.getPos().subtract(mc.player.getPos());
        float angleToEntity = getAngleToVec(diff);
        return angleToEntity <= angle.value;
    }

    private float getAngleToVec(Vec3d vec) {
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        double diffX = vec.x;
        double diffY = vec.y;
        double diffZ = vec.z;
        double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yawDist = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitchDist = (float) (Math.toDegrees(-Math.atan2(diffY, horizontalDist)));
        float yawDelta = MathHelper.wrapDegrees(yaw - yawDist);
        float pitchDelta = MathHelper.wrapDegrees(pitch - pitchDist);
        return MathHelper.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    private void attackTarget(LivingEntity target) {
        attackTimer++;
        int delay = (int) (20 / cps.value);
        
        if (attackTimer >= delay) {
            if (mode.mode.equals("Normal")) {
                faceTargetPacket(target);
            } else {
                faceTargetSmooth(target);
            }
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            attackTimer = 0;
            if (antiCheat.value && Math.random() > 0.8) {
                attackTimer += (int)(Math.random() * 3);
            }
        }
    }

    private void faceTargetPacket(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffY = target.getY() + target.getHeight() / 2 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = target.getZ() - mc.player.getZ();
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void faceTargetSmooth(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        float targetYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float diff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        mc.player.setYaw(mc.player.getYaw() + diff * 0.5f);
    }
}
