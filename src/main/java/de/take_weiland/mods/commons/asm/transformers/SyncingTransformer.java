package de.take_weiland.mods.commons.asm.transformers;

import static de.take_weiland.mods.commons.asm.ASMUtils.hasAnnotation;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.List;
import java.util.logging.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.FMLLog;
import de.take_weiland.mods.commons.asm.ASMUtils;
import de.take_weiland.mods.commons.asm.SelectiveTransformer;
import de.take_weiland.mods.commons.internal.sync.SyncType;
import de.take_weiland.mods.commons.internal.sync.SyncedObject;
import de.take_weiland.mods.commons.internal.sync.Syncer;
import de.take_weiland.mods.commons.network.ModPacket;

public class SyncingTransformer extends SelectiveTransformer {

	private static final Type SYNCED_TYPE_TYPE = getType(SyncedObject.class);
	private static final String PERFORM_SYNC = "performSync";
	private static final String PERFORM_SYNC_DESC = getMethodDescriptor(VOID_TYPE, SYNCED_TYPE_TYPE);
	private static final String SYNCER_WRITE = "write";
	private static final Type DATA_OUTPUT = Type.getType(DataOutput.class);
	private static final Type DATA_INPUT = getType(DataInput.class);
	private static final String SYNC_METHOD = "_sc_sync";
	private static final String SYNCER_CLASS = Type.getInternalName(Syncer.class);
	private static final String WRITE_IDX = "writeIdx";
	private static final String WRITE_IDX_DESC = Type.getMethodDescriptor(VOID_TYPE, INT_TYPE, DATA_OUTPUT);
	private static final String READ_IDX = "readIdx";
	private static final String READ_IDX_DESC = getMethodDescriptor(INT_TYPE, DATA_INPUT);
	
	private static final String WRITE_TERMINATION = "writeTermination";
	private static final String WRITE_TERMINATION_DESC = Type.getMethodDescriptor(VOID_TYPE, DATA_OUTPUT);
	
//	private static final String WRITE = "write";
	
	private static final String IS_DIRTY = "_SC_SYNC_isDirty";
	private static final String IS_DIRTY_DESC = Type.getMethodDescriptor(BOOLEAN_TYPE);
	
	private static final String WRITE = "_SC_SYNC_write";
	private static final String WRITE_DESC = getMethodDescriptor(VOID_TYPE, DATA_OUTPUT);
	
	private static final String READ = "_SC_SYNC_read";
	private static final String READ_DESC = getMethodDescriptor(VOID_TYPE, DATA_INPUT);
	
	private static final Type OBJECT_TYPE = getType(Object.class);
	private static final Type CLASS_TYPE = getType(Class.class);
	private static final Type SYNCED_ANN = getObjectType("de/take_weiland/mods/commons/sync/Synced");
	
	private static final String SYNCER_EQUAL = "equal";
	
	public static final Logger LOGGER;
	
	static {
		FMLLog.makeLog("SevenCommonsSync");
		LOGGER = Logger.getLogger("SevenCommonsSync");
	}
	
	@Override
	protected boolean transforms(String className) {
		return !className.startsWith("net.minecraft.");
	}

