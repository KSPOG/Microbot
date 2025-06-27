package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "itemName",
            name = "Item Name",
            description = "Item to flip",
            position = 1
    )
    default String itemName() { return "Air rune"; }
}
