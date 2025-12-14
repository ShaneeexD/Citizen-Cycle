package com.citizencycle.events;

import com.citizencycle.objects.CitizenStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CitizenStatusChanged
{
	private final CitizenStatus status;
	private final boolean distracted;
}
