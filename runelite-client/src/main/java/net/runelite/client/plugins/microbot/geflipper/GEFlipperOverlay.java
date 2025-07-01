package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

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
        panelComponent.setPreferredSize(new Dimension(200, 100));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("GE Flipper V" + GEFlipperPlugin.VERSION)
                .color(Color.GREEN)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Profit:")
                .right(Long.toString(plugin.getProfit()))
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Profit p/h:")
                .right(Long.toString(plugin.getProfitPerHour()))
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Run Time:")
                .right(TimeUtils.getFormattedDurationBetween(plugin.getStartTime(), Instant.now()))
                .build());
        return super.render(graphics);
    }
}
