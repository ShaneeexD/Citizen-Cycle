package com.citizencycle;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CitizenCyclePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CitizenCyclePlugin.class);
		RuneLite.main(args);
	}
}
