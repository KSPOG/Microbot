package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {


    @ConfigItem(
            keyName = "minVolume",
            name = "Minimum Volume",
            description = "Skip items with daily volume below this value",
            position = 0
    )
    default int minVolume() { return 1000; }

    @ConfigItem(
            keyName = "delay",
            name = "Loop Delay (ms)",
            description = "Delay between flip checks",
            position = 1
    )
    default int delay() { return 1000; }

    @ConfigItem(
            keyName = "cancelMinutes",
            name = "Cancel Offer Minutes",
            description = "Cancel buy offers after this many minutes",
            position = 2
    )
    default int cancelMinutes() { return 25; }
}