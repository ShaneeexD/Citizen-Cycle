package com.citizencycle.modules.sidepanel;

import com.citizencycle.objects.CitizenStatus;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CitizenSidePanel extends PluginPanel
{
	private final Consumer<Integer> worldHopCallback;
	private final JPanel worldListPanel;
	private final JLabel statusLabel;
	private final JLabel errorLabel;

	@Setter
	private int currentWorld;

	private List<CitizenStatus> citizenStatuses = new ArrayList<>();
	private List<World> worldList = new ArrayList<>();

	public CitizenSidePanel(Consumer<Integer> worldHopCallback)
	{
		this.worldHopCallback = worldHopCallback;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel titleLabel = new JLabel("Citizen Cycle");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
		headerPanel.add(titleLabel, BorderLayout.NORTH);

		statusLabel = new JLabel("Waiting for data...");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		headerPanel.add(statusLabel, BorderLayout.CENTER);

		errorLabel = new JLabel("");
		errorLabel.setForeground(Color.RED);
		headerPanel.add(errorLabel, BorderLayout.SOUTH);

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

	public void setWorldList(List<World> worlds)
	{
		this.worldList = worlds;
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

		List<CitizenStatus> sortedStatuses = new ArrayList<>(citizenStatuses);
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
				else if (status.getSecondsSinceDistractionEnded() >= 50)
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
		else if (status.getSecondsSinceDistractionEnded() >= 50)
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
		if (status.isDistracted())
		{
			long duration = status.getDistractionDurationSeconds();
			statusText = "DISTRACTED (" + duration + "s)";
			statusColor = Color.GREEN;
		}
		else if (status.isInOptimalHopWindow())
		{
			long secondsUntil = status.getSecondsUntilNextDistraction();
			statusText = "HOP NOW! ~" + secondsUntil + "s";
			statusColor = new Color(255, 200, 0);
		}
		else if (status.getSecondsSinceDistractionEnded() >= 50)
		{
			long secondsUntil = status.getSecondsUntilNextDistraction();
			statusText = "Approaching (~" + secondsUntil + "s)";
			statusColor = Color.YELLOW;
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
