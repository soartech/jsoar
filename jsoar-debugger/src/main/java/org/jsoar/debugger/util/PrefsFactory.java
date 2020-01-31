package org.jsoar.debugger.util;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class PrefsFactory implements PreferencesFactory {
    Preferences rootPreferences;


    @Override
    public Preferences systemRoot() {
        return userRoot();
    }

    @Override
    public Preferences userRoot() {
        if (rootPreferences == null) {
            rootPreferences = new Prefs(null, "");
        }
        return rootPreferences;
    }


}
