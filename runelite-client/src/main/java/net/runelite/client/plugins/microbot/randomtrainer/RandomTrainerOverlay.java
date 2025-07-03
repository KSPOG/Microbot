package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class RandomTrainerOverlay extends OverlayPanel {
    private final RandomTrainerScript script;

    @Inject
    public RandomTrainerOverlay(RandomTrainerScript script) {
        super();
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(200, 96));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Random Trainer V" + RandomTrainerScript.VERSION)
                .color(Color.GREEN)
                .build());
        panelComponent.getChildren().add(LineComponent.builder().build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status: " + Microbot.status)
                .build());
        String task = "None";
        if (script != null) {
            task = script.getCurrentTaskName();
        }
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current Task: " + task)
                .build());
        String runtime = script != null ? script.getTimeRunning() : "00:00:00";
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time Running: " + runtime)
                .build());
        return super.render(graphics);
    }
}
