package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import java.awt.*;

public class GEFlipperOverlay extends OverlayPanel {
    private final GEFlipperPlugin plugin;

    @Inject
    GEFlipperOverlay(GEFlipperPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("GE Flipper " + GEFlipperScript.VERSION)
                    .color(Color.CYAN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(GEFlipperScript.status)
                    .build());

            long runtime = System.currentTimeMillis() - GEFlipperScript.startTime;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time Running:")
                    .right(formatDuration(runtime))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profit:")
                    .right(QuantityFormatter.formatNumber(GEFlipperScript.profit) + " gp")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profit p/h:")
                    .right(QuantityFormatter.formatNumber(GEFlipperScript.profitPerHour) + " gp")
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
