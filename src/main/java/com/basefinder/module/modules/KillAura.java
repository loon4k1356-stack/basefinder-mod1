package com.basefinder.module.modules;

import com.basefinder.module.Module;
import com.basefinder.module.settings.ModeSetting;
import com.basefinder.module.settings.NumberSetting;
import com.basefinder.module.settings.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KillAura extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    // Настройки
    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Legit");
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 6.0, 0.1);
    private final NumberSetting angle = new NumberSetting("Max Angle", 80, 0, 180, 1);
    private final NumberSetting delay = new NumberSetting("Attack Delay", 150, 0, 1000, 10);
    
    // Anti-Cheat Settings (HolyWorld Lite)
    private final BoolSetting antiCheat = new BoolSetting("Anti-Cheat Bypass", true);
    private final NumberSetting patternChangeTime = new NumberSetting("Pattern Change (sec)", 20, 15, 30, 1);

    private long lastAttackTime = 0;
    private long currentPatternStartTime = 0;
    private int currentAttackPattern = 0; // 0 = normal, 1 = slow, 2 = random delay

    public KillAura() {
        super("KillAura", "Automatically attacks entities", Category.COMBAT);
        addSettings(mode, range, angle, delay, antiCheat, patternChangeTime);
    }

    @Override
    public void onEnable() {
        currentPatternStartTime = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Логика смены паттерна для обхода античита
        if (antiCheat.get()) {
            long now = System.currentTimeMillis();
            long changeThreshold = (long) patternChangeTime.get() * 1000;
            
            if (now - currentPatternStartTime > changeThreshold) {
                currentAttackPattern = random.nextInt(3); // Меняем паттерн каждые 15-30 сек
                currentPatternStartTime = now;
                // Можно добавить сообщение в чат для отладки: "Pattern changed to " + currentAttackPattern
            }
        }

        Entity target = findTarget();
        if (target != null) {
            attackEntity((LivingEntity) target);
        }
    }

    private Entity findTarget() {
        List<Entity> targets = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player) continue;
            if (((LivingEntity) e).getHealth() <= 0) continue;
            if (!(e instanceof PlayerEntity)) continue; // Бить только игроков (можно убрать для мобов)

            double dist = mc.player.distanceTo(e);
            if (dist > range.get()) continue;

            // Проверка угла (FOV)
            if (!canSeeFeet(e)) continue;

            targets.add(e);
        }

        if (targets.isEmpty()) return null;
        // Возвращаем ближайшего
        targets.sort((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)));
        return targets.get(0);
    }

    private boolean canSeeFeet(Entity entity) {
        Vec3d eyes = mc.player.getCameraPosVec(1.0f);
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        Vec3d direction = targetPos.subtract(eyes).normalize();
        Vec3d playerRotation = getRotationVector(mc.player.getYaw(), mc.player.getPitch());
        
        double dot = direction.dotProduct(playerRotation);
        double angleCos = Math.cos(Math.toRadians(angle.get()));
        
        return dot > angleCos;
    }

    private Vec3d getRotationVector(float yaw, float pitch) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float g = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float h = -MathHelper.cos(-pitch * 0.017453292F);
        float i = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(g * h, i, f * h);
    }

    private void attackEntity(LivingEntity target) {
        long now = System.currentTimeMillis();
        long currentDelay = delay.get();

        // Применение разных паттернов задержки для обхода античита
        if (antiCheat.get()) {
            if (currentAttackPattern == 1) currentDelay += 50; // Чуть медленнее
            if (currentAttackPattern == 2) currentDelay = (long) (delay.get() * (0.8 + random.nextFloat() * 0.4)); // Рандом
        }

        if (now - lastAttackTime < currentDelay) return;

        // Поворот к врагу (только если не Legit или если угол слишком большой)
        if (!mode.getMode().equals("Legit")) {
            lookAtEntity(target);
        }

        // Атака
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        lastAttackTime = now;
    }

    private void lookAtEntity(Entity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dy = entity.getY() + entity.getStandingEyeHeight() - (mc.player.getY() + mc.player.getStandingEyeHeight());
        double dz = entity.getZ() - mc.player.getZ();
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
