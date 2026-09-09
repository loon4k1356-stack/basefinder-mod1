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
    
    // Настройки (исправлены конструкторы: добавлено описание)
    private final ModeSetting mode = new ModeSetting("Mode", "Mode", "Normal", "Normal", "Legit");
    private final NumberSetting range = new NumberSetting("Range", "Range", 4.5, 3.0, 6.0, 0.1);
    private final NumberSetting angle = new NumberSetting("Angle", "Angle", 80.0, 0.0, 180.0, 1.0);
    private final NumberSetting cps = new NumberSetting("CPS", "CPS", 12.0, 1.0, 20.0, 1.0);
    private final BoolSetting antiCheat = new BoolSetting("Anti-Cheat (HW)", "Anti-Cheat (HW)", true);
    private final NumberSetting changeTime = new NumberSetting("Pattern Change (s)", "Pattern Change (s)", 20.0, 5.0, 60.0, 1.0);

    private long lastPatternChange = System.currentTimeMillis();
    private int currentAttackPattern = 0;
    private int attackTimer = 0;

    public KillAura() {
        super("KillAura", "Automatic combat module", Category.COMBAT);
        // Добавлен метод addSettings (если его нет в Module, раскомментируй строку ниже и добавь список в Module)
        // settings.add(mode); settings.add(range); ... (см. примечание внизу)
        setupSettings();
    }

    private void setupSettings() {
        // Если в твоем классе Module нет метода addSettings(varargs), добавляем по одному
        if (settings != null) {
            settings.add(mode);
            settings.add(range);
            settings.add(angle);
            settings.add(cps);
            settings.add(antiCheat);
            settings.add(changeTime);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (antiCheat.get()) {
            long now = System.currentTimeMillis();
            double changeInterval = changeTime.get() * 1000;
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
                if (mc.player.squaredDistanceTo(e) <= (range.get() * range.get())) {
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
        return angleToEntity <= angle.get();
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
        int delay = (int) (20 / cps.get());
        
        if (attackTimer >= delay) {
            if (mode.getMode().equals("Normal")) {
                faceTargetPacket(target);
            } else {
                faceTargetSmooth(target);
            }

            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            // Вызов частиц в TargetHUD
            if (com.basefinder.BaseFinderClient.targetHUD != null) {
                com.basefinder.BaseFinderClient.targetHUD.onAttack();
            }

            attackTimer = 0;
            if (antiCheat.get() && Math.random() > 0.8) {
                attackTimer += (int)(Math.random() * 3);
            }
        }
    }

    private void faceTargetPacket(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        // Исправлено: getEyeHeight() требует аргумент Pose в новых версиях, используем упрощенный расчет
        double eyeHeight = 1.62; 
        double diffY = target.getY() + target.getHeight() / 2 - (mc.player.getY() + eyeHeight);
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
