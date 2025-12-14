package com.citizencycle.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CitizenStatus
{
	// Distraction cycle is approximately 80-85 seconds total
	// Distraction lasts ~12-17 seconds, then ~68 seconds until next distraction
	public static final int DISTRACTION_CYCLE_SECONDS = 83;
	public static final int OPTIMAL_HOP_WINDOW_START = 58; // ~25s until distraction - HOP NOW
	public static final int APPROACHING_WINDOW_START = 40; // ~43s until distraction - Approaching

	private int world;
	private boolean distracted;
	private long distractionStartTime;
	private long distractionEndTime;
	private long lastUpdateTime;
	private String reportedBy;

	public long getDistractionDurationSeconds()
	{
		if (!distracted || distractionStartTime == 0)
		{
			return 0;
		}
		return (System.currentTimeMillis() - distractionStartTime) / 1000;
	}

	public long getSecondsSinceUpdate()
	{
		if (lastUpdateTime == 0)
		{
			return Long.MAX_VALUE;
		}
		return (System.currentTimeMillis() - lastUpdateTime) / 1000;
	}

	public boolean isStale()
	{
		// Data is stale if no update in 2 full cycles
		return getSecondsSinceUpdate() > (DISTRACTION_CYCLE_SECONDS * 2);
	}

	/**
	 * Returns seconds since the last distraction ended.
	 * Returns -1 if currently distracted or no end time recorded.
	 */
	public long getSecondsSinceDistractionEnded()
	{
		if (distracted || distractionEndTime == 0)
		{
			return -1;
		}
		return (System.currentTimeMillis() - distractionEndTime) / 1000;
	}

	/**
	 * Returns estimated seconds until the next distraction.
	 * Based on ~93 second cycle from end of last distraction.
	 * Returns -1 if currently distracted or no data.
	 */
	public long getSecondsUntilNextDistraction()
	{
		if (distracted)
		{
			return 0; // Currently distracted
		}
		if (distractionEndTime == 0)
		{
			return -1; // No data
		}
		long secondsSinceEnd = getSecondsSinceDistractionEnded();
		long estimate = DISTRACTION_CYCLE_SECONDS - secondsSinceEnd;
		return Math.max(0, estimate);
	}

	/**
	 * Returns true if this world is in the optimal hop window (~25s+ after distraction ended).
	 */
	public boolean isInOptimalHopWindow()
	{
		if (distracted)
		{
			return false;
		}
		long secondsSinceEnd = getSecondsSinceDistractionEnded();
		if (secondsSinceEnd < 0)
		{
			return false;
		}
		return secondsSinceEnd >= OPTIMAL_HOP_WINDOW_START;
	}

	/**
	 * Returns true if approaching optimal window (~40-58s after distraction ended).
	 */
	public boolean isApproaching()
	{
		if (distracted)
		{
			return false;
		}
		long secondsSinceEnd = getSecondsSinceDistractionEnded();
		if (secondsSinceEnd < 0)
		{
			return false;
		}
		return secondsSinceEnd >= APPROACHING_WINDOW_START && secondsSinceEnd < OPTIMAL_HOP_WINDOW_START;
	}

	/**
	 * Returns a priority score for sorting. Lower = higher priority.
	 * Priority: 1) HOP NOW (optimal window), 2) Approaching, 3) Distracted, 4) Waiting
	 */
	public int getPriorityScore()
	{
		if (isStale())
		{
			return Integer.MAX_VALUE;
		}

		long secondsSinceEnd = getSecondsSinceDistractionEnded();

		// In optimal window - highest priority (HOP NOW)
		if (isInOptimalHopWindow())
		{
			// Within window, prioritize those who've been waiting longer
			return (int) (100 - secondsSinceEnd);
		}

		// Approaching window - second priority
		if (isApproaching())
		{
			return 200 + (int) (OPTIMAL_HOP_WINDOW_START - secondsSinceEnd);
		}

		// Currently distracted - third priority
		if (distracted)
		{
			return 300;
		}

		// Waiting (recently ended, 0-40s) - lowest priority
		if (secondsSinceEnd >= 0)
		{
			return 400 + (int) (APPROACHING_WINDOW_START - secondsSinceEnd);
		}

		return Integer.MAX_VALUE - 1; // No data
	}
}