	@Override
	protected boolean transform(ClassNode clazz, String className) {
		if (!hasAnnotation(clazz, SYNCED_ANN) || (clazz.access & ACC_INTERFACE) == ACC_INTERFACE) {
			return false;
		}
		
		Class<?> superClass = null;
		try {
			superClass = getClass().getClassLoader().loadClass(ASMUtils.undoInternalName(clazz.superName));
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		List<FieldNode> syncedFields = Lists.newArrayList();
		List<FieldNode> companions = Lists.newArrayList();
		for (FieldNode field : ImmutableList.copyOf(clazz.fields)) { // copy the list because we add to it
			if (hasAnnotation(field, SYNCED_ANN)) {
				if (!canSync(field)) {
					LOGGER.warning(String.format("Field %s in Class %s cannot be synced!", field.name, className));
				} else {
					syncedFields.add(field);
					companions.add(createCompanion(clazz, field));
				}
			}
		}
		
		if (syncedFields.isEmpty()) {
			LOGGER.warning(String.format("Class %s is @Synced but has no @Synced fields!", className));
		} else {
			SyncType type = null;
			if (Entity.class.isAssignableFrom(superClass)) {
				type = SyncType.ENTITY;
			} else if (TileEntity.class.isAssignableFrom(superClass)) {
				type = SyncType.TILE_ENTITY;
			} else if (Container.class.isAssignableFrom(superClass)) {
				type = SyncType.CONTAINER;
			} else {
				LOGGER.warning("Cannot sync class " + clazz.name + "!");
				return false;
			}
			
			addIsDirty(clazz, syncedFields, companions);
			addWrite(clazz, syncedFields, companions);
			addRead(clazz, syncedFields);
			
			InsnList insns = createPerformSyncCall(clazz, type);
			String name;
			switch (type) {
			case ENTITY:
				name = ASMUtils.useMcpNames() ? "onUpdate" : "func_70071_h_";
				addOrCreateMethod(clazz, name, getMethodDescriptor(VOID_TYPE), insns);
				break;
			case TILE_ENTITY:
				name = ASMUtils.useMcpNames() ? "updateEntity" : "func_70316_g";
				addOrCreateMethod(clazz, name, getMethodDescriptor(VOID_TYPE), insns);
				break;
			case CONTAINER:
				name = ASMUtils.useMcpNames() ? "detectAndSendChanges" : "func_75142_b";
				addOrCreateMethod(clazz, name, getMethodDescriptor(VOID_TYPE), insns);
				break;
			}
			
			clazz.interfaces.add(getType(SyncedObject.class).getInternalName());
			LOGGER.info(String.format("Made class %s @Synced", className));
		}
		
		return true;
	}
	
	private void addOrCreateMethod(ClassNode clazz, String name, String desc, InsnList insns) {
		MethodNode method = null;
		for (MethodNode m : clazz.methods) {
			if (m.name.equals(name) && m.desc.equals(desc)) {
				method = m;
				break;
			}
		}
		if (method == null) {
			method = new MethodNode(ACC_PUBLIC, name, desc, null, null);
			insns.insert(new MethodInsnNode(INVOKESPECIAL, clazz.superName, name, desc));
			insns.insert(new VarInsnNode(ALOAD, 0)); // reverse order because of insert!
			insns.add(new InsnNode(RETURN));
			method.instructions = insns;
			clazz.methods.add(method);
		} else {
			method.instructions.insert(insns);
		}
	}
	
	private InsnList createPerformSyncCall(ClassNode clazz, SyncType type) {
		InsnList insns = new InsnList();
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new FieldInsnNode(GETSTATIC, SYNCED_TYPE_TYPE.getInternalName(), type.name(), SYNCED_TYPE_TYPE.getDescriptor()));
		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, PERFORM_SYNC, PERFORM_SYNC_DESC));
		return insns;
	}
	
	private void addIsDirty(ClassNode clazz, List<FieldNode> fields, List<FieldNode> companions) {
		MethodNode method = new MethodNode(ACC_PUBLIC, IS_DIRTY, IS_DIRTY_DESC, null, null);
		InsnList insns = method.instructions;
		
		int len = fields.size();
		for (int i = 0; i < len; ++i) {
			FieldNode field = fields.get(i);
			FieldNode companion = companions.get(i);
			
			addGetField(clazz, insns, field);
			addGetField(clazz, insns, companion);
			
			addCheckEqual(insns, field);
			
			LabelNode afterReturn = new LabelNode();
			insns.add(new JumpInsnNode(IFNE, afterReturn));
			
			insns.add(new InsnNode(ICONST_1));
			insns.add(new InsnNode(IRETURN));
			
			insns.add(afterReturn);
		}
		insns.add(new InsnNode(ICONST_0));
		insns.add(new InsnNode(IRETURN));
		
		clazz.methods.add(method);
		
		// code is basically (except some special cases for primitives)
		// public boolean _SC_SYNC_isDirty() {
		//     if (!Syncer.equal(this.field0, this.field0_SC_SYNC)) {
		//         return true;
		//     }
		//     ... // for every field
		//     return false;
		// }
	}
	
	private void addWrite(ClassNode clazz, List<FieldNode> fields, List<FieldNode> companions) {
		MethodNode method = new MethodNode(ACC_PUBLIC, WRITE, WRITE_DESC, null, null);
		InsnList insns = method.instructions;
		
		int len = fields.size();
		for (int i = 0; i < len; ++i) {
			FieldNode field = fields.get(i);
			FieldNode companion = companions.get(i);
			
			addGetField(clazz, insns, field);
			addGetField(clazz, insns, companion);
			
			addCheckEqual(insns, field);
			
			LabelNode afterSync = new LabelNode();
			insns.add(new JumpInsnNode(IFNE, afterSync));
			
			insns.add(new IntInsnNode(BIPUSH, i));
			insns.add(new VarInsnNode(ALOAD, 1));
			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, WRITE_IDX, WRITE_IDX_DESC));
			
			insns.add(new VarInsnNode(ALOAD, 0));
			addGetField(clazz, insns, field);
			insns.add(new InsnNode(DUP));
			addWrite(insns, field);
			
			insns.add(new FieldInsnNode(PUTFIELD, clazz.name, companion.name, companion.desc));
			
			insns.add(afterSync);
		}
		
		insns.add(new InsnNode(ICONST_M1));
		insns.add(new VarInsnNode(ALOAD, 1));
		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, WRITE_IDX, WRITE_IDX_DESC));
		
		insns.add(new InsnNode(RETURN));
		
		clazz.methods.add(method);
		
		// code is basically (except some special cases for primitives)
		// public void _SC_SYNC_write(DataOutput out) {
		//     if (Syncer.equal(this.field0, this.field0_SC_SYNC)) {
		//         Syncer.write(this.field0, out);
		//         this.field0_SC_SYNC = this.field0;
		//     }
		//     ... // for every field
		// }
	}
	
	private void addRead(ClassNode clazz, List<FieldNode> fields) {
		MethodNode method = new MethodNode(ACC_PUBLIC, READ, READ_DESC, null, null);
		InsnList insns = method.instructions;
		
		LabelNode readNext = new LabelNode();
		LabelNode last = new LabelNode();
		
		insns.add(readNext);
		insns.add(new VarInsnNode(ALOAD, 1));
		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, READ_IDX, READ_IDX_DESC));
		insns.add(new InsnNode(DUP));
		insns.add(new InsnNode(ICONST_M1));
		insns.add(new JumpInsnNode(IF_ICMPEQ, last));
		
		int len = fields.size();
		LabelNode[] labels = new LabelNode[len];
		for (int i = 0; i < len; ++i) {
			labels[i] = new LabelNode();
		}
		LabelNode dflt = new LabelNode();
		
		insns.add(new TableSwitchInsnNode(0, len - 1, dflt, labels));
		
		for (int i = 0; i < len; ++i) {
			LabelNode label = labels[i];
			FieldNode field = fields.get(i);
			
			insns.add(label);
			
			insns.add(new VarInsnNode(ALOAD, 0));
			insns.add(new VarInsnNode(ALOAD, 1));
			
			Type fieldType = getType(field.desc);
			if (ASMUtils.isPrimitive(fieldType)) {
				insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, "read_" + fieldType.getClassName(), getMethodDescriptor(fieldType, DATA_INPUT)));
			} else {
				insns.add(new LdcInsnNode(fieldType));
				insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, "read", getMethodDescriptor(OBJECT_TYPE, CLASS_TYPE, DATA_INPUT)));
				insns.add(new TypeInsnNode(CHECKCAST, fieldType.getDescriptor()));
			}
			insns.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
			insns.add(new JumpInsnNode(GOTO, readNext));
		}
		
		insns.add(dflt);
		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, "throwErr", getMethodDescriptor(VOID_TYPE)));
		
		insns.add(last);
		insns.add(new InsnNode(RETURN));
		
		clazz.methods.add(method);
	}

	private void addGetField(ClassNode clazz, InsnList insns, FieldNode field) {
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
	}
	
	private void addGetClass(InsnList insns) {
		insns.add(new InsnNode(DUP));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, OBJECT_TYPE.getInternalName(), "getClass", getMethodDescriptor(CLASS_TYPE)));
	}

	private void addCheckEqual(InsnList insns, FieldNode field) {
		Type fieldType = getType(field.desc);
		if (ASMUtils.isPrimitive(fieldType)) {
			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, SYNCER_EQUAL, getMethodDescriptor(BOOLEAN_TYPE, fieldType, fieldType)));
		} else {
			addGetClass(insns);
			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, SYNCER_EQUAL, getMethodDescriptor(BOOLEAN_TYPE, OBJECT_TYPE, OBJECT_TYPE)));
		}
	}
	
	private void addWrite(InsnList insns, FieldNode field) {
		Type fieldType = getType(field.desc);
		boolean isPrimitive = ASMUtils.isPrimitive(fieldType);
		
		if (!isPrimitive) {
			addGetClass(insns);
		}
		
		insns.add(new VarInsnNode(ALOAD, 1)); // the data output
		
		if (isPrimitive) {
			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, SYNCER_WRITE, getMethodDescriptor(VOID_TYPE, fieldType, DATA_OUTPUT)));
		} else {
			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, SYNCER_WRITE, getMethodDescriptor(VOID_TYPE, fieldType, CLASS_TYPE, DATA_OUTPUT)));
		}
	}

