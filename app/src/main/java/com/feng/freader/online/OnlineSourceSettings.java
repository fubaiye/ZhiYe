package com.feng.freader.online;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public class OnlineSourceSettings {
    private static final String NAME = "online_source_settings";
    private final SharedPreferences preferences;

    public OnlineSourceSettings(Context context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void applyTo(List<OnlineBookSource> sources) {
        for (OnlineBookSource source : sources) {
            source.setEnabled(preferences.getBoolean(source.getId() + "_enabled", source.isEnabled()));
        }
    }

    public void save(OnlineBookSource source) {
        preferences.edit().putBoolean(source.getId() + "_enabled", source.isEnabled()).apply();
    }
}
