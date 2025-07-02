package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RandomTrainerConfig.GROUP)
public interface RandomTrainerConfig extends Config {
    String GROUP = "randomtrainer";

    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "switchDelay",
            name = "Skill Switch Delay (s)",
            description = "Time between selecting a new skill to train",
            position = 0,
            section = generalSection
    )
    default int switchDelay() { return 600; }

    @ConfigSection(
            name = "Combat",
            description = "Combat Training Goals",
            position = 1
    )
    String combatSection = "combat";

    @ConfigItem(
            keyName = "attackLevels",
            name = "Attack Levels",
            description = "Levels of Attack to train",
            position = 0,
            section = combatSection
    )
    default int attackLevels() { return 0; }

    @ConfigItem(
            keyName = "strengthLevels",
            name = "Strength Levels",
            description = "Levels of Strength to train",
            position = 1,
            section = combatSection
    )
    default int strengthLevels() { return 0; }

    @ConfigItem(
            keyName = "defenceLevels",
            name = "Defence Levels",
            description = "Levels of Defence to train",
            position = 2,
            section = combatSection
    )
    default int defenceLevels() { return 0; }

    @ConfigItem(
            keyName = "rangedLevels",
            name = "Ranged Levels",
            description = "Levels of Ranged to train",
            position = 3,
            section = combatSection
    )
    default int rangedLevels() { return 0; }

    @ConfigItem(
            keyName = "mageLevels",
            name = "Mage Levels",
            description = "Levels of Magic to train",
            position = 4,
            section = combatSection
    )
    default int mageLevels() { return 0; }
}
