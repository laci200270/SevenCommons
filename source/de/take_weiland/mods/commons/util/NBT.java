package de.take_weiland.mods.commons.util;

import java.util.List;

import com.google.common.base.Function;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public final class NBT {

	private NBT() { }

	/**
	 * view the given NBTTagList as a {@link List}<br>
	 * the type parameter T can be used if you are sure that this list only contains NBT-Tags of the given type
	 * @param nbtList the list to view
	 * @return a modifiable list view of the NBTTagList
	 */
	public static final <T extends NBTBase> List<T> asList(NBTTagList nbtList) {
		return ((NBTListProxy)nbtList).getWrappedList();
	}

	public static final NBTTagCompound getOrCreateCompound(NBTTagCompound parent, String key) {
		if (!parent.hasKey(key)) {
			parent.setCompoundTag(key, new NBTTagCompound());
		}
		return parent.getCompoundTag(key);
	}
	
	public static NBTTagList getOrCreateList(NBTTagCompound parent, String key) {
		if (!parent.hasKey(key)) {
			parent.setTag(key, new NBTTagList());
		}
		return parent.getTagList(key);
	}
	
	private static final Function<NBTTagString, String> GET_STRING_FUNC = new Function<NBTTagString, String>() {

		@Override
		public String apply(NBTTagString input) {
			return input.data;
		}
		
	};

	public static Function<NBTTagString, String> getStringFunction() {
		return GET_STRING_FUNC;
	}
	
}
