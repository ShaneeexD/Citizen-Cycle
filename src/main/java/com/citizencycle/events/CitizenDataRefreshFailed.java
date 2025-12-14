package com.citizencycle.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CitizenDataRefreshFailed
{
	private final String message;
}
