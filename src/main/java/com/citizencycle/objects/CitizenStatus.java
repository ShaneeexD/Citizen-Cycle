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
	public static final int OPTIMAL_HOP_WINDOW_START = 68; // seconds after distraction ended
	public static final int OPTIMAL_HOP_WINDOW_END = 83;   // seconds after distraction ended

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
	 * Returns true if this world is in the optimal hop window (5-15 seconds until distraction).
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
		return secondsSinceEnd >= OPTIMAL_HOP_WINDOW_START && secondsSinceEnd <= OPTIMAL_HOP_WINDOW_END;
	}

	/**
	 * Returns a priority score for sorting. Lower = higher priority.
	 * Worlds in optimal hop window get highest priority.
	 * Then worlds approaching the window.
	 * Currently distracted worlds are lower priority (you'd miss the distraction).
	 */
	public int getPriorityScore()
	{
		if (isStale())
		{
			return Integer.MAX_VALUE;
		}

		// Currently distracted - low priority (you'd arrive after it ends)
		if (distracted)
		{
			return 1000;
		}

		long secondsSinceEnd = getSecondsSinceDistractionEnded();
		if (secondsSinceEnd < 0)
		{
			return Integer.MAX_VALUE - 1; // No data
		}

		// In optimal window (78-93s) - highest priority
		if (isInOptimalHopWindow())
		{
			// Within window, prioritize those closer to distraction
			return (int) (OPTIMAL_HOP_WINDOW_END - secondsSinceEnd);
		}

		// Approaching window (50-68s) - medium priority
		if (secondsSinceEnd >= 50 && secondsSinceEnd < OPTIMAL_HOP_WINDOW_START)
		{
			return 100 + (int) (OPTIMAL_HOP_WINDOW_START - secondsSinceEnd);
		}

		// Recently ended distraction (0-50s) - lower priority, need to wait
		return 500 + (int) (50 - secondsSinceEnd);
	}
}
