package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "minVolume",
            name = "Minimum Volume",
            description = "Skip items with volume below this",
            position = 1
    )
    default int minVolume() { return 500; }

    @ConfigItem(
            keyName = "minMargin",
            name = "Minimum Margin",
            description = "Minimum gp margin to flip",
            position = 2
    )
    default int minMargin() { return 10; }

    @ConfigItem(
            keyName = "apiKey",
            name = "GE Tracker API Key",
            description = "Key for accessing GE Tracker API",
            position = 3
    )
    default String apiKey() { return ""; }

}
