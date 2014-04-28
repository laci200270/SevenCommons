package de.take_weiland.mods.commons.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static com.google.common.base.Preconditions.checkArgument;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.VOID;

/**
 * @author diesieben07
 */
class FieldAccessWrapped extends AbstractFieldAccess {

	private final ClassNode clazz;
	private final MethodNode getter;
	private final MethodNode setter;

	FieldAccessWrapped(ClassNode clazz, MethodNode getter, MethodNode setter) {
		checkArgument((getter.access & ACC_STATIC) == (setter.access & ACC_STATIC), "setter and getter must have the same static-state");

		Type valueType = Type.getReturnType(getter.desc);
		checkArgument(valueType.getSort() != VOID, "getter must not return void!");

		Type[] setterArgs = Type.getArgumentTypes(setter.desc);
		checkArgument(setterArgs.length == 1, "setter must only take one argument");
		checkArgument(Type.getReturnType(setter.desc).getSort() == VOID, "setter must return void");
		checkArgument(setterArgs[0].equals(valueType), "setter takes wrong argument!");
		this.clazz = clazz;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	CodePiece makeGet() {
		InsnList insns = new InsnList();
		int invokeOp;
		if ((getter.access & ACC_STATIC) != ACC_STATIC) {
			insns.add(new VarInsnNode(ALOAD, 0));
			invokeOp = (setter.access & ACC_PRIVATE) == ACC_PRIVATE ? INVOKESPECIAL : INVOKEVIRTUAL;
		} else {
			invokeOp = INVOKESTATIC;
		}
		insns.add(new MethodInsnNode(invokeOp, clazz.name, getter.name, getter.desc));
		return ASMUtils.asCodePiece(insns);
	}

	@Override
	public CodePiece setValue(CodePiece loadValue) {
		if (!isWritable()) {
			throw new UnsupportedOperationException();
		}
		InsnList insns = new InsnList();
		int invokeOp;
		if ((setter.access & ACC_STATIC) != ACC_STATIC) {
			insns.add(new VarInsnNode(ALOAD, 0));
			invokeOp = (setter.access & ACC_PRIVATE) == ACC_PRIVATE ? INVOKESPECIAL : INVOKEVIRTUAL;
		} else {
			invokeOp = PUTSTATIC;
		}
		loadValue.appendTo(insns);
		insns.add(new MethodInsnNode(invokeOp, clazz.name, setter.name, setter.desc));
		return ASMUtils.asCodePiece(insns);
	}

	@Override
	public boolean isWritable() {
		return setter != null;
	}
}
