package de.take_weiland.mods.commons.templates;

import de.take_weiland.mods.commons.Listenable;
import net.minecraft.inventory.IInventory;

public interface SCInventory<T extends SCInventory<T>> extends IInventory, Listenable<T> {


}
