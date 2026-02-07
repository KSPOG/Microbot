package net.runelite.client.plugins.microbot.autofishing.enums;

import net.runelite.api.ItemID;

public enum Fish {
	SHRIMP("Shrimp", ItemID.RAW_SHRIMPS, ItemID.SHRIMPS),
	ANCHOVIES("Anchovies", ItemID.RAW_ANCHOVIES, ItemID.ANCHOVIES),
	SARDINE("Sardine", ItemID.RAW_SARDINE, ItemID.SARDINE),
	HERRING("Herring", ItemID.RAW_HERRING, ItemID.HERRING),
	TROUT("Trout", ItemID.RAW_TROUT, ItemID.TROUT),
	SALMON("Salmon", ItemID.RAW_SALMON, ItemID.SALMON),
	TUNA("Tuna", ItemID.RAW_TUNA, ItemID.TUNA),
	LOBSTER("Lobster", ItemID.RAW_LOBSTER, ItemID.LOBSTER),
	SWORDFISH("Swordfish", ItemID.RAW_SWORDFISH, ItemID.SWORDFISH);

	private final String name;
	private final int rawItemId;
	private final int cookedItemId;

	Fish(String name, int rawItemId, int cookedItemId) {
		this.name = name;
		this.rawItemId = rawItemId;
		this.cookedItemId = cookedItemId;
	}

	public String getName() {
		return name;
	}

	public int getRawItemId() {
		return rawItemId;
	}

	public int getCookedItemId() {
		return cookedItemId;
	}
}
