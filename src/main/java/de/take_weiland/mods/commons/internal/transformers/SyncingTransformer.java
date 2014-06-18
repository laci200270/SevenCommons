package de.take_weiland.mods.commons.internal.transformers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cpw.mods.fml.common.FMLLog;
import de.take_weiland.mods.commons.asm.*;
import de.take_weiland.mods.commons.internal.SyncASMHooks;
import de.take_weiland.mods.commons.internal.SyncType;
import de.take_weiland.mods.commons.internal.SyncedEntityProperties;
import de.take_weiland.mods.commons.internal.SyncedObject;
import de.take_weiland.mods.commons.net.*;
import de.take_weiland.mods.commons.sync.Sync;
import de.take_weiland.mods.commons.sync.TypeSyncer;
import de.take_weiland.mods.commons.util.ItemStacks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IExtendedEntityProperties;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static de.take_weiland.mods.commons.asm.MCPNames.CLASS_ENTITY;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * contains black bytecode magic. Do not touch.
 */
public final class SyncingTransformer implements ASMClassTransformer {

	private static final Logger LOGGER;
	private static final ClassInfo extPropsCI = ClassInfo.of(IExtendedEntityProperties.class);
	private static final ClassInfo entityCI = ClassInfo.of("net/minecraft/entity/Entity");
	private static final ClassInfo tileEntityCI = ClassInfo.of("net/minecraft/tileentity/TileEntity");
	private static final ClassInfo containerCI = ClassInfo.of("net/minecraft/inventory/Container");


	static {
		FMLLog.makeLog("SevenCommonsSync");
		LOGGER = Logger.getLogger("SevenCommonsSync");
	}

	@Override
	public boolean transforms(String internalName) {
		return !internalName.startsWith("net/minecraft/")
				&& !internalName.startsWith("net/minecraftforge/")
				&& !internalName.startsWith("cpw/mods/fml/")
				&& !internalName.startsWith("org/apache/");
	}

	@Override
	public boolean transform(ClassNode clazz, ClassInfo classInfo) {
		if (classInfo.isInterface() || classInfo.isEnum()) {
			return false;
		}

		if (!ASMUtils.hasAnnotationOnAnything(clazz, Sync.class)) {
			return false;
		}

		SyncType type = null;
		if (entityCI.isAssignableFrom(classInfo)) {
			type = SyncType.ENTITY;
		} else if (tileEntityCI.isAssignableFrom(classInfo)) {
			type = SyncType.TILE_ENTITY;
		} else if (containerCI.isAssignableFrom(classInfo)) {
			type = SyncType.CONTAINER;
		}

		if (extPropsCI.isAssignableFrom(classInfo)) {
			if (type == null) {
				type = SyncType.ENTITY_PROPS;
			} else {
				throw new IllegalStateException("Cannot sync on IExtendedEntityProperties class which extends Entity, TileEntity or Container!");
			}
		}

		if (type == null) {
			throw new IllegalStateException(String.format("Can't sync class %s", clazz.name));
		}

		List<ASMVariable> variables = ASMVariables.allWith(clazz, Sync.class, CodePieces.getThis());
		List<SyncedElement> elements = Lists.newArrayListWithCapacity(variables.size());

		Map<Type, Syncer> knownSyncers = Maps.newHashMap();

		for (ASMVariable var : variables) {
			AnnotationNode syncedAnnotation = var.getterAnnotation(Sync.class);
			Type syncerType = ASMUtils.getAnnotationProperty(syncedAnnotation, "syncer");
			Syncer syncer;
			if (syncerType == null) {
				syncer = Syncer.forType(var.getType());
			} else if (!knownSyncers.containsKey(syncerType)) {
				CodePiece syncerInstance = obtainInstance(clazz, syncerType);
				syncer = new CustomSyncer(syncerInstance, var.getType());
				knownSyncers.put(syncerType, syncer);
			} else {
				syncer = knownSyncers.get(syncerType);
			}

			elements.add(new SyncedElement(var, makeCompanion(clazz, var), syncer));
		}

		// checks if any superclass of this class already has @Sync properties
		// and counts them to avoid conflicting IDs
		int superSyncCount = countSuperSyncs(clazz);

		// only need to add the SyncedEntityProperties interface if superclass has not already done it
		if (type == SyncType.ENTITY_PROPS && superSyncCount == 0) {
			addEntityPropertyStuff(clazz);
		}

		MethodNode writeIndexMethod = createWriteIdx(clazz, superSyncCount + variables.size());
		MethodNode readIndexMethod = createReadIdx(clazz, superSyncCount + variables.size());

		// this checks if the virtual context ("this") matches the static context.
		// this is needed so that only the (synced) class furthest down in the hierarchy chain calls the sync method from the tick method
		ASMCondition isActualClass = makeActualClassCheck(clazz);

		// create the method that actually syncs the data
		MethodNode syncMethod = createSyncMethod(clazz, elements, type, superSyncCount, writeIndexMethod);

		// create the method that reads the data back from the packet
		createReadMethod(clazz, superSyncCount, elements, readIndexMethod);

		// call the sync method from the tick method
		makeSyncCall(clazz, syncMethod, type, superSyncCount > 0, isActualClass);

		clazz.interfaces.add(Type.getInternalName(SyncedObject.class));
		return true;
	}

