package net.runelite.client.plugins.microbot.f2pfishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Script;

@Slf4j
public class F2pFishingScript extends Script {
	private F2pFishingConfig config;

	public boolean run(F2pFishingConfig config) {
		this.config = config;
		return true;
	}

	public boolean shouldRestockCoins(int coinCount) {
		int threshold = config != null ? config.coinRestockThreshold() : 100;
		return coinCount < threshold;
	}
}
