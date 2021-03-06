package de.take_weiland.mods.commons.internal.sync.builtin;

import com.google.common.base.Objects;
import de.take_weiland.mods.commons.sync.Syncer;

/**
 * @author diesieben07
 */
abstract class SyncerDefaultEquals<V> implements Syncer<V, V> {

    private final Class<V> type;

    SyncerDefaultEquals(Class<V> type) {
        this.type = type;
    }

    @Override
    public final Class<V> getCompanionType() {
        return type;
    }

    @Override
    public boolean equal(V value, V companion) {
        return Objects.equal(value, companion);
    }
}
