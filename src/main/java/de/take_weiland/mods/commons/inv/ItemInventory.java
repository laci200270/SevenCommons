package de.take_weiland.mods.commons.inv;

import com.google.common.collect.ImmutableSet;
import de.take_weiland.mods.commons.nbt.NBTData;
import de.take_weiland.mods.commons.util.ItemStacks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.UUID;


/**
 * <p>An inventory that stores its contents to an {@link net.minecraft.item.ItemStack}.</p>
 * <p>ItemStacks that provide an ItemInventory must be protected inside a Container that displays their ItemInventory.
 * Guard against this by either using {@link SimpleSlot}, {@link Containers#addPlayerInventory(Container, InventoryPlayer)
 * Containers.addPlayerInventory} or by calling {@link #canTakeStack(Container, ItemStack, EntityPlayer)} from your Slot
 * implementation.</p>
 */
public class ItemInventory implements SimpleInventory, NameableInventory {

    static final String NBT_UUID_KEY = "_sc$iteminv$uuid";

    final UUID uuid = UUID.randomUUID();

    /**
     * <p>The ItemStack that contains this inventory.</p>
     */
    protected final ItemStack stack;
    /**
     * <p>The NBT key used to store the inventory data.</p>
     */
    protected final String nbtKey;

    /**
     * <p>An array of ItemStacks to store the inventory.</p>
     */
    protected final ItemStack[] storage;

    /**
     * <p>This constructor uses the given NBT key to store the data.</p>
     * <p>This constructor calls {@link #getSizeInventory()} to determine the size of the inventory. It needs to be overridden and work properly when called from this constructor.</p>
     *
     * @param stack  the ItemStack to save to
     * @param nbtKey the NBT key to use
     */
    protected ItemInventory(ItemStack stack, String nbtKey) {
        this.storage = new ItemStack[getSizeInventory()];
        this.stack = stack;
        this.nbtKey = nbtKey;
        writeUUID();
        readFromNBT(getNbt());
    }

    /**
     * <p>This constructor uses the given NBT key to store the data.</p>
     *
     * @param size   the size of this inventory
     * @param stack  the ItemStack to save to
     * @param nbtKey the NBT key to use
     */
    protected ItemInventory(int size, ItemStack stack, String nbtKey) {
        this.storage = new ItemStack[size];
        this.stack = stack;
        this.nbtKey = nbtKey;
        writeUUID();
        readFromNBT(getNbt());
    }

    /**
     * <p>This constructor uses the NBT key <tt>&lt;ModID&gt;:&lt;ItemName&gt;.inv</tt> to store the data.</p>
     * <p>This constructor calls {@link #getSizeInventory()} to determine the size of the inventory. It needs to be
     * overridden and work properly when called from this constructor.</p>
     *
     * @param stack the ItemStack to save to
     */
    protected ItemInventory(ItemStack stack) {
        this(stack, defaultNBTKey(stack));
    }

    /**
     * <p>This constructor uses the NBT key <tt>&lt;ModID&gt;:&lt;ItemName&gt;.inv</tt> to store the data.</p>
     *
     * @param size  the size of this inventory
     * @param stack the ItemStack to save to
     */
    protected ItemInventory(int size, ItemStack stack) {
        this(size, stack, defaultNBTKey(stack));
    }

    private void writeUUID() {
        ItemStacks.getNbt(stack).setTag(nbtKey, NBTData.writeUUID(uuid));
    }

    private static String defaultNBTKey(ItemStack stack) {
        return Item.itemRegistry.getNameForObject(stack.getItem()) + ".inv";
    }

    @Override
    public int getSizeInventory() {
        return storage.length;
    }

    @Override
    public void setSlotNoMark(int slot, @Nullable ItemStack stack) {
        storage[slot] = stack;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return storage[slot];
    }

    @Override
    public void markDirty() {
        saveData();
    }

    @Override
    public void closeInventory() {
        if (stack.stackTagCompound != null) {
            stack.stackTagCompound.removeTag(NBT_UUID_KEY);
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        // usually the ItemStack is in the player's inventory
        return true;
    }

    /**
     * saves this inventory to the ItemStack.
     */
    protected final void saveData() {
        writeToNBT(getNbt());
    }

    private NBTTagCompound getNbt() {
        return ItemStacks.getNbt(stack, nbtKey);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        SimpleInventory.super.readFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        SimpleInventory.super.writeToNBT(nbt);
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }

    @Override
    public void setCustomName(String name) {
        stack.setStackDisplayName(name);
    }

    @Override
    public String getCustomName() {
        return stack.getDisplayName();
    }

    @Override
    public String getDefaultName() {
        return null;
    }

    /**
     * <p>Check if the given ItemStack can be picked up by the given player inside the given Container.
     * This method checks if the ItemStack is in use by any ItemInventory in the given Container and returns false if that
     * is the case.</p>
     * <p>If you use {@link SimpleSlot} you do not need this method.</p>
     *
     * @param container the Container
     * @param stack     the ItemStack
     * @param player    the player
     * @return true if the stack can be picked up
     */
    public static boolean canTakeStack(Container container, @Nullable ItemStack stack, EntityPlayer player) {
        if (stack == null) {
            return true;
        }

        UUID uuid = NBTData.readUUID(ItemStacks.getNbt(stack, NBT_UUID_KEY));
        if (uuid == null) {
            return true;
        }

        ImmutableSet<IInventory> inventories = Containers.getInventories(container);
        for (IInventory inventory : inventories) {
            if (inventory instanceof ItemInventory && uuid.equals(((ItemInventory) inventory).uuid)) {
                return false;
            }
        }

        return true;
    }
}
