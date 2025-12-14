package com.citizencycle.objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BroadcastPayload
{
	private String playerName;
	private int world;
	private boolean distracted;
	private long timestamp;
}