	private static MethodNode createWriteIdx(ClassNode clazz, int elementCount) {
		MethodNode method = new MethodNode(ACC_PROTECTED, "_sc$writeSyncIdx", ASMUtils.getMethodDescriptor(void.class, WritableDataBuf.class, int.class), null, null);
		clazz.methods.add(method);

		Class<?> indexType = selectIndexSize(elementCount);

		String name = "write" + StringUtils.capitalize(indexType.getName());

		CodePieces.invoke(INVOKEINTERFACE, getInternalName(WritableDataBuf.class), name, ASMUtils.getMethodDescriptor(void.class, indexType),
				CodePieces.of(new VarInsnNode(ALOAD, 1)), CodePieces.of(new VarInsnNode(ILOAD, 2)))
			.appendTo(method.instructions);

		method.instructions.add(new InsnNode(RETURN));
		return method;
	}

	private static MethodNode createReadIdx(ClassNode clazz, int elementCount) {
		MethodNode method = new MethodNode(ACC_PROTECTED, "_sc$readSyncIdx", ASMUtils.getMethodDescriptor(int.class, DataBuf.class), null, null);
		clazz.methods.add(method);

		Class<?> indexType = selectIndexSize(elementCount);

		String name = "read" + StringUtils.capitalize(indexType.getName());

		CodePieces.invoke(INVOKEINTERFACE, getInternalName(DataBuf.class), name, ASMUtils.getMethodDescriptor(indexType),
				CodePieces.of(new VarInsnNode(ALOAD, 1))).appendTo(method.instructions);

		method.instructions.add(new InsnNode(IRETURN));

		return method;
	}

	private static Class<?> selectIndexSize(int elementCount) {
		if (elementCount <= Byte.MAX_VALUE) {
			return byte.class;
		} else if (elementCount <= Short.MAX_VALUE) {
			return short.class;
		} else { // most likely never gonna happen :D
			throw new IllegalStateException("Cannot sync more than Short.MAX_VALUE elements in one class hierarchy!");
		}
	}

	private static ASMCondition makeActualClassCheck(ClassNode clazz) {
		// cannot use getClass because that might return a non-synced class if there is a non-synced class in the hierarchy chain
		// in that case we want to get first synced class while walking up the chain
		MethodNode method = new MethodNode(ACC_PROTECTED, "_sc$syncClass", getMethodDescriptor(getType(Class.class)), null, null);
		clazz.methods.add(method);

		CodePieces.constant(Type.getObjectType(clazz.name)).appendTo(method.instructions);
		method.instructions.add(new InsnNode(ARETURN));

		return Conditions.ifEqual(CodePieces.invoke(clazz, method, CodePieces.getThis()),
					CodePieces.constant(Type.getObjectType(clazz.name)),
					Type.getType(Class.class), false);
	}


