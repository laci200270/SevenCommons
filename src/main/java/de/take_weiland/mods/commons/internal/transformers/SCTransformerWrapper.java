package de.take_weiland.mods.commons.internal.transformers;

import de.take_weiland.mods.commons.asm.ASMClassTransformerWrapper;

/**
 * @author diesieben07
 */
public class SCTransformerWrapper extends ASMClassTransformerWrapper {

	@Override
	protected void setup() {
		// vanilla stuff
		register(new GuiScreenTransformer());
		register(new EntityZombieTransformer());
		register(new EntityPlayerTransformer());
		register(new EntityTransformer());
		register(new ItemStackTransformer());
		register(new EntityTrackerEntryTransformer());

		// packets
		register(new SimplePacketTypeTransformer());
		register(new PacketTransformer());

		// @Synced & @ToNbt
		register(new AnnotationFindingTransformer());

		register(new HasSimpleMetaTransformer());
		register(new SimpleMetadataTransformer());

		register(new ListenableTransformer());
	}

}
