package com.basefinder.module.settings; // Или твой пакет
import com.basefinder.module.Setting;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String desc, boolean value) {
        super(name, desc, value);
    }
    // Метод getValue() уже есть в родителе, если он правильно написан
}
