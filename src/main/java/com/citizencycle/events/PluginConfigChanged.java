package com.citizencycle.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PluginConfigChanged
{
	private final String key;
	private final String newValue;
}
