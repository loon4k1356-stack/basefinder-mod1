package com.basefinder.module.modules;

import com.basefinder.BaseFinderClient;
import com.basefinder.module.Module;
import com.basefinder.module.settings.BoolSetting;
import com.basefinder.module.settings.ModeSetting;
import com.basefinder.module.settings.NumberSetting;
import com.basefinder.module.settings.Setting;
import com.basefinder.scanner.BlockScanner;
import java.util.ArrayList;
import java.util.List;

public class BaseFinderModule extends Module {
    public final NumberSetting scanRadius = new NumberSetting("Scan Radius", "Radius to scan", 300, 50, 1000, 10);
    public final ModeSetting scanMode = new ModeSetting("Scan Mode", "Mode", 0, "LITE", "FULL");
    public final NumberSetting liteHeight = new NumberSetting("Lite Height", "Max Y for LITE", 30, -64, 64, 1);
    public final BoolSetting showTracers = new BoolSetting("Tracers", "Draw lines", true);
    public final BoolSetting showBoxes = new BoolSetting("Boxes", "Draw boxes", true);
    public final NumberSetting boxAlpha = new NumberSetting("Box Alpha", "Transparency", 0.4, 0.1, 1.0, 0.1);
    
    private List<Setting<?>> settings;
    
    public BaseFinderModule() {
        super("BaseFinder", "Scans for selected blocks", Category.WORLD);
        settings = new ArrayList<>();
        settings.add(scanRadius);
        settings.add(scanMode);
        settings.add(liteHeight);
        settings.add(showTracers);
        settings.add(showBoxes);
        settings.add(boxAlpha);
    }
    
    @Override
    public void onEnable() {
        BlockScanner scanner = BaseFinderClient.scanner;
        if (scanner != null) {
            scanner.setScanRadius(scanRadius.getInt());
            scanner.setLiteMode(scanMode.is("LITE"));
            scanner.setLiteHeightLimit(liteHeight.getInt());
            scanner.start();
        }
    }
    
    @Override
    public void onDisable() {
        BlockScanner scanner = BaseFinderClient.scanner;
        if (scanner != null) scanner.stop();
    }
    
    @Override
    public void onTick() {
        BlockScanner scanner = BaseFinderClient.scanner;
        if (scanner != null) {
            scanner.setScanRadius(scanRadius.getInt());
            scanner.setLiteMode(scanMode.is("LITE"));
            scanner.setLiteHeightLimit(liteHeight.getInt());
        }
    }
    
    public List<Setting<?>> getSettings() { return settings; }
    public int getFoundCount() { return BaseFinderClient.scanner != null ? BaseFinderClient.scanner.getFoundBlocks().size() : 0; }
    public int getSelectedCount() { return BaseFinderClient.scanner != null ? BaseFinderClient.scanner.getSelectedBlocks().size() : 0; }
}
