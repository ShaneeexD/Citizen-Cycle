package com.citizencycle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("citizencycle")
public interface CitizenCycleConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "General settings",
		position = 0
	)
	String generalSection = "general";

	@ConfigSection(
		name = "Crowdsourcing",
		description = "Crowdsourcing settings",
		position = 1
	)
	String crowdsourcingSection = "crowdsourcing";

	@ConfigSection(
		name = "Notifications",
		description = "Notification settings",
		position = 2
	)
	String notificationSection = "notifications";

	// General Settings
	@ConfigItem(
		keyName = "showSidePanel",
		name = "Show Side Panel",
		description = "Show the side panel with citizen status across worlds",
		section = generalSection,
		position = 0
	)
	default boolean showSidePanel()
	{
		return true;
	}

	// Crowdsourcing Settings
	@ConfigItem(
		keyName = "endpoint",
		position = 0,
		name = "Endpoint",
		description = "Endpoint to post and fetch citizen distraction data"
	)
	default String getEndpoint()
	{
		return "https://citizen-cycle.up.railway.app/api/citizens";
	}

	@ConfigItem(
		keyName = "authorization",
		position = 1,
		name = "Authorization",
		description = "Used to set the http authorization header."
	)
	default String getAuthorization()
	{
		return "";
	}

	@ConfigItem(
		keyName = "includeIgn",
		name = "Send in-game name",
		description = "Includes your in-game name with your broadcasts.",
		position = 2,
		section = crowdsourcingSection
	)
	default boolean includeIgn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoBroadcast",
		name = "Auto Broadcast",
		description = "Automatically broadcast citizen distraction status when detected.",
		section = crowdsourcingSection,
		position = 3
	)
	default boolean autoBroadcast()
	{
		return true;
	}

	@ConfigItem(
		keyName = "refreshInterval",
		name = "Refresh Interval (seconds)",
		description = "How often to refresh citizen data from the server.",
		section = crowdsourcingSection,
		position = 4
	)
	default int refreshInterval()
	{
		return 5;
	}

	// Notification Settings
	@ConfigItem(
		keyName = "notifyOnDistraction",
		name = "Notify on Distraction",
		description = "Play a sound when a citizen becomes distracted",
		section = notificationSection,
		position = 0
	)
	default boolean notifyOnDistraction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyOnDistractionEnd",
		name = "Notify on Distraction End",
		description = "Play a sound when distraction ends",
		section = notificationSection,
		position = 1
	)
	default boolean notifyOnDistractionEnd()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyWorldAvailable",
		name = "Notify World Available",
		description = "Notify when another world has a distracted citizen",
		section = notificationSection,
		position = 2
	)
	default boolean notifyWorldAvailable()
	{
		return false;
	}
}
