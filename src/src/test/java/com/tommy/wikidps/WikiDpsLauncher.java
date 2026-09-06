package com.tommy.wikidps;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WikiDpsLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(com.dpscalc.DpsCalcPlugin.class);
        RuneLite.main(args);
    }
}
