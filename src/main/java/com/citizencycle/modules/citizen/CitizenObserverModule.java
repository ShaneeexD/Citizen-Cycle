package com.citizencycle.modules.citizen;

import com.google.inject.Inject;
import com.citizencycle.CitizenCycleConfig;
import com.citizencycle.PluginModuleContract;
import com.citizencycle.events.CitizenStatusChanged;
import com.citizencycle.objects.BroadcastPayload;
import com.citizencycle.objects.CitizenStatus;
import com.citizencycle.services.HttpService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class CitizenObserverModule extends PluginModuleContract
{
	private static final Set<Integer> WEALTHY_CITIZEN_NPC_IDS = Set.of(13302, 13303, 13304, 13305);
	private static final String WEALTHY_CITIZEN_NAME = "Wealthy citizen";
	private static final int THIEVING_RANGE = 15;

	@Inject
	private Client client;

	@Inject
	private CitizenCycleConfig config;

	@Inject
	private HttpService httpService;

	@Getter
	private boolean inThievingArea = false;

	@Getter
	private boolean citizenDistracted = false;

	@Getter
	private NPC distractedCitizen = null;

	@Getter
	private long distractionStartTime = 0;

	private boolean wasDistracted = false;
	private boolean hasBroadcastedDistraction = false;
	private boolean hasBroadcastedEnd = false;

	@Override
	public void startUp()
	{
		resetState();
	}

	@Override
	public void shutDown()
	{
		resetState();
	}

	private void resetState()
	{
		inThievingArea = false;
		citizenDistracted = false;
		distractedCitizen = null;
		distractionStartTime = 0;
		wasDistracted = false;
		hasBroadcastedDistraction = false;
		hasBroadcastedEnd = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			resetState();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		updateThievingAreaStatus(player);

		if (!inThievingArea)
		{
			return;
		}

		updateDistractionStatus(player);
		handleDistractionStateChange();
	}

	private void updateThievingAreaStatus(Player player)
	{
		WorldView worldView = player.getWorldView();
		if (worldView == null)
		{
			inThievingArea = false;
			return;
		}

		for (NPC npc : worldView.npcs())
		{
			if (npc == null) continue;

			if (WEALTHY_CITIZEN_NPC_IDS.contains(npc.getId()))
			{
				int distance = player.getWorldLocation().distanceTo(npc.getWorldLocation());
				if (distance <= THIEVING_RANGE)
				{
					inThievingArea = true;
					return;
				}
			}
		}

		inThievingArea = false;
	}

	private void updateDistractionStatus(Player player)
	{
		WorldView worldView = player.getWorldView();
		if (worldView == null)
		{
			citizenDistracted = false;
			distractedCitizen = null;
			return;
		}

		boolean foundDistracted = false;
		NPC foundDistractedNpc = null;

		for (NPC npc : worldView.npcs())
		{
			if (npc == null) continue;

			String npcName = npc.getName();
			if (npcName != null && npcName.equals(WEALTHY_CITIZEN_NAME))
			{
				if (npc.isInteracting())
				{
					Actor interactingWith = npc.getInteracting();
					if (interactingWith != null && interactingWith.getCombatLevel() == 0)
					{
						foundDistracted = true;
						foundDistractedNpc = npc;
						break;
					}
				}
			}
		}

		citizenDistracted = foundDistracted;
		distractedCitizen = foundDistractedNpc;

		if (citizenDistracted && !wasDistracted)
		{
			distractionStartTime = System.currentTimeMillis();
		}
		else if (!citizenDistracted)
		{
			distractionStartTime = 0;
		}
	}

	private void handleDistractionStateChange()
	{
		if (citizenDistracted && !wasDistracted)
		{
			onDistractionStarted();
		}
		else if (!citizenDistracted && wasDistracted)
		{
			onDistractionEnded();
		}

		wasDistracted = citizenDistracted;
	}

	private void onDistractionStarted()
	{
		log.debug("Wealthy citizen distraction started");
		hasBroadcastedEnd = false;

		CitizenStatus status = new CitizenStatus(
			client.getWorld(),
			true,
			System.currentTimeMillis(),
			0, // distractionEndTime - not ended yet
			System.currentTimeMillis(),
			client.getLocalPlayer().getName()
		);

		dispatch(new CitizenStatusChanged(status, true));

		if (config.autoBroadcast() && !hasBroadcastedDistraction)
		{
			broadcastStatus(true);
			hasBroadcastedDistraction = true;
		}

		if (config.notifyOnDistraction())
		{
			client.playSoundEffect(SoundEffectID.UI_BOOP);
		}
	}

	private void onDistractionEnded()
	{
		log.debug("Wealthy citizen distraction ended");
		hasBroadcastedDistraction = false;

		CitizenStatus status = new CitizenStatus(
			client.getWorld(),
			false,
			0, // distractionStartTime - reset
			System.currentTimeMillis(), // distractionEndTime - just ended
			System.currentTimeMillis(),
			client.getLocalPlayer().getName()
		);

		dispatch(new CitizenStatusChanged(status, false));

		if (config.autoBroadcast() && !hasBroadcastedEnd)
		{
			broadcastStatus(false);
			hasBroadcastedEnd = true;
		}

		if (config.notifyOnDistractionEnd())
		{
			client.playSoundEffect(SoundEffectID.UI_BOOP);
		}
	}

	private void broadcastStatus(boolean distracted)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		BroadcastPayload payload = new BroadcastPayload(
			player.getName(),
			client.getWorld(),
			distracted,
			System.currentTimeMillis()
		);

		httpService.post(payload, new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to broadcast citizen status: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
				log.debug("Successfully broadcast citizen status");
			}
		});
	}

	public long getDistractionDurationSeconds()
	{
		if (!citizenDistracted || distractionStartTime == 0)
		{
			return 0;
		}
		return (System.currentTimeMillis() - distractionStartTime) / 1000;
	}
}
