package net.runelite.client.plugins.microbot.f2pfishing.enums;

import java.util.List;

public enum FishingMethod {
	SMALL_NET("Small net", "Net", List.of("Small fishing net"), 1, 100),
	BAIT_SARDINE_HERRING("Bait", "Bait", List.of("Fishing rod", "Fishing bait"), 5, 100),
	LURE_TROUT_SALMON("Lure", "Lure", List.of("Fly fishing rod", "Feather"), 20, 100),
	BAIT_PIKE("Bait", "Bait", List.of("Fishing rod", "Fishing bait"), 25, 100),
	CAGE("Cage", "Cage", List.of("Lobster pot"), 40, 100),
	HARPOON_TUNA_SWORDFISH("Harpoon", "Harpoon", List.of("Harpoon"), 35, 100);

	private final String displayName;
	private final String action;
	private final List<String> requiredItems;
	private final int levelRequired;
	private final int coinRestockThreshold;

	FishingMethod(
		String displayName,
		String action,
		List<String> requiredItems,
		int levelRequired,
		int coinRestockThreshold
	) {
		this.displayName = displayName;
		this.action = action;
		this.requiredItems = requiredItems;
		this.levelRequired = levelRequired;
		this.coinRestockThreshold = coinRestockThreshold;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getActions() {
		return List.of(action);
	}

	public List<String> getRequiredItems() {
		return requiredItems;
	}

	public int getLevelRequired() {
		return levelRequired;
	}

	public int getCoinRestockThreshold() {
		return coinRestockThreshold;
	}
}
