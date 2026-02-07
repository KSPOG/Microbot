package net.runelite.client.plugins.microbot.autofishing.enums;

public enum FishingMethod {
	SMALL_NET("Small net"),
	BIG_NET("Big net"),
	NET("Net"),
	BAIT("Bait"),
	LURE("Lure"),
	CAGE("Cage"),
	HARPOON("Harpoon");

	private final String displayName;

	FishingMethod(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
