package de.take_weiland.mods.commons.client;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Abstract base class for GuiScreens which have a parent screen that should be reopened when this screen closes.
 * @author diesieben07
 */
public abstract class GuiScreenWithParent extends GuiScreen {

	/**
	 * The parent screen
	 */
	protected final GuiScreen parent;
	
	public GuiScreenWithParent(GuiScreen parent) {
		this.parent = parent;
	}

	/**
	 * Close this GuiScreen (display the parent screen again)
	 */
	protected void close() {
		mc.displayGuiScreen(parent);
	}

	@Override
	protected void keyTyped(char c, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			close();
		}
	}
	
}
