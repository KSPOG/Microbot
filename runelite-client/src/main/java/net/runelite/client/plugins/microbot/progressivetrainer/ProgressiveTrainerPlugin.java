package net.runelite.client.plugins.microbot.progressivetrainer;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Progressive Trainer",
        description = "Progressively trains skills using existing Microbot plugins",
        tags = {"microbot", "training", "progressive"},
        enabledByDefault = false
)
public class ProgressiveTrainerPlugin extends Plugin {
    @Inject
    private ProgressiveTrainerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ProgressiveTrainerOverlay overlay;

    @Provides
    ProgressiveTrainerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProgressiveTrainerConfig.class);
    }

    @Inject
    private ProgressiveTrainerScript script;

    public String getCurrentTask() {
        return script.getCurrentTask();
    }

    public long getMinutesUntilSwitch() {
        return script.getMinutesUntilSwitch();
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        if (overlayManager != null) {
            overlayManager.remove(overlay);
        }
    }
}
