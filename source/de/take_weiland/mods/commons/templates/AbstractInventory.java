package de.take_weiland.mods.commons.templates;

import net.minecraft.item.ItemStack;
import de.take_weiland.mods.commons.util.CollectionUtils;
import de.take_weiland.mods.commons.util.Inventories;

public abstract class AbstractInventory implements AdvancedInventory {

	protected final ItemStack[] storage;
	
	protected AbstractInventory() {
		storage = provideStorage();
	}
	
	@Override
	public ItemStack[] getStorage() {
		return storage;
	}

	protected ItemStack[] provideStorage() {
		return new ItemStack[getSizeInventory()];
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return CollectionUtils.safeArrayAccess(storage, slot);
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		return Inventories.decreaseStackSize(this, slot, count);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		return Inventories.getAndRemove(this, slot);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack item) {
		if (slot >= 0 && slot < storage.length) {
			storage[slot] = item;
		}
		onInventoryChanged();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void onInventoryChanged() { }
	
	@Override
	public void openChest() { }

	@Override
	public void closeChest() { }

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack item) {
		return true;
	}

}
