package com.citizencycle.events;

import com.citizencycle.objects.CitizenStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CitizenDataRefreshed
{
	private final List<CitizenStatus> citizenStatuses;
}
