package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "cancelMinutes",
            name = "Cancel After Minutes",
            description = "Abort offers after this many minutes",
            position = 0
    )
    default int cancelAfterMinutes() { return 15; }

    @ConfigItem(
            keyName = "useVolume",
            name = "Use Trade Volume",
            description = "Only flip items with trade volume",
            position = 1
    )
    default boolean useTradeVolume() { return false; }

    @ConfigItem(
            keyName = "minVolume",
            name = "Minimum Trade Volume",
            description = "Minimum trade volume when volume check enabled",
            position = 2
    )
    default int minimumTradeVolume() { return 1000; }
}

}
