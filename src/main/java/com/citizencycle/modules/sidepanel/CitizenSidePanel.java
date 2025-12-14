package com.citizencycle.modules.sidepanel;

import com.citizencycle.objects.CitizenStatus;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CitizenSidePanel extends PluginPanel
{
	private final Consumer<Integer> worldHopCallback;
	private final JPanel worldListPanel;
	private final JLabel statusLabel;
	private final JLabel errorLabel;

	@Setter
	private int currentWorld;

	@Setter
	private int playerTotalLevel = 0;

	private List<CitizenStatus> citizenStatuses = new ArrayList<>();
	private Map<Integer, World> worldMap = new java.util.HashMap<>();

	// Filter toggles
	private boolean showHopNow = true;
	private boolean showApproaching = true;
	private boolean showDistracted = true;
	private boolean showWaiting = true;

	private JToggleButton hopNowBtn;
	private JToggleButton approachingBtn;
	private JToggleButton distractedBtn;
	private JToggleButton waitingBtn;

	public CitizenSidePanel(Consumer<Integer> worldHopCallback)
	{
		this.worldHopCallback = worldHopCallback;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel titleLabel = new JLabel("Citizen Cycle");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(titleLabel);

		statusLabel = new JLabel("Waiting for data...");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(statusLabel);

		errorLabel = new JLabel("");
		errorLabel.setForeground(Color.RED);
		errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(errorLabel);

		headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));

		// Filter buttons panel
		JPanel filterPanel = new JPanel(new GridLayout(2, 2, 4, 4));
		filterPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		filterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		hopNowBtn = createFilterButton("Hop Now", new Color(255, 200, 0), true);
		approachingBtn = createFilterButton("Approaching", Color.YELLOW, true);
		distractedBtn = createFilterButton("Distracted", Color.GREEN, true);
		waitingBtn = createFilterButton("Waiting", Color.GRAY, true);

		hopNowBtn.addActionListener(e -> {
			showHopNow = hopNowBtn.isSelected();
			updateFilterButtonStyle(hopNowBtn, new Color(255, 200, 0), showHopNow);
			rebuild();
		});
		approachingBtn.addActionListener(e -> {
			showApproaching = approachingBtn.isSelected();
			updateFilterButtonStyle(approachingBtn, Color.YELLOW, showApproaching);
			rebuild();
		});
		distractedBtn.addActionListener(e -> {
			showDistracted = distractedBtn.isSelected();
			updateFilterButtonStyle(distractedBtn, Color.GREEN, showDistracted);
			rebuild();
		});
		waitingBtn.addActionListener(e -> {
			showWaiting = waitingBtn.isSelected();
			updateFilterButtonStyle(waitingBtn, Color.GRAY, showWaiting);
			rebuild();
		});

		filterPanel.add(hopNowBtn);
		filterPanel.add(approachingBtn);
		filterPanel.add(distractedBtn);
		filterPanel.add(waitingBtn);

		headerPanel.add(filterPanel);
		headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));

		add(headerPanel, BorderLayout.NORTH);

		worldListPanel = new JPanel();
		worldListPanel.setLayout(new BoxLayout(worldListPanel, BoxLayout.Y_AXIS));
		worldListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(worldListPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane, BorderLayout.CENTER);
	}

	private JToggleButton createFilterButton(String text, Color color, boolean selected)
	{
		JToggleButton btn = new JToggleButton(text, selected);
		btn.setFocusPainted(false);
		btn.setFont(btn.getFont().deriveFont(Font.BOLD, 10f));
		updateFilterButtonStyle(btn, color, selected);
		return btn;
	}

	private void updateFilterButtonStyle(JToggleButton btn, Color color, boolean selected)
	{
		if (selected)
		{
			btn.setBackground(color.darker().darker());
			btn.setForeground(color);
		}
		else
		{
			btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			btn.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		}
	}

	private boolean shouldShowStatus(CitizenStatus status)
	{
		if (status.isInOptimalHopWindow())
		{
			return showHopNow;
		}
		else if (status.isApproaching())
		{
			return showApproaching;
		}
		else if (status.isDistracted())
		{
			return showDistracted;
		}
		else
		{
			return showWaiting;
		}
	}

	public void setWorldList(List<World> worlds)
	{
		this.worldMap.clear();
		for (World world : worlds)
		{
			this.worldMap.put(world.getId(), world);
		}
	}

	/**
	 * Check if a world is safe to hop to (not PvP, not beta, meets total level requirement)
	 */
	private boolean isWorldSafe(int worldId)
	{
		World world = worldMap.get(worldId);
		if (world == null)
		{
			return true; // Allow if we don't have world data
		}

		EnumSet<WorldType> types = world.getTypes();

		// Filter out dangerous/restricted worlds
		if (types.contains(WorldType.PVP) ||
			types.contains(WorldType.HIGH_RISK) ||
			types.contains(WorldType.DEADMAN) ||
			types.contains(WorldType.NOSAVE_MODE) ||
			types.contains(WorldType.SEASONAL) ||
			types.contains(WorldType.TOURNAMENT) ||
			types.contains(WorldType.FRESH_START_WORLD) ||
			types.contains(WorldType.BETA_WORLD))
		{
			return false;
		}

		// Check total level requirements
		if (types.contains(WorldType.SKILL_TOTAL))
		{
			int requiredTotal = getRequiredTotalLevel(world);
			if (playerTotalLevel > 0 && playerTotalLevel < requiredTotal)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Get the required total level for a world based on its ID
	 */
	private int getRequiredTotalLevel(World world)
	{
		// World IDs for total level worlds
		// 500 total: 373, 386, 393, 390
		// 750 total: 320, 329, 389, 395
		// 1250 total: 361, 366, 378, 383
		// 1500 total: 375, 376, 377, 386
		// 1750 total: 466, 463, 480, 479
		// 2000 total: 360, 367, 371, 374
		// 2200 total: 365, 368, 369, 370
		int id = world.getId();
		
		if (id == 365 || id == 368 || id == 369 || id == 370)
		{
			return 2200;
		}
		else if (id == 360 || id == 367 || id == 371 || id == 374)
		{
			return 2000;
		}
		else if (id == 466 || id == 463 || id == 480 || id == 479)
		{
			return 1750;
		}
		else if (id == 375 || id == 376 || id == 377 || id == 386)
		{
			return 1500;
		}
		else if (id == 361 || id == 366 || id == 378 || id == 383)
		{
			return 1250;
		}
		else if (id == 320 || id == 329 || id == 389 || id == 395)
		{
			return 750;
		}
		else if (id == 373 || id == 393 || id == 390)
		{
			return 500;
		}
		
		return 0;
	}

	public void updateCitizenData(List<CitizenStatus> statuses)
	{
		this.citizenStatuses = statuses;
		setErrorMessage("");
	}

	public void setErrorMessage(String message)
	{
		errorLabel.setText(message);
	}

	public void rebuild()
	{
		worldListPanel.removeAll();

		// Filter to only safe worlds and sort by priority
		List<CitizenStatus> sortedStatuses = new ArrayList<>();
		for (CitizenStatus status : citizenStatuses)
		{
			if (isWorldSafe(status.getWorld()))
			{
				sortedStatuses.add(status);
			}
		}
		// Sort by priority score (lower = higher priority = closer to next distraction)
		sortedStatuses.sort((a, b) -> Integer.compare(a.getPriorityScore(), b.getPriorityScore()));

		int optimalCount = 0;
		int approachingCount = 0;
		for (CitizenStatus status : sortedStatuses)
		{
			if (!status.isStale())
			{
				if (status.isInOptimalHopWindow())
				{
					optimalCount++;
				}
				else if (status.isApproaching())
				{
					approachingCount++;
				}
			}
		}

		if (optimalCount > 0)
		{
			statusLabel.setText(optimalCount + " world(s) ready to hop!");
		}
		else if (approachingCount > 0)
		{
			statusLabel.setText(approachingCount + " world(s) approaching");
		}
		else
		{
			statusLabel.setText("Tracking " + sortedStatuses.size() + " world(s)");
		}

		for (CitizenStatus status : sortedStatuses)
		{
			if (status.isStale())
			{
				continue;
			}

			// Apply filter toggles
			if (!shouldShowStatus(status))
			{
				continue;
			}

			JPanel worldPanel = createWorldPanel(status);
			worldListPanel.add(worldPanel);
			worldListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		worldListPanel.revalidate();
		worldListPanel.repaint();
	}

	private JPanel createWorldPanel(CitizenStatus status)
	{
		JPanel panel = new JPanel(new BorderLayout());
		
		// Color based on state: optimal window = gold, approaching = yellow, distracted = green, other = gray
		Color bgColor;
		Color borderColor;
		if (status.isInOptimalHopWindow())
		{
			bgColor = new Color(100, 80, 0); // Gold/orange for optimal
			borderColor = new Color(255, 200, 0);
		}
		else if (status.isApproaching())
		{
			bgColor = new Color(80, 80, 0); // Yellow for approaching
			borderColor = Color.YELLOW;
		}
		else if (status.isDistracted())
		{
			bgColor = new Color(0, 80, 0); // Green for currently distracted
			borderColor = Color.GREEN;
		}
		else
		{
			bgColor = ColorScheme.DARKER_GRAY_COLOR;
			borderColor = ColorScheme.MEDIUM_GRAY_COLOR;
		}
		
		panel.setBackground(bgColor);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor),
			new EmptyBorder(8, 8, 8, 8)
		));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

		JPanel leftPanel = new JPanel(new GridLayout(2, 1));
		leftPanel.setOpaque(false);

		JLabel worldLabel = new JLabel("World " + status.getWorld());
		worldLabel.setForeground(Color.WHITE);
		worldLabel.setFont(worldLabel.getFont().deriveFont(Font.BOLD));

		String statusText;
		Color statusColor;
		if (status.isInOptimalHopWindow())
		{
			long secondsUntil = status.getSecondsUntilNextDistraction();
			statusText = "HOP NOW! ~" + secondsUntil + "s";
			statusColor = new Color(255, 200, 0);
		}
		else if (status.isApproaching())
		{
			long secondsUntil = status.getSecondsUntilNextDistraction();
			statusText = "Approaching (~" + secondsUntil + "s)";
			statusColor = Color.YELLOW;
		}
		else if (status.isDistracted())
		{
			long duration = status.getDistractionDurationSeconds();
			statusText = "DISTRACTED (" + duration + "s)";
			statusColor = Color.GREEN;
		}
		else if (status.getSecondsSinceDistractionEnded() >= 0)
		{
			long secondsUntil = status.getSecondsUntilNextDistraction();
			statusText = "Waiting (~" + secondsUntil + "s)";
			statusColor = Color.GRAY;
		}
		else
		{
			statusText = "No timing data";
			statusColor = Color.GRAY;
		}

		JLabel statusTextLabel = new JLabel(statusText);
		statusTextLabel.setForeground(statusColor);

		leftPanel.add(worldLabel);
		leftPanel.add(statusTextLabel);

		panel.add(leftPanel, BorderLayout.CENTER);

		if (status.getWorld() != currentWorld)
		{
			JButton hopButton = new JButton("Hop");
			hopButton.setFocusPainted(false);
			hopButton.addActionListener(e -> worldHopCallback.accept(status.getWorld()));
			panel.add(hopButton, BorderLayout.EAST);
		}
		else
		{
			JLabel currentLabel = new JLabel("Current");
			currentLabel.setForeground(Color.CYAN);
			panel.add(currentLabel, BorderLayout.EAST);
		}

		return panel;
	}
}
