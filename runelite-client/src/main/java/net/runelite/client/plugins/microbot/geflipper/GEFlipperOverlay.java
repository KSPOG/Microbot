package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import java.text.NumberFormat;

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

            NumberFormat fmt = NumberFormat.getIntegerInstance();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profit:")
                    .right(fmt.format(GEFlipperScript.profit))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profit p/h:")
                    .right(fmt.format(GEFlipperScript.profitPerHour))
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }
}