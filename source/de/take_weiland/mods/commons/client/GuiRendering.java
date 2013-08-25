package de.take_weiland.mods.commons.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.SCGuiContainerAccessor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

public final class GuiRendering {

	private GuiRendering() { }
	
	public static final <T extends GuiContainer & ContainerGui<?>> void fillAreaWithIcon(Icon icon, int x, int y, int width, int height, T gui) {
		int xSize = SCGuiContainerAccessor.getSizeX(gui);
		int ySize = SCGuiContainerAccessor.getSizeY(gui);
		
		int cols = MathHelper.ceiling_float_int(width / 16F);
		int rows = MathHelper.ceiling_float_int(height / 16F);
//		System.out.println("cols: " + cols + " / rows: " + rows);
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				gui.drawTexturedModelRectFromIcon(x + col * 16, y + row * 16, icon, 16, 16);
			}
		}
		gui.getMinecraft().renderEngine.func_110577_a(gui.getTexture());
		gui.drawTexturedModalRect(x + width, y, x + width, y, xSize - (x + width), height);
		gui.drawTexturedModalRect(x, y + height, x, y + height, width, ySize - (y + height));
	}
	
	public static final <T extends GuiContainer & ContainerGui<?>> void drawFluidStack(IFluidTank tank, int x, int y, int width, int fullHeight, T gui) {
		FluidStack fluidStack = tank.getFluid();

		if (fluidStack != null) {
			Fluid fluid = fluidStack.getFluid();
			TextureManager engine = gui.getMinecraft().renderEngine;
			Icon fluidIcon = fluid.getStillIcon();
			int fluidHeight = MathHelper.ceiling_float_int((fluidStack.amount / (float)tank.getCapacity()) * fullHeight);

			engine.func_110577_a(engine.func_130087_a(fluid.getSpriteNumber()));
			fillAreaWithIcon(fluidIcon, x, y + fullHeight - fluidHeight, width, fluidHeight, gui);
		}
	}

}
