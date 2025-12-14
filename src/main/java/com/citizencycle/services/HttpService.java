package com.citizencycle.services;

import com.google.inject.Inject;
import com.citizencycle.CitizenCycleConfig;
import com.citizencycle.objects.BroadcastPayload;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpService
{
	@Inject
	private CitizenCycleConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	public void post(BroadcastPayload payload, Callback callback) throws IllegalArgumentException
	{
		String endpoint = config.getEndpoint();
		if (endpoint == null || endpoint.isEmpty())
		{
			return;
		}

		Request request = new Request.Builder()
			.url(endpoint.replaceAll("\\s", ""))
			.addHeader("authorization", config.getAuthorization().replaceAll("\\s", ""))
			.post(RequestBody.create(MediaType.parse("application/json"), GSON.toJson(payload)))
			.build();

		okHttpClient.newCall(request).enqueue(callback);
	}

	public void get(Callback callback)
	{
		String endpoint = config.getEndpoint();
		if (endpoint == null || endpoint.isEmpty())
		{
			return;
		}

		Request request = new Request.Builder()
			.url(endpoint)
			.addHeader("authorization", config.getAuthorization())
			.get()
			.build();

		okHttpClient.newCall(request).enqueue(callback);
	}
}
