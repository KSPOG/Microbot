package net.runelite.client.plugins.microbot.f2pfishing.enums;

import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

public enum Fish {
	SHRIMP_AND_ANCHOVIES(
		List.of("Raw shrimps", "Raw anchovies"),
		new int[]{ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES},
		new int[]{ItemID.SHRIMPS, ItemID.ANCHOVIES},
		FishingMethod.SMALL_NET,
		new int[]{NpcID.FISHING_SPOT},
		Collections.emptyList()
	),
	SARDINE(
		List.of("Raw sardine"),
		new int[]{ItemID.RAW_SARDINE},
		new int[]{ItemID.SARDINE},
		FishingMethod.BAIT_SARDINE_HERRING,
		new int[]{NpcID.ROD_FISHING_SPOT},
		Collections.emptyList()
	),
	HERRING(
		List.of("Raw herring"),
		new int[]{ItemID.RAW_HERRING},
		new int[]{ItemID.HERRING},
		FishingMethod.BAIT_SARDINE_HERRING,
		new int[]{NpcID.ROD_FISHING_SPOT},
		Collections.emptyList()
	),
	TROUT_AND_SALMON(
		List.of("Raw trout", "Raw salmon"),
		new int[]{ItemID.RAW_TROUT, ItemID.RAW_SALMON},
		new int[]{ItemID.TROUT, ItemID.SALMON},
		FishingMethod.LURE_TROUT_SALMON,
		new int[]{NpcID.ROD_FISHING_SPOT},
		Collections.emptyList()
	),
	PIKE(
		List.of("Raw pike"),
		new int[]{ItemID.RAW_PIKE},
		new int[]{ItemID.PIKE},
		FishingMethod.BAIT_PIKE,
		new int[]{NpcID.ROD_FISHING_SPOT},
		Collections.emptyList()
	),
	LOBSTER(
		List.of("Raw lobster"),
		new int[]{ItemID.RAW_LOBSTER},
		new int[]{ItemID.LOBSTER},
		FishingMethod.CAGE,
		new int[]{NpcID.FISHING_SPOT},
		Collections.emptyList()
	),
	TUNA_AND_SWORDFISH(
		List.of("Raw tuna", "Raw swordfish"),
		new int[]{ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH},
		new int[]{ItemID.TUNA, ItemID.SWORDFISH},
		FishingMethod.HARPOON_TUNA_SWORDFISH,
		new int[]{NpcID.FISHING_SPOT},
		Collections.emptyList()
	);

	private final List<String> itemNames;
	private final int rawItemId;
	private final int cookedItemId;
	private final int[] rawItemIds;
	private final int[] cookedItemIds;
	private final FishingMethod method;
	private final int[] fishingSpot;
	private final List<WorldPoint> locations;

	Fish(
		List<String> itemNames,
		int[] rawItemIds,
		int[] cookedItemIds,
		FishingMethod method,
		int[] fishingSpot,
		List<WorldPoint> locations
	) {
		this.itemNames = itemNames;
		this.rawItemIds = rawItemIds;
		this.cookedItemIds = cookedItemIds;
		this.rawItemId = rawItemIds.length > 0 ? rawItemIds[0] : -1;
		this.cookedItemId = cookedItemIds.length > 0 ? cookedItemIds[0] : -1;
		this.method = method;
		this.fishingSpot = fishingSpot;
		this.locations = locations;
	}

	public List<String> getItemNames() {
		return itemNames;
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

	public List<String> getActions() {
		if (method == null) {
			return Collections.emptyList();
		}
		return method.getActions();
	}

	public List<String> getRequiredItems() {
		if (method == null) {
			return Collections.emptyList();
		}
		return method.getRequiredItems();
	}

	public int[] getFishingSpot() {
		return fishingSpot.clone();
	}

	public WorldPoint getClosestLocation(WorldPoint playerLocation) {
		if (locations.isEmpty() || playerLocation == null) {
			return null;
		}

		WorldPoint closest = null;
		int bestDistance = Integer.MAX_VALUE;
		for (WorldPoint location : locations) {
			int distance = location.distanceTo(playerLocation);
			if (distance < bestDistance) {
				bestDistance = distance;
				closest = location;
			}
		}

		return closest;
	}

	public FishingMethod getMethod() {
		return method;
	}

	public int getCoinRestockThreshold() {
		if (method == null) {
			return 0;
		}
		return method.getCoinRestockThreshold();
	}
}
