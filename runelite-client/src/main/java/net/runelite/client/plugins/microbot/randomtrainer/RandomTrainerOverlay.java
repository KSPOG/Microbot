package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class RandomTrainerOverlay extends OverlayPanel {
    private final RandomTrainerScript script;

    @Inject
    public RandomTrainerOverlay(RandomTrainerPlugin plugin, RandomTrainerScript script) {
        super(plugin);
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(200, 80));
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
        return super.render(graphics);
    }

}
