package net.runelite.client.plugins.microbot.autofishing.enums;

import net.runelite.api.ItemID;

public enum Fish {
	SHRIMP("Shrimp", ItemID.RAW_SHRIMPS, ItemID.SHRIMPS),

	SHRIMP_AND_ANCHOVIES(
		"Shrimp + Anchovies",
		new int[]{ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES},
		new int[]{ItemID.SHRIMPS, ItemID.ANCHOVIES}
	),
	ANCHOVIES("Anchovies", ItemID.RAW_ANCHOVIES, ItemID.ANCHOVIES),
	SARDINE_AND_HERRING(
		"Sardine + Herring",
		new int[]{ItemID.RAW_SARDINE, ItemID.RAW_HERRING},
		new int[]{ItemID.SARDINE, ItemID.HERRING}
	),
	PIKE("Pike", ItemID.RAW_PIKE, ItemID.PIKE),
	SARDINE("Sardine", ItemID.RAW_SARDINE, ItemID.SARDINE),
	HERRING("Herring", ItemID.RAW_HERRING, ItemID.HERRING),
	TROUT_AND_SALMON(
		"Trout + Salmon",
		new int[]{ItemID.RAW_TROUT, ItemID.RAW_SALMON},
		new int[]{ItemID.TROUT, ItemID.SALMON}
	),
	TROUT("Trout", ItemID.RAW_TROUT, ItemID.TROUT),
	SALMON("Salmon", ItemID.RAW_SALMON, ItemID.SALMON),
	TUNA_AND_SWORDFISH(
		"Tuna + Swordfish",
		new int[]{ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH},
		new int[]{ItemID.TUNA, ItemID.SWORDFISH}
	),

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

	private final int[] rawItemIds;
	private final int[] cookedItemIds;

	Fish(String name, int rawItemId, int cookedItemId) {
		this(name, new int[]{rawItemId}, new int[]{cookedItemId});
	}

	Fish(String name, int[] rawItemIds, int[] cookedItemIds) {
		this.name = name;
		this.rawItemIds = rawItemIds;
		this.cookedItemIds = cookedItemIds;
		this.rawItemId = rawItemIds.length > 0 ? rawItemIds[0] : -1;
		this.cookedItemId = cookedItemIds.length > 0 ? cookedItemIds[0] : -1;


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


	public int[] getRawItemIds() {
		return rawItemIds.clone();
	}

	public int[] getCookedItemIds() {
		return cookedItemIds.clone();
	}

}
