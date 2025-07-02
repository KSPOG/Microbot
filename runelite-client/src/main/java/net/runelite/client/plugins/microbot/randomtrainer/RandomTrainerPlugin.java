package net.runelite.client.plugins.microbot.randomtrainer;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Random Trainer",
        description = "Trains random skills",
        tags = {"random", "trainer", "microbot"},
        enabledByDefault = false
)
public class RandomTrainerPlugin extends Plugin {
    // Script version for display in the overlay
    static final String VERSION = RandomTrainerScript.VERSION;
    @Inject
    private RandomTrainerConfig config;

    @Provides
    RandomTrainerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RandomTrainerConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RandomTrainerOverlay overlay;
    @Inject
    private RandomTrainerScript script;

    @Override
    protected void startUp() throws AWTException {
        overlayManager.add(overlay);
        script.run(config, this);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    public boolean isBreakHandlerEnabled() {
        return net.runelite.client.plugins.microbot.Microbot.isPluginEnabled(net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin.class);
    }
}