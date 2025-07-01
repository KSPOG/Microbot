package net.runelite.client.plugins.microbot.geflipper;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.Default + "GE Flipper",
        description = "Microbot grand exchange flipper",
        tags = {"ge", "flipper", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class GEFlipperPlugin extends Plugin {
    static final String VERSION = "1.0";

    @Inject
    private GEFlipperConfig config;

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private GEFlipperOverlay overlay;
    @Inject
    private GEFlipperScript script;

    @Getter
    private Instant startTime;

    @Provides
    GEFlipperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GEFlipperConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        startTime = Instant.now();
        overlayManager.add(overlay);
        script.run(this, config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    long getProfit() { return script.getProfit(); }
    long getProfitPerHour() { return script.getProfitPerHour(); }
}
