package de.take_weiland.mods.commons.internal.updater;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import de.take_weiland.mods.commons.net.WritableDataBuf;

import java.util.Collection;
import java.util.List;

/**
 * @author diesieben07
 */
public interface ModVersion {
	Ordering<ModVersion> MOD_VERSION_ORDERING = Ordering.natural().onResultOf(new Function<ModVersion, ArtifactVersion>() {
		@Override
		public ArtifactVersion apply(ModVersion input) {
			return input.getModVersion();
		}
	}).reverse();

	boolean isUseable();

	boolean isInstalled();

	boolean canBeInstalled();

	ArtifactVersion getModVersion();

	String getVersionString();

	String getDownloadURL();

	String getPatchNotes();

	List<Dependency> getDependencies();

	Collection<String> getDependencyDisplay();

	void write(WritableDataBuf buf);
}
