package de.take_weiland.mods.commons.asm.transformers;

import de.take_weiland.mods.commons.asm.ASMConstants;
import de.take_weiland.mods.commons.asm.ASMUtils;
import de.take_weiland.mods.commons.asm.AppendingTransformer;
import de.take_weiland.mods.commons.internal.SevenCommons;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class EntityPlayerTransformer extends AppendingTransformer {

	@Override
	protected InsnList getAppends(ClassNode clazz, MethodNode method) {
		
		InsnList insns = new InsnList();
		insns.add(new VarInsnNode(Opcodes.ALOAD, 1)); // load this = the new player
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // load the first parameter = the old player
		
		Type entityPlayer = Type.getObjectType(clazz.name);
		
		insns.add(ASMUtils.generateStaticMethodCall(SevenCommons.ASM_HOOK_CLASS, "onPlayerClone", Type.VOID_TYPE, entityPlayer, entityPlayer));
		
		return insns;
	}

	@Override
	protected String getMcpMethod() {
		return ASMConstants.M_CLONE_PLAYER_MCP;
	}

	@Override
	protected String getSrgMethod() {
		return ASMConstants.M_CLONE_PLAYER_SRG;
	}

	@Override
	protected boolean transforms(String className) {
		return className.equals("net.minecraft.entity.player.EntityPlayer");
	}
}