	private static void addEntityPropertyStuff(ClassNode clazz) {
		// tick method is handled by makeSyncCall

		Type entityType = getObjectType(CLASS_ENTITY);
		Type stringType = getType(String.class);

		FieldNode ownerField = new FieldNode(ACC_PRIVATE, "_sc$syncPropsOwner", entityType.getDescriptor(), null, null);
		clazz.fields.add(ownerField);

		MethodNode ownerGetter = new MethodNode(ACC_PUBLIC, SyncedEntityProperties.GET_ENTITY, getMethodDescriptor(entityType), null, null);
		clazz.methods.add(ownerGetter);
		CodePieces.getField(clazz, ownerField, CodePieces.getThis()).appendTo(ownerGetter.instructions);
		ownerGetter.instructions.add(new InsnNode(ARETURN));

		FieldNode identField = new FieldNode(ACC_PRIVATE, "_sc$syncPropsIdent", stringType.getDescriptor(), null, null);
		clazz.fields.add(identField);

		MethodNode identGetter = new MethodNode(ACC_PUBLIC, SyncedEntityProperties.GET_IDENTIFIER, getMethodDescriptor(stringType), null, null);
		clazz.methods.add(identGetter);
		CodePieces.getField(clazz, identField, CodePieces.getThis()).appendTo(identGetter.instructions);
		identGetter.instructions.add(new InsnNode(ARETURN));

		MethodNode method = new MethodNode(ACC_PUBLIC, SyncedEntityProperties.INJECT_DATA, getMethodDescriptor(VOID_TYPE, entityType, stringType), null, null);
		clazz.methods.add(method);
		InsnList insns = method.instructions;

		CodePieces.setField(clazz, ownerField, CodePieces.getThis(), CodePieces.of(new VarInsnNode(ALOAD, 1))).appendTo(insns);
		CodePieces.setField(clazz, identField, CodePieces.getThis(), CodePieces.of(new VarInsnNode(ALOAD, 2))).appendTo(insns);

		insns.add(new InsnNode(RETURN));

		clazz.interfaces.add(SyncedEntityProperties.CLASS_NAME);
	}

	private static void makeSyncCall(ClassNode clazz, MethodNode syncMethod, SyncType type, boolean isSuperSynced, ASMCondition isActualClass) {
		String name = type.getTickMethod();
		MethodNode tickMethod = ASMUtils.findMethod(clazz, name);
		if (tickMethod == null) {
			tickMethod = new MethodNode(ACC_PUBLIC, name, getMethodDescriptor(VOID_TYPE), null, null);
			// tick method for ENTITY_PROPS only exist if class is Synced, so only call super tick method, if a superclass is actually synced
			if (type != SyncType.ENTITY_PROPS || isSuperSynced) {
				CodePieces.invokeSuper(clazz, tickMethod).appendTo(tickMethod.instructions);
			}
			tickMethod.instructions.add(new InsnNode(RETURN));
			clazz.methods.add(tickMethod);
		}
		isActualClass.then(CodePieces.invoke(clazz, syncMethod, CodePieces.getThis(), CodePieces.constantNull(), CodePieces.constant(true))
							.append(CodePieces.ofOpcode(POP)))
				.build()
				.prependTo(tickMethod.instructions);
	}

	private static int countSuperSyncs(ClassNode clazz) {
		int count = 0;
		while (clazz != null && !"java/lang/Object".equals(clazz.superName)) {
			clazz = ASMUtils.getThinClassNode(clazz.superName);
			for (MethodNode method : clazz.methods) {
				if (ASMUtils.hasAnnotation(method, Sync.class)) ++count;
			}
			for (FieldNode field : clazz.fields) {
				if (ASMUtils.hasAnnotation(field, Sync.class)) ++count;
			}
		}
		return count;
	}

