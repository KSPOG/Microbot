package net.runelite.client.plugins.microbot.progressivetrainer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("progressivetrainer")
public interface ProgressiveTrainerConfig extends Config {
    @ConfigItem(
            keyName = "minSwitchMinutes",
            name = "Min switch minutes",
            description = "Minimum minutes before switching skills",
            position = 0
    )
    default int minSwitchMinutes() { return 30; }

    @ConfigItem(
            keyName = "maxSwitchMinutes",
            name = "Max switch minutes",
            description = "Maximum minutes before switching skills",
            position = 1
    )
    default int maxSwitchMinutes() { return 60; }
}
