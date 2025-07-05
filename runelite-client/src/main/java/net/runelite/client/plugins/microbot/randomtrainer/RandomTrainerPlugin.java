package net.runelite.client.plugins.microbot.randomtrainer;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.AWTException;

@PluginDescriptor(
        name = PluginDescriptor.KSP + "Random Trainer",
        description = "Trains random skills",
        tags = {"random", "trainer", "microbot"},
        enabledByDefault = false
)
public class RandomTrainerPlugin extends Plugin {
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
        if (script.run(config, this)) {
            overlayManager.add(overlay);
        }
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
