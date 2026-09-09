package com.tommy.wikidps;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WikiDpsLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(com.loadoutlab.DpsLoadoutLabPlugin.class);
        RuneLite.main(args);
    }
}
