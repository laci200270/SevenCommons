package de.take_weiland.mods.commons.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.relauncher.Side;
import de.take_weiland.mods.commons.internal.PacketContainerSync;
import de.take_weiland.mods.commons.util.Containers;
import de.take_weiland.mods.commons.util.Sides;

public abstract class AbstractContainer<T extends IInventory> extends Container implements AdvancedContainer<T> {

	protected final T inventory;
	
	protected final EntityPlayer player;
	
	private final int firstPlayerSlot;
	
	public final Iterable<EntityPlayer> viewingPlayers = Iterables.filter(crafters, EntityPlayer.class);
	
	protected AbstractContainer(T upper, EntityPlayer player) {
		this(upper, player, 8, 84);
	}
	
	protected AbstractContainer(T upper, EntityPlayer player, int playerInventoryX, int playerInventoryY) {
		inventory = upper;
		this.player = player;
		addSlots();
		firstPlayerSlot = inventorySlots.size();
		Containers.addPlayerInventory(this, player.inventory, playerInventoryX, playerInventoryY);
	}
	
	protected AbstractContainer(World world, int x, int y, int z, EntityPlayer player) {
		this(world, x, y, z, player, 8, 84);
	}
	
	@SuppressWarnings("unchecked")
	protected AbstractContainer(World world, int x, int y, int z, EntityPlayer player, int playerInventoryX, int playerInventoryY) {
		this((T) world.getBlockTileEntity(x, y, z), player, playerInventoryX, playerInventoryY);
	}
	
	@Override
	public int getFirstPlayerSlot() {
		return firstPlayerSlot;
	}

	protected abstract void addSlots();
	
	@Override
	public final T inventory() {
		return inventory;
	}
	
	@Override
	public EntityPlayer getPlayer() {
		return player;
	}
	
	@Override
	public boolean handlesButton(EntityPlayer player, int buttonId) {
		return false;
	}
	
	@Override
	public void clickButton(Side side, EntityPlayer player, int buttonId) { }

	@Override
	public boolean enchantItem(EntityPlayer player, int id) {
		clickButton(Sides.logical(player), player, id);
		return true;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return inventory.isUseableByPlayer(player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
		return Containers.transferStack(this, player, slot);
	}
	
	protected boolean enableSyncing() {
		return false;
	}
	
	@Override
	public boolean prepareSyncData() {
		return false;
	}
	
	@Override
	public void writeSyncData(ByteArrayDataOutput out, boolean all) { }
	
	@Override
	public void readSyncData(ByteArrayDataInput in) { }

	private boolean noSync = false;

	@Override
	public void addCraftingToCrafters(ICrafting crafter) {
		noSync = true; // dirty hack to stop sending data twice because super.addCraftingToCrafters calls detectAndSendChanges
		super.addCraftingToCrafters(crafter);
		noSync = false;
		if (enableSyncing() && crafter instanceof EntityPlayer) {
			new PacketContainerSync(this, true).sendToPlayer((EntityPlayer) crafter);
		}
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (!noSync && enableSyncing() && prepareSyncData()) {
			new PacketContainerSync(this, false).sendTo(viewingPlayers);
		}
	}

}