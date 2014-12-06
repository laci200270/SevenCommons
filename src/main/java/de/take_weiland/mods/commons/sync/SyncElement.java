package de.take_weiland.mods.commons.sync;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.AnnotatedElement;

/**
 * @author diesieben07
 */
public interface SyncElement<T> extends AnnotatedElement {

	TypeToken<T> getType();

}