	private static void createReadMethod(ClassNode clazz, int superSyncCount, List<SyncedElement> elements, MethodNode readIndexMethod) {
		MethodNode method = new MethodNode(ACC_PUBLIC, SyncedObject.READ, ASMUtils.getMethodDescriptor(int.class, DataBuf.class), null, null);
		clazz.methods.add(method);
		InsnList insns = method.instructions;

		LabelNode methodStart = new LabelNode();
		LabelNode methodEnd = new LabelNode();

		insns.add(methodStart);

		final int _this = 0;
		final int buf = 1;
		final int idx = 2;
		final int first = 3;
		method.localVariables.add(new LocalVariableNode("this", getObjectType(clazz.name).getDescriptor(), null, methodStart, methodEnd, _this));
		method.localVariables.add(new LocalVariableNode("buf", getDescriptor(DataBuf.class), null, methodStart, methodEnd, buf));
		method.localVariables.add(new LocalVariableNode("idx", Type.INT_TYPE.getDescriptor(), null, methodStart, methodEnd, idx));
		if (superSyncCount > 0) {
			method.localVariables.add(new LocalVariableNode("first", Type.BOOLEAN_TYPE.getDescriptor(), null, methodStart, methodEnd, first));
			CodePieces.constant(true).appendTo(insns);
			insns.add(new VarInsnNode(ISTORE, first));
		}

		CodeBuilder builder = new CodeBuilder();

		if (superSyncCount > 0) {
			builder.add(Conditions.ifTrue(CodePieces.of(new VarInsnNode(ILOAD, first)))
					.then(CodePieces.invokeSuper(clazz, method, CodePieces.of(new VarInsnNode(ALOAD, buf)))
							.append(new VarInsnNode(ISTORE, idx))
							.append(CodePieces.constant(false).append(new VarInsnNode(ISTORE, first))))
					.otherwise(CodePieces.invoke(clazz, readIndexMethod,
							CodePieces.getThis(),
							CodePieces.of(new VarInsnNode(ALOAD, buf)))
							.append(CodePieces.of(new VarInsnNode(ISTORE, idx))))
					.build());
		} else {
			builder.add(CodePieces.invoke(clazz, readIndexMethod,
					CodePieces.getThis(),
					CodePieces.of(new VarInsnNode(ALOAD, buf))));

			builder.add(new VarInsnNode(ISTORE, idx));
		}

		SwitchBuilder sb = new SwitchBuilder();

		for (int i = 0, len = elements.size(); i < len; ++i) {
			SyncedElement element = elements.get(i);

			sb.add(i + superSyncCount, element.variable.set(
					element.syncer.read(element.variable.get(), CodePieces.of(new VarInsnNode(ALOAD, buf)))));

			if (!ASMUtils.isPrimitive(element.variable.getType())) {
				sb.add(-(i + superSyncCount + 1), element.variable.set(CodePieces.constantNull()));
			}
		}

		sb.add(Integer.MAX_VALUE, CodePieces.constant(Integer.MAX_VALUE).append(new InsnNode(IRETURN)));
		sb._default(CodePieces.of(new VarInsnNode(ILOAD, idx)).append(new InsnNode(IRETURN)));

		builder.add(sb.build(CodePieces.of(new VarInsnNode(ILOAD, idx))));

		CodePiece availableBytes = CodePieces.invoke(INVOKEINTERFACE, getInternalName(DataBuf.class),
				"available", getMethodDescriptor(INT_TYPE), CodePieces.of(new VarInsnNode(ALOAD, buf)));

		Conditions.ifEqual(availableBytes, CodePieces.constant(0), Type.INT_TYPE)
				.negate()
				.makeDoWhile(builder.build())
				.appendTo(insns);

		CodePieces.constant(Integer.MAX_VALUE)
				.appendTo(insns);
		insns.add(new InsnNode(IRETURN));

		insns.add(methodEnd);
	}

