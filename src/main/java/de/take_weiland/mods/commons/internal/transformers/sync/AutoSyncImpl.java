package de.take_weiland.mods.commons.internal.transformers.sync;

import de.take_weiland.mods.commons.asm.ASMUtils;
import de.take_weiland.mods.commons.asm.ASMVariable;
import de.take_weiland.mods.commons.asm.CodePiece;
import de.take_weiland.mods.commons.asm.CodePieces;
import de.take_weiland.mods.commons.asm.info.ClassInfo;
import de.take_weiland.mods.commons.internal.ASMHooks;
import de.take_weiland.mods.commons.internal.sync.AutoSyncedObject;
import de.take_weiland.mods.commons.internal.sync.SyncMethod;
import de.take_weiland.mods.commons.net.MCDataOutputStream;
import de.take_weiland.mods.commons.sync.Sync;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author diesieben07
 */
public class AutoSyncImpl extends SyncingTransformerImpl {

	private static final ClassInfo entityCI = ClassInfo.of("net/minecraft/entity/Entity");
	private static final ClassInfo tileEntityCI = ClassInfo.of("net/minecraft/tileentity/TileEntity");
	private static final ClassInfo containerCI = ClassInfo.of("net/minecraft/inventory/Container");
	static final ClassInfo entityPropsCI = ClassInfo.of("net/minecraftforge/common/IExtendedEntityProperties");

	private final SyncMethod method;

	AutoSyncImpl(ClassNode clazz, ClassInfo classInfo) {
		super(Sync.class, clazz, classInfo);

		SyncMethod method0  = null;

		if (entityCI.isAssignableFrom(classInfo)) {
			method0 = SyncMethod.ENTITY;
		} else if (tileEntityCI.isAssignableFrom(classInfo)) {
			method0 = SyncMethod.TILE_ENTITY;
		} else if (containerCI.isAssignableFrom(classInfo)) {
			method0 = SyncMethod.CONTAINER;
		}

		if (method0 == null) {
			if (entityPropsCI.isAssignableFrom(classInfo)) {
				method0 = SyncMethod.ENTITY_PROPS;
			} else {
				throw new IllegalArgumentException(String.format("Cannot @Sync on class %s", clazz.name));
			}
		}

		method = method0;
	}

	@Override
	String uniqueIdentifier() {
		return "autosync";
	}

	@Override
	String readMethodName() {
		return AutoSyncedObject.SYNC_READ;
	}

	@Override
	String writeMethodName() {
		return memberName("write");
	}

	@Override
	boolean isOutStreamLazy() {
		return true;
	}

	@Override
	CodePiece makeStreamLazy(ASMVariable stream) {
		String desc = ASMUtils.getMethodDescriptor(MCDataOutputStream.class, SyncMethod.class, Object.class);
		CodePiece streamCreator = CodePieces.invokeStatic(
				ASMHooks.CLASS_NAME, ASMHooks.NEW_SYNC_STREAM, desc,
				CodePieces.constant(method), CodePieces.getThis());
		return CodePieces.makeLazy(stream, streamCreator);
	}

	@Override
	CodePiece handleWriteFinished(CodePiece stream) {
		String owner = ASMHooks.CLASS_NAME;
		String name = ASMHooks.SEND_SYNC_PACKET;
		String desc = ASMUtils.getMethodDescriptor(void.class, MCDataOutputStream.class, SyncMethod.class, Object.class);
		return CodePieces.invokeStatic(owner, name, desc, stream, CodePieces.constant(method), CodePieces.getThis());
	}

	@Override
	void postTransform() {
		method.postTransform(clazz, superIsSynced);

		clazz.interfaces.add(AutoSyncedObject.CLASS_NAME);

		MethodNode syncClassMethod = addSyncClass();
		CodePiece syncClass = CodePieces.invoke(clazz, syncClassMethod, CodePieces.getThis());
		CodePiece invokeDoSync = CodePieces.invoke(clazz, doSyncMethod,
				CodePieces.getThis(), CodePieces.constant(false), CodePieces.constantNull())
				.append(new InsnNode(POP));

		CodePiece myClass = CodePieces.constant(Type.getObjectType(clazz.name));
		CodePiece checkedDoSync = CodePieces.doIfSame(syncClass, myClass, invokeDoSync, Type.getType(Class.class));

		method.addDoSyncCall(clazz, checkedDoSync);
	}

	private MethodNode addSyncClass() {
		String name = memberName("syncClass");
		String desc = ASMUtils.getMethodDescriptor(Class.class);
		MethodNode method = new MethodNode(ACC_PROTECTED, name, desc, null, null);
		CodePieces.constant(Type.getObjectType(clazz.name)).appendTo(method.instructions);
		method.instructions.add(new InsnNode(ARETURN));

		clazz.methods.add(method);
		return method;
	}
}