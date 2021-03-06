package de.take_weiland.mods.commons.client;

import com.google.common.collect.Lists;
import de.take_weiland.mods.commons.util.SCReflector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

import java.util.List;

import static de.take_weiland.mods.commons.client.Guis.isPointInRegion;
import static org.lwjgl.opengl.GL11.*;

public abstract class ScrollPane extends Gui {

	private boolean clip;

	protected int x;
	protected int y;
	protected int width;
	protected int height;
	protected int contentHeight;

	protected int scrollbarWidth = 10;
	protected int scrollbarHeight = 30;
	private int scrollbarClickOffset = 0;
	private int scrollbarY = 0;
	private boolean isDragging = false;

	private final GuiScreen screen;
	protected final Minecraft mc = Minecraft.getMinecraft();
	private final List<GuiButton> buttons = Lists.newArrayList();

	public ScrollPane(GuiScreen screen, int x, int y, int width, int height, int contentHeight) {
		this.screen = screen;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.contentHeight = contentHeight;
	}

	private int newScrollIfInside;

	public final void onMouseWheel(int dWheel) {
		if (needsScrollbar() && dWheel != 0) {
			dWheel = -Integer.signum(dWheel) * 5;
			newScrollIfInside = MathHelper.clamp_int(scrollbarY + dWheel, -1, height - scrollbarHeight);
		}
	}

	public final void draw(int mouseX, int mouseY) {
		if (newScrollIfInside >= 0 && Guis.isPointInRegion(x, y, width, height, mouseX, mouseY)) {
			scrollbarY = newScrollIfInside;
			newScrollIfInside = -1;
		}

		float yTranslate = computeYTranslate();
		mouseX -= x;
		mouseY -= yTranslate;

		glColor3f(1, 1, 1);
		int scale = Guis.computeGuiScale();

		if (clip) {
			glScissor(0, mc.displayHeight - (y + height) * scale, (width + x) * scale, height * scale);
			glEnable(GL_SCISSOR_TEST);
		}

		glPushMatrix();

		glTranslatef(x, yTranslate, 0);

		drawInternal(mouseX, mouseY);

		glPopMatrix();

		if (clip) {
			glDisable(GL_SCISSOR_TEST);
		}

		if (needsScrollbar()) {
			int scrollbarX = x + (width - scrollbarWidth);
			drawScrollbar(scrollbarX, y + scrollbarY, scrollbarX + scrollbarWidth, y + scrollbarY + scrollbarHeight);
		}
	}

	private float computeYTranslate() {
		return y - ((scrollbarY) / (float) (height - scrollbarHeight)) * (contentHeight - height);
	}

	private void drawInternal(int mouseX, int mouseY) {
		drawImpl(mouseX, mouseY);
		int n = buttons.size();
		for (int i = 0; i < n; ++i) {
			buttons.get(i).drawButton(mc, mouseX, mouseY);
		}
	}

	private boolean needsScrollbar() {
		return height < contentHeight;
	}

	public final void onMouseClick(int mouseX, int mouseY, int btn) {
		if (btn == 0 && needsScrollbar()) {
			if (Guis.isPointInRegion(x + (width - scrollbarWidth), y + scrollbarY, scrollbarWidth, scrollbarHeight, mouseX, mouseY)) {
				isDragging = true;
				scrollbarClickOffset = mouseY - (y + scrollbarY);
			}
		}

		mouseX -= x;
		mouseY -= computeYTranslate();
		if (isPointInRegion(0, 0, width, height, mouseX, mouseY)) {
			for (GuiButton button : buttons) {
				if (button.mousePressed(mc, mouseX, mouseY)) {
					SCReflector.instance.actionPerformed(screen, button);
				}
			}
		}
		handleMouseClick(mouseX, mouseY, btn);
	}

	public final void onMouseBtnReleased(int btn) {
		if (btn == 0) {
			isDragging = false;
		}
	}

	public final void onMouseMoved(int mouseX, int mouseY) {
		if (isDragging) {
			scrollbarY = MathHelper.clamp_int(mouseY - y - scrollbarClickOffset, 0, height - scrollbarHeight);
		}
	}

	public final void setScrollbarWidth(int scrollbarWidth) {
		this.scrollbarWidth = scrollbarWidth;
	}

	public final void setX(int x) {
		this.x = x;
	}

	public final void setY(int y) {
		this.y = y;
	}

	public final void setWidth(int width) {
		this.width = width;
	}

	public final void setHeight(int height) {
		if (this.height != height) {
			recalcScrollbar();
		}
		this.height = height;
	}

	public final void setContentHeight(int contentHeight) {
		if (this.contentHeight != contentHeight) {
			recalcScrollbar();
		}
		this.contentHeight = contentHeight;
	}

	private void recalcScrollbar() {
		scrollbarY = 0;
		isDragging = false;
	}

	public final void clearButtons() {
		buttons.clear();
	}

	public final void addButton(GuiButton button) {
		buttons.add(button);
	}

	public final void setClip(boolean clip) {
		this.clip = clip;
	}

	protected void drawScrollbar(int x, int y, int x2, int y2) {
		drawRect(x, y, x2, y2, 0xff000000);
		drawRect(x, y, x + 1, y2, 0xff444444);
		drawRect(x2 - 1, y, x2, y2, 0xff444444);
		drawRect(x, y, x2, y + 1, 0xff444444);
		drawRect(x, y2 - 1, x2, y2, 0xff444444);
	}

	protected void drawImpl(int mouseX, int mouseY) {
		drawImpl();
	}

	protected abstract void drawImpl();

	protected void handleMouseClick(int relX, int relY, int btn) {
	}

}