	private static MethodNode createSyncMethod(ClassNode clazz, List<SyncedElement> elements, SyncType type, int superSyncCount, MethodNode writeIndexMethod) {
		Type packetBuilderType = getType(PacketBuilder.class);
		MethodNode method = new MethodNode(ACC_PROTECTED, "_sc$doSync", Type.getMethodDescriptor(packetBuilderType, packetBuilderType, Type.BOOLEAN_TYPE), null, null);
		clazz.methods.add(method);
		InsnList insns = method.instructions;

		LabelNode start = new LabelNode();
		LabelNode end = new LabelNode();

		final int _this = 0;
		final int packetBuilder = 1;
		final int isRootCall = 2;
		method.localVariables.add(new LocalVariableNode("this", Type.getObjectType(clazz.name).getDescriptor(), null, start, end, _this));
		method.localVariables.add(new LocalVariableNode("packetBuilder", packetBuilderType.getDescriptor(), null, start, end, packetBuilder));
		method.localVariables.add(new LocalVariableNode("isRootCall", Type.BOOLEAN_TYPE.getDescriptor(), null, start, end, isRootCall));

		if (superSyncCount > 0) {
			CodePieces.invokeSuper(clazz, method, CodePieces.of(new VarInsnNode(ALOAD, packetBuilder)), CodePieces.constant(false))
					.append(new VarInsnNode(ASTORE, packetBuilder))
					.appendTo(insns);
		}

		CodePiece createBuilder = CodePieces.invokeStatic(SyncASMHooks.CLASS_NAME, SyncASMHooks.CREATE_BUILDER,
				ASMUtils.getMethodDescriptor(PacketBuilder.class, Object.class, SyncType.class),
				CodePieces.getThis(), CodePieces.constant(type))
				.append(CodePieces.of(new VarInsnNode(ASTORE, packetBuilder)));

		CodePiece packetBuilderDirect = CodePieces.of(new VarInsnNode(ALOAD, packetBuilder));
		CodePiece checkBuilder = Conditions.ifNull(packetBuilderDirect)
				.then(createBuilder)
				.build();

		for (int i = 0, len = elements.size(); i < len; i++) {
			SyncedElement element = elements.get(i);
			int index = i + superSyncCount;

			CodePiece syncNonNull = CodePieces.invoke(clazz, writeIndexMethod,
					CodePieces.getThis(), packetBuilderDirect, CodePieces.constant(index))
					.append(element.syncer.write(element.variable.get(), packetBuilderDirect))
					.append(element.companion.set(element.variable.get()));

			CodePiece syncingCode;
			if (!ASMUtils.isPrimitive(element.variable.getType())) {
				syncingCode = element.syncer.isNull(element.variable.get())
						.then(CodePieces.invoke(clazz, writeIndexMethod,
								CodePieces.getThis(), packetBuilderDirect, CodePieces.constant(-(index + 1))))
						.otherwise(syncNonNull)
						.build();
			} else {
				syncingCode = syncNonNull;
			}

			element.syncer.equals(element.companion.get(), element.variable.get())
					.otherwise(checkBuilder.append(syncingCode))
					.build().appendTo(insns);

		}

		Conditions.ifNull(packetBuilderDirect)
				.otherwise(Conditions.ifTrue(CodePieces.of(new VarInsnNode(ILOAD, isRootCall)))
						.then(CodePieces.invokeStatic(SyncASMHooks.CLASS_NAME, SyncASMHooks.SEND_FINISHED,
										ASMUtils.getMethodDescriptor(void.class, Object.class, SyncType.class, PacketBuilder.class),
										CodePieces.getThis(), CodePieces.constant(type), packetBuilderDirect))
						.build())
				.build()
				.appendTo(insns);

		packetBuilderDirect.appendTo(insns);
		insns.add(new InsnNode(ARETURN));

		return method;
	}

	private static CodePiece obtainInstance(ClassNode clazz, Type packetTargetType) {
		ClassNode targetClazz = ASMUtils.getThinClassNode(packetTargetType.getInternalName());

		boolean canBeStatic = false;

		CodePiece instanceLoader = CodePieces.obtainInstance(clazz, targetClazz,
				new Type[]{ Type.getObjectType(clazz.name) },
				CodePieces.getThis());

		if (instanceLoader == null) {
			instanceLoader = CodePieces.obtainInstance(clazz, targetClazz,
					new Type[] { Type.getType(Object.class) },
					CodePieces.getThis());

			if (instanceLoader == null) {
				instanceLoader = CodePieces.obtainInstance(clazz, targetClazz);
				canBeStatic = true;
				if (instanceLoader == null) {
					throw new IllegalStateException(String.format("Failed to obtain instance of %s for @Synced class %s", packetTargetType.getInternalName(), clazz.name));
				}
			}
		}

		return CodePieces.cache(clazz, packetTargetType, instanceLoader, canBeStatic, false);
	}

	private static ASMVariable makeCompanion(ClassNode clazz, ASMVariable var) {
		FieldNode field = new FieldNode(ACC_PRIVATE, var.name() + "_sc$syncCompanion", var.getType().getDescriptor(), null, null);
		clazz.fields.add(field);
		return ASMVariables.of(clazz, field, CodePieces.getThis());
	}

	private static class SyncedElement {

		final ASMVariable variable;
		final ASMVariable companion;
		final Syncer syncer;

		SyncedElement(ASMVariable variable, ASMVariable companion, Syncer syncer) {
			this.variable = variable;
			this.companion = companion;
			this.syncer = syncer;
		}
	}

	private static abstract class Syncer {

		static final Map<Type, Type> boxedTypes = ImmutableMap.<Type, Type>builder()
			.put(getType(Boolean.class), Type.BOOLEAN_TYPE)
			.put(getType(Byte.class), Type.BYTE_TYPE)
			.put(getType(Short.class), Type.SHORT_TYPE)
			.put(getType(Integer.class), Type.INT_TYPE)
			.put(getType(Long.class), Type.LONG_TYPE)
			.put(getType(Character.class), Type.CHAR_TYPE)
			.put(getType(Float.class), Type.FLOAT_TYPE)
			.put(getType(Double.class), Type.DOUBLE_TYPE)
			.build();

