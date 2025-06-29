package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "itemName",
            name = "Item Name",
            description = "Name of the item to flip. Leave blank for all F2P tradeables",
            position = 0
    )
    default String itemName() { return ""; }

    @ConfigItem(
            keyName = "minMargin",
            name = "Minimum Margin",
            description = "Only flip items with at least this margin",
            position = 1
    )
    default int minMargin() { return 10; }

    @ConfigItem(
            keyName = "minVolume",
            name = "Minimum Volume",
            description = "Skip items with daily volume below this value",
            position = 2
    )
    default int minVolume() { return 1000; }

    @ConfigItem(
            keyName = "delay",
            name = "Loop Delay (ms)",
            description = "Delay between flip checks",
            position = 3
    )
    default int delay() { return 1000; }

    @ConfigItem(
            keyName = "cancelMinutes",
            name = "Cancel Offer Minutes",
            description = "Cancel buy offers after this many minutes",
            position = 4
    )
    default int cancelMinutes() { return 25; }
}
