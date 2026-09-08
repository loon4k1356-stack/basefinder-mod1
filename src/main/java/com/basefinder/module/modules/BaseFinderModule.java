package com.basefinder.module.modules;

import com.basefinder.module.Module;
import com.basefinder.module.settings.BoolSetting;
import com.basefinder.module.settings.NumberSetting;
import com.basefinder.module.settings.Setting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BaseFinderModule extends Module {

    // Список выбранных блоков для сканирования и отображения (ESP)
    private final List<BlockPos> selectedBlocks = new ArrayList<>();

    // Настройки (примеры, можешь добавить свои)
    private final NumberSetting range = new NumberSetting("Range", 50, 10, 200, 1);
    private final BoolSetting autoStart = new BoolSetting("Auto Start", false);

    public BaseFinderModule() {
        super("BaseFinder", "Finds valuable bases and structures", Category.MISC);
        addSettings(range, autoStart);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Логика при включении модуля
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Логика при выключении модуля
    }

    /**
     * Возвращает список выбранных блоков.
     * Используется для отрисовки 3D ESP и логики сканера.
     */
    public List<BlockPos> getSelectedBlocks() {
        return selectedBlocks;
    }

    /**
     * Добавляет блок в список выбранных.
     */
    public void addSelectedBlock(BlockPos pos) {
        if (!selectedBlocks.contains(pos)) {
            selectedBlocks.add(pos);
        }
    }

    /**
     * Удаляет блок из списка выбранных.
     */
    public void removeSelectedBlock(BlockPos pos) {
        selectedBlocks.remove(pos);
    }

    /**
     * Очищает весь список.
     */
    public void clearSelectedBlocks() {
        selectedBlocks.clear();
    }

    // Методы для обратной совместимости, если где-то используется getFoundCount
    public int getFoundCount() {
        return selectedBlocks.size();
    }

    public int getSelectedCount() {
        return selectedBlocks.size();
    }
}