		private static final Set<Type> integratedTypes = ImmutableSet.of(
			Type.getType(String.class), Type.getType(UUID.class)
		);

		static Syncer forType(Type t) {
			if (ASMUtils.isPrimitive(t) || integratedTypes.contains(t)) {
				return new IntegratedSyncer(t);
			} else if (boxedTypes.containsKey(t)) {
				return new BoxedSyncer(t);
			} else if (t.getInternalName().equals("net/minecraft/item/ItemStack")) {
				return ItemStackSyncer.instance();
			} else if (ClassInfo.of(t).isEnum()) {
				return new EnumSyncer(t);
			} else {
				throw new UnsupportedOperationException("NYI");
			}
		}

		abstract ASMCondition equals(CodePiece oldValue, CodePiece newValue);

		/**
		 * writes the value to the PacketBuilder
		 */
		abstract CodePiece write(CodePiece newValue, CodePiece packetBuilder);

		ASMCondition isNull(CodePiece value) {
			return Conditions.ifNull(value);
		}

		CodePiece wrapIndex(CodePiece value, int index) {
			return Conditions.ifNull(value)
					.then(CodePieces.constant(-index))
					.otherwise(CodePieces.constant(index))
					.build();
		}

		abstract CodePiece read(CodePiece oldValue, CodePiece packetBuilder);

	}

	private static class BoxedSyncer extends Syncer {

		private final Type type;

		BoxedSyncer(Type type) {
			this.type = type;
		}

		@Override
		ASMCondition equals(CodePiece oldValue, CodePiece newValue) {
			return Conditions.ifEqual(oldValue, newValue, type, true);
		}

		@Override
		CodePiece write(CodePiece newValue, CodePiece packetBuilder) {
			Type unboxed = boxedTypes.get(type);
			String owner = SyncASMHooks.CLASS_NAME;
			String name = SyncASMHooks.WRITE_INTEGRATED;
			String desc = Type.getMethodDescriptor(Type.VOID_TYPE, unboxed, Type.getType(WritableDataBuf.class));

			String unboxName = unboxed.getClassName() + "Value";
			String unboxDesc = getMethodDescriptor(unboxed);

			return CodePieces.invokeStatic(owner, name, desc,
					CodePieces.invoke(INVOKEVIRTUAL, type.getInternalName(), unboxName, unboxDesc, newValue),
					packetBuilder);
		}

		@Override
		CodePiece read(CodePiece oldValue, CodePiece packetBuilder) {
			Type unboxed = boxedTypes.get(type);

			String owner = SyncASMHooks.CLASS_NAME;
			String name = String.format(SyncASMHooks.READ_INTEGRATED, unboxed.getClassName().replace('.', '_'));
			String desc = Type.getMethodDescriptor(unboxed, Type.getType(DataBuf.class));

			String boxDesc = getMethodDescriptor(type, unboxed);

			return CodePieces.invokeStatic(type.getInternalName(), "valueOf", boxDesc,
						CodePieces.invokeStatic(owner, name, desc, packetBuilder));
		}
	}

	private static class ItemStackSyncer extends Syncer {

		private static ItemStackSyncer instance;

		static ItemStackSyncer instance() {
			return instance == null ? (instance = new ItemStackSyncer()) : instance;
		}

		@Override
		ASMCondition equals(CodePiece oldValue, CodePiece newValue) {
			return Conditions.ifTrue(CodePieces.invokeStatic(Type.getInternalName(ItemStacks.class),
					"equal", ASMUtils.getMethodDescriptor(boolean.class, ItemStack.class, ItemStack.class),
					oldValue, newValue));
		}

		@Override
		CodePiece write(CodePiece newValue, CodePiece packetBuilder) {
			return CodePieces.invokeStatic(Type.getInternalName(DataBuffers.class),
					"writeItemStack", ASMUtils.getMethodDescriptor(void.class, WritableDataBuf.class, ItemStack.class),
					packetBuilder, newValue);
		}

		@Override
		CodePiece read(CodePiece oldValue, CodePiece packetBuilder) {
			return CodePieces.invokeStatic(Type.getInternalName(DataBuffers.class),
					"readItemStack", ASMUtils.getMethodDescriptor(ItemStack.class, DataBuf.class),
					packetBuilder);
		}
	}

	private static class EnumSyncer extends Syncer {

