package net.runelite.client.plugins.microbot.f2pfishing;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class F2pFishingOverlay extends Overlay {
	public F2pFishingOverlay() {
		setPosition(OverlayPosition.BOTTOM_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
}
