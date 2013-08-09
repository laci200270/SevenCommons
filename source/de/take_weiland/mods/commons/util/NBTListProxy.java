package de.take_weiland.mods.commons.util;

import java.util.List;

import net.minecraft.nbt.NBTBase;
import de.take_weiland.mods.commons.asm.proxy.Getter;
import de.take_weiland.mods.commons.asm.proxy.TargetClass;

@TargetClass("net.minecraft.nbt.NBTTagList")
public interface NBTListProxy {

	@Getter(mcpName = "tagList", obfName = "a")
	public <T extends NBTBase> List<T> getWrappedList();
	
}