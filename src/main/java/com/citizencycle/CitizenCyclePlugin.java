package com.citizencycle;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.citizencycle.modules.broadcast.BroadcastModule;
import com.citizencycle.modules.citizen.CitizenObserverModule;
import com.citizencycle.modules.sidepanel.SidePanelModule;
import com.citizencycle.modules.worldhop.WorldHopModule;
import java.lang.reflect.InvocationTargetException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import lombok.Getter;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;

@PluginDescriptor(
	name = "Citizen Cycle",
	description = "Crowdsources Wealthy Citizen distraction data across worlds for efficient thieving",
	tags = {"thieving", "wealthy", "citizen", "pickpocket", "crowdsource", "worldhop"}
)
public class CitizenCyclePlugin extends Plugin
{
	@Getter
	@Inject
	private CitizenCycleConfig config;

	@Provides
	protected CitizenCycleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CitizenCycleConfig.class);
	}

	private final ArrayList<Class<? extends PluginModuleContract>> modules = new ArrayList<>(Arrays.asList(
		BroadcastModule.class,
		CitizenObserverModule.class,
		SidePanelModule.class,
		WorldHopModule.class
	));

	@Inject
	private EventBus eventBus;

	private final HashMap<Class<? extends PluginModuleContract>, PluginModuleContract> registeredModules = new HashMap<>();

	private int secondsElapsed = 1;

	protected <T extends PluginModuleContract> void registerModule(Class<T> className)
	{
		if (this.registeredModules.containsKey(className))
		{
			return;
		}

		T module;

		try
		{
			module = className.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
		{
			e.printStackTrace();
			return;
		}

		injector.injectMembers(module);
		module.setInjector(injector);

		this.registeredModules.put(className, module);
	}

	@Override
	protected void startUp()
	{
		for (Class<? extends PluginModuleContract> module : modules)
		{
			this.registerModule(module);
		}

		for (PluginModuleContract module : this.registeredModules.values())
		{
			eventBus.register(module);
			module.startUp();
		}
	}

	@Override
	protected void shutDown()
	{
		for (PluginModuleContract module : this.registeredModules.values())
		{
			eventBus.unregister(module);
			module.shutDown();
		}

		secondsElapsed = 1;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		Class<?> inter = config.getClass().getInterfaces()[0];
		ConfigGroup group = inter.getAnnotation(ConfigGroup.class);

		if (group != null && event.getGroup().equals(group.value()))
		{
			eventBus.post(new com.citizencycle.events.PluginConfigChanged(event.getKey(), event.getNewValue()));
		}
	}

	@Schedule(
		period = 1,
		unit = ChronoUnit.SECONDS
	)
	public void everySecondTick()
	{
		for (PluginModuleContract module : this.registeredModules.values())
		{
			module.onSecondElapsed(secondsElapsed);
		}

		secondsElapsed++;
	}
}
