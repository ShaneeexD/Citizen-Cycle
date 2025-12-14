package com.citizencycle.modules.broadcast;

import com.google.inject.Inject;
import com.citizencycle.CitizenCycleConfig;
import com.citizencycle.PluginModuleContract;
import com.citizencycle.events.CitizenDataRefreshFailed;
import com.citizencycle.events.CitizenDataRefreshed;
import com.citizencycle.objects.CitizenStatus;
import com.citizencycle.services.HttpService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BroadcastModule extends PluginModuleContract
{
	@Inject
	private CitizenCycleConfig config;

	@Inject
	private HttpService httpService;

	@Getter
	private final ConcurrentHashMap<Integer, CitizenStatus> worldStatusMap = new ConcurrentHashMap<>();

	private int lastRefreshSecond = 0;

	@Override
	public void startUp()
	{
		worldStatusMap.clear();
		lastRefreshSecond = 0;
	}

	@Override
	public void shutDown()
	{
		worldStatusMap.clear();
	}

	@Override
	public void onSecondElapsed(int secondsSinceStartup)
	{
		int refreshInterval = config.refreshInterval();
		if (refreshInterval <= 0)
		{
			refreshInterval = 5;
		}

		if (secondsSinceStartup - lastRefreshSecond >= refreshInterval)
		{
			lastRefreshSecond = secondsSinceStartup;
			refreshCitizenData();
		}
	}

	private void refreshCitizenData()
	{
		String endpoint = config.getEndpoint();
		if (endpoint == null || endpoint.isEmpty())
		{
			return;
		}

		httpService.get(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to refresh citizen data: {}", e.getMessage());
				dispatch(new CitizenDataRefreshFailed(e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try
				{
					if (!response.isSuccessful())
					{
						dispatch(new CitizenDataRefreshFailed("HTTP " + response.code()));
						return;
					}

					String body = response.body().string();
					List<CitizenStatus> statuses = parseResponse(body);

					worldStatusMap.clear();
					for (CitizenStatus status : statuses)
					{
						worldStatusMap.put(status.getWorld(), status);
					}

					dispatch(new CitizenDataRefreshed(statuses));
				}
				finally
				{
					response.close();
				}
			}
		});
	}

	private List<CitizenStatus> parseResponse(String json)
	{
		List<CitizenStatus> statuses = new ArrayList<>();

		try
		{
			JsonArray array = GSON.fromJson(json, JsonArray.class);
			for (JsonElement element : array)
			{
				JsonObject obj = element.getAsJsonObject();
				CitizenStatus status = new CitizenStatus(
					obj.get("world").getAsInt(),
					obj.get("distracted").getAsBoolean(),
					obj.has("distractionStartTime") ? obj.get("distractionStartTime").getAsLong() : 0,
					obj.has("distractionEndTime") ? obj.get("distractionEndTime").getAsLong() : 0,
					obj.has("lastUpdateTime") ? obj.get("lastUpdateTime").getAsLong() : System.currentTimeMillis(),
					obj.has("reportedBy") ? obj.get("reportedBy").getAsString() : ""
				);
				statuses.add(status);
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to parse citizen data response: {}", e.getMessage());
		}

		return statuses;
	}

	public CitizenStatus getStatusForWorld(int world)
	{
		return worldStatusMap.get(world);
	}

	public List<CitizenStatus> getDistractedWorlds()
	{
		List<CitizenStatus> distracted = new ArrayList<>();
		for (CitizenStatus status : worldStatusMap.values())
		{
			if (status.isDistracted() && !status.isStale())
			{
				distracted.add(status);
			}
		}
		return distracted;
	}
}
