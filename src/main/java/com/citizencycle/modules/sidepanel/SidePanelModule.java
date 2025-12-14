package com.citizencycle.modules.sidepanel;

import com.google.inject.Inject;
import com.citizencycle.CitizenCycleConfig;
import com.citizencycle.PluginModuleContract;
import com.citizencycle.events.CitizenDataRefreshFailed;
import com.citizencycle.events.CitizenDataRefreshed;
import com.citizencycle.events.WorldHopRequest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SidePanelModule extends PluginModuleContract
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WorldService worldService;

	@Inject
	private CitizenCycleConfig config;

	private CitizenSidePanel sidePanel;
	private NavigationButton navButton;
	private List<World> worldList = new ArrayList<>();

	@Override
	public void startUp()
	{
		sidePanel = new CitizenSidePanel(this::onWorldHopRequested);
		sidePanel.setCurrentWorld(client.getWorld());

		BufferedImage icon;
		try
		{
			icon = ImageUtil.loadImageResource(getClass(), "/citizen_icon.png");
		}
		catch (Exception e)
		{
			icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		}

		navButton = NavigationButton.builder()
			.tooltip("Citizen Cycle")
			.icon(icon)
			.panel(sidePanel)
			.build();

		if (config.showSidePanel())
		{
			clientToolbar.addNavigation(navButton);
		}

		fetchWorldData();
	}

	@Override
	public void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Override
	public void onSecondElapsed(int secondsSinceStartup)
	{
		// Rebuild panel every second to update timers
		SwingUtilities.invokeLater(() -> sidePanel.rebuild());
	}

	private void fetchWorldData()
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult != null)
		{
			worldList = worldResult.getWorlds();
			sidePanel.setWorldList(worldList);
		}
	}

	private void onWorldHopRequested(int worldId)
	{
		for (World world : worldList)
		{
			if (world.getId() == worldId)
			{
				dispatch(new WorldHopRequest(world));
				break;
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			SwingUtilities.invokeLater(() -> sidePanel.rebuild());
		}
	}

	@Subscribe
	public void onWorldChanged(WorldChanged event)
	{
		sidePanel.setCurrentWorld(client.getWorld());
		SwingUtilities.invokeLater(() -> sidePanel.rebuild());
	}

	@Subscribe
	public void onCitizenDataRefreshed(CitizenDataRefreshed event)
	{
		SwingUtilities.invokeLater(() -> {
			sidePanel.updateCitizenData(event.getCitizenStatuses());
			sidePanel.rebuild();
		});
	}

	@Subscribe
	public void onCitizenDataRefreshFailed(CitizenDataRefreshFailed event)
	{
		SwingUtilities.invokeLater(() -> sidePanel.setErrorMessage(event.getMessage()));
	}
}
