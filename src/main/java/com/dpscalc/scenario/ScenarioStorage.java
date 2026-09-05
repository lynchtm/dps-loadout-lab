package com.dpscalc.scenario;

import net.runelite.client.config.ConfigManager;

/** Persistence boundary makes profile switching testable without a running client. */
public interface ScenarioStorage {
    long profileId();
    String getRSProfileKey();
    String getConfiguration(String group,String rsProfile,String key);
    void setConfiguration(String group,String rsProfile,String key,String value);

    static ScenarioStorage runeLite(ConfigManager manager) {
        return new ScenarioStorage() {
            public long profileId(){return manager.getProfile().getId();}
            public String getRSProfileKey(){return manager.getRSProfileKey();}
            public String getConfiguration(String group,String profile,String key){return manager.getConfiguration(group,profile,key);}
            public void setConfiguration(String group,String profile,String key,String value){manager.setConfiguration(group,profile,key,value);}
        };
    }
}
