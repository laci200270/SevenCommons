package de.take_weiland.mods.commons.templates;

interface InternalBlockCommons<C extends InternalBlockCommons<C>> {

	boolean teFeaturesEnabled();
	
	void setTeFeaturesEnabled(boolean value);
	
}