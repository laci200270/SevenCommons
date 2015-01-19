package de.take_weiland.mods.commons.internal;

import de.take_weiland.mods.commons.serialize.ByteStreamSerializer;
import de.take_weiland.mods.commons.serialize.NBTSerializer;
import de.take_weiland.mods.commons.sync.Watcher;

/**
 * Dummy class used as default value for various Annotations
 *
 * @author diesieben07
 */
public interface AnnotationNull extends NBTSerializer<Object>, Watcher<Object>, ByteStreamSerializer<Object> {
}
