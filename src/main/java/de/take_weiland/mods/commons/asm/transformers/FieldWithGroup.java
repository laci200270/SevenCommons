package de.take_weiland.mods.commons.asm.transformers;

import org.objectweb.asm.tree.FieldNode;

final class FieldWithGroup {

	final int group;
	final FieldNode field;
	
	FieldWithGroup(FieldNode field, int group) {
		this.group = group;
		this.field = field;
	}
	
}
