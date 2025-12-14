package com.citizencycle.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.http.api.worlds.World;

@Getter
@AllArgsConstructor
public class WorldHopRequest
{
	private final World world;
}
