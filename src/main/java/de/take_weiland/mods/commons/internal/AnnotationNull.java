package de.take_weiland.mods.commons.internal;

import de.take_weiland.mods.commons.nbt.NBTSerializer;
import de.take_weiland.mods.commons.net.PacketTarget;
import de.take_weiland.mods.commons.sync.TypeSyncer;

/**
 * Dummy class used as default value for various Annotations
 * @author diesieben07
 */
public interface AnnotationNull extends NBTSerializer<Object>, TypeSyncer<Object>, PacketTarget {
}