package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "minMargin",
            name = "Minimum Margin",
            description = "Only flip items with at least this margin",
            position = 1
    )
    default int minMargin() { return 10; }
            keyName = "itemName",
            name = "Item Name",
            description = "Item to flip",
            position = 1
    )
    default String itemName() { return "Air rune"; }
}
