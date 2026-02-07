package net.runelite.client.plugins.microbot.f2pfishing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("f2pfishing")
public interface F2pFishingConfig extends Config {
	@Range(min = 0)
	@ConfigItem(
		keyName = "coinRestockThreshold",
		name = "Coin restock threshold",
		description = "Restock coins only when the count falls below this value"
	)
	default int coinRestockThreshold() {
		return 100;
	}
}