		private final Type type;

		EnumSyncer(Type type) {
			this.type = type;
		}

		@Override
		ASMCondition equals(CodePiece oldValue, CodePiece newValue) {
			return Conditions.ifEqual(oldValue, newValue, type, false);
		}

		@Override
		CodePiece write(CodePiece newValue, CodePiece packetBuilder) {
			return CodePieces.invokeStatic(Type.getInternalName(DataBuffers.class),
					"writeEnum",
					ASMUtils.getMethodDescriptor(void.class, WritableDataBuf.class, Enum.class),
					packetBuilder, newValue);
		}

		@Override
		CodePiece read(CodePiece oldValue, CodePiece dataBuf) {
			return CodePieces.castTo(type, CodePieces.invokeStatic(Type.getInternalName(DataBuffers.class),
					"readEnum",
					ASMUtils.getMethodDescriptor(Enum.class, DataBuf.class, Class.class),
					dataBuf, CodePieces.constant(type)));
		}
	}

	private static class IntegratedSyncer extends Syncer {

		final Type typeToSync;

		IntegratedSyncer(Type typeToSync) {
			this.typeToSync = typeToSync;
		}

		@Override
		ASMCondition isNull(CodePiece value) {
			return ASMUtils.isPrimitive(typeToSync) ? Conditions.alwaysFalse() : super.isNull(value);
		}

		@Override
		CodePiece wrapIndex(CodePiece value, int index) {
			return CodePieces.constant(index);
		}

		@Override
		CodePiece write(CodePiece newValue, CodePiece packetBuilder) {
			String owner = SyncASMHooks.CLASS_NAME;
			String name = SyncASMHooks.WRITE_INTEGRATED;
			String desc = Type.getMethodDescriptor(Type.VOID_TYPE, typeToSync, Type.getType(WritableDataBuf.class));

			return CodePieces.invokeStatic(owner, name, desc, newValue, packetBuilder);
		}

		@Override
		ASMCondition equals(CodePiece oldValue, CodePiece newValue) {
			return Conditions.ifEqual(oldValue, newValue, typeToSync, true);
		}

		@Override
		CodePiece read(CodePiece oldValue, CodePiece packetBuilder) {
			String owner = SyncASMHooks.CLASS_NAME;
			String name = String.format(SyncASMHooks.READ_INTEGRATED, typeToSync.getClassName().replace('.', '_'));
			String desc = Type.getMethodDescriptor(typeToSync, Type.getType(DataBuf.class));

			return CodePieces.invokeStatic(owner, name, desc, packetBuilder);
		}
	}

	private static class CustomSyncer extends Syncer {

		private final CodePiece syncer;
		private final Type actualType;

		CustomSyncer(CodePiece syncer, Type actualType) {
			this.syncer = syncer;
			this.actualType = actualType;
		}

		@Override
		ASMCondition equals(CodePiece oldValue, CodePiece newValue) {
			String owner = TypeSyncer.CLASS_NAME;
			String name = TypeSyncer.METHOD_EQUAL;
			Type objectType = Type.getType(Object.class);
			String desc = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, objectType, objectType);

			CodePiece invoke = CodePieces.invoke(INVOKEINTERFACE, owner, name, desc, syncer, newValue, oldValue);

			return Conditions.of(invoke, IFNE, IFEQ);
		}

		@Override
		CodePiece write(CodePiece newValue, CodePiece packetBuilder) {
			String owner = TypeSyncer.CLASS_NAME;
			String name = TypeSyncer.METHOD_WRITE;
			String desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.getType(WritableDataBuf.class));

			return CodePieces.invoke(INVOKEINTERFACE, owner, name, desc, syncer, newValue, packetBuilder);
		}

		@Override
		CodePiece read(CodePiece oldValue, CodePiece packetBuilder) {
			String owner = TypeSyncer.CLASS_NAME;
			String name = TypeSyncer.METHOD_READ;
			Type objectType = Type.getType(Object.class);
			String desc = Type.getMethodDescriptor(objectType, objectType, Type.getType(DataBuf.class));

			CodePiece invoke = CodePieces.invoke(INVOKEINTERFACE, owner, name, desc, syncer, oldValue, packetBuilder);
			if (!ASMUtils.isPrimitive(actualType) || !actualType.equals(objectType)) {
				return CodePieces.castTo(actualType, invoke);
			} else {
				return invoke;
			}
		}
	}

}