//	private void addSyncMethod(ClassNode clazz, List<FieldNode> fields) {
//		String desc = Type.getMethodDescriptor(VOID_TYPE, INT_TYPE, DATA_OUTPUT);
//		String[] exceptions = { Type.getInternalName(IOException.class) };
//		MethodNode method = new MethodNode(ACC_PRIVATE, SYNC_METHOD, desc, null, exceptions);
//		InsnList insns = method.instructions;
//		
//		insns.add(new VarInsnNode(ALOAD, 1)); // load fieldIdx
//		insns.add(new VarInsnNode(ALOAD, 2)); // load DataOutput
//		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, WRITE_IDX, WRITE_IDX_DESC)); // write the field index
//		
//		insns.add(new VarInsnNode(ALOAD, 1)); // load fieldIdx for switch
//		
//		LabelNode dflt = new LabelNode();
//		LabelNode[] labels = new LabelNode[fields.size()];
//		insns.add(new TableSwitchInsnNode(0, fields.size(), dflt, labels));
//		
//		for (int i = 0; i < labels.length; ++i) {
//			FieldNode field = fields.get(i);
//			insns.add(labels[i]); // jump target for i-th field
//			
//			insns.add(new VarInsnNode(ALOAD, 0)); // load this for GETFIELD
//			insns.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
//			
//			insns.add(new VarInsnNode(ALOAD, 2)); // load DataOutput
//			
//			insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, WRITE, Type.getMethodDescriptor(VOID_TYPE, Type.getType(field.desc), DATA_OUTPUT)));
//		}
//		
//		
//		insns.add(new VarInsnNode(ALOAD, 2)); // load DataOutput
//		insns.add(new MethodInsnNode(INVOKESTATIC, SYNCER_CLASS, WRITE_TERMINATION, WRITE_TERMINATION_DESC));
//		
//		clazz.methods.add(method);
//	}
	
	private boolean canSync(FieldNode field) {
		return ASMUtils.isPrimitive(Type.getType(field.desc));
	}
	
	private FieldNode createCompanion(ClassNode clazz, FieldNode field) {
		FieldNode companion = new FieldNode(ACC_PRIVATE, getCompanionName(field), field.desc, null, null);
		clazz.fields.add(companion);
		return companion;
	}
	
	private String getCompanionName(FieldNode field) {
		return field.name + "_SC_SYNC";
	}

}
