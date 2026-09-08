package com.basefinder.module.modules;

import com.basefinder.BaseFinderClient;
import com.basefinder.module.Module;
import com.basefinder.module.settings.BoolSetting;
import com.basefinder.module.settings.NumberSetting;
import com.basefinder.module.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseFinderModule extends Module {

    private final Set<Block> selectedBlocks = new HashSet<>();
    private final List<BlockPos> foundPositions = new ArrayList<>();

    private final NumberSetting range = new NumberSetting("Range", 64, 10, 128, 1);
    private final BoolSetting autoStart = new BoolSetting("Auto Start", false);

    public BaseFinderModule() {
        super("BaseFinder", "Scans for valuable base blocks", Category.MISC);
        addSettings(range, autoStart);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoStart.get() && BaseFinderClient.scanner != null) {
            BaseFinderClient.scanner.startScan();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (BaseFinderClient.scanner != null) {
            BaseFinderClient.scanner.stopScan();
        }
    }

    // Методы для работы с блоками
    public void addSelectedBlock(Block block) {
        selectedBlocks.add(block);
    }

    public void removeSelectedBlock(Block block) {
        selectedBlocks.remove(block);
    }

    public Set<Block> getSelectedBlocksSet() {
        return selectedBlocks;
    }

    // Метод, который возвращает List<BlockPos> для рендера (конвертируем Set<Block> в список позиций при сканировании)
    // Для ESP нам нужны позиции, которые нашел сканер. 
    // Предположим, что сканер заполняет foundPositions.
    public List<BlockPos> getFoundPositions() {
        return new ArrayList<>(foundPositions);
    }
    
    public void addFoundPosition(BlockPos pos) {
        if (!foundPositions.contains(pos)) {
            foundPositions.add(pos);
        }
    }

    public void clearFoundPositions() {
        foundPositions.clear();
    }

    public int getFoundCount() {
        return foundPositions.size();
    }

    public int getSelectedCount() {
        return selectedBlocks.size();
    }
    
    // Для совместимости с ClickGUI, если там ожидается getSettings
    public List<Setting<?>> getSettingsList() {
        return getSettings();
    }
}
