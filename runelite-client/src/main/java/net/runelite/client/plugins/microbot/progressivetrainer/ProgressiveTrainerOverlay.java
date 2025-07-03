package net.runelite.client.plugins.microbot.progressivetrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ProgressiveTrainerOverlay extends OverlayPanel {
    private final ProgressiveTrainerPlugin plugin;

    @Inject
    ProgressiveTrainerOverlay(ProgressiveTrainerPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 90));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Progressive Trainer")
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Task: " + plugin.getCurrentTask())
                .build());

        long minutes = plugin.getMinutesUntilSwitch();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next switch: " + minutes + "m")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left(Microbot.status)
                .build());
        return super.render(graphics);
    }
}
