package de.take_weiland.mods.commons.internal.sync;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import de.take_weiland.mods.commons.internal.prop.AbstractProperty;
import de.take_weiland.mods.commons.reflect.Property;
import de.take_weiland.mods.commons.reflect.PropertyAccess;
import de.take_weiland.mods.commons.reflect.SCReflection;
import de.take_weiland.mods.commons.sync.Syncer;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.IExtendedEntityProperties;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;
import static org.objectweb.asm.commons.GeneratorAdapter.NE;
import static org.objectweb.asm.commons.Method.getMethod;

/**
 * @author diesieben07
 */
public final class BytecodeEmittingCompanionGenerator {

    public static final int PRV_STC_FNL = ACC_PRIVATE | ACC_STATIC | ACC_FINAL;
    private static final String SYNCER = "syn";
    private static final String COMPANION = "com";
    private static final String PROP_ACC = "prp";
    private static final String COMP_ACC = "cop";

    private final DefaultCompanionFactory factory;
    private final Class<?> clazz;

    private String className;
    private String superName;
    private Class<?> superClass;
    private ClassWriter cw;
    private final Map<Property<?, ?>, Syncer<?, ?, ?>> properties;
    private int firstID;

    BytecodeEmittingCompanionGenerator(DefaultCompanionFactory factory, Class<?> clazz, Map<Property<?, ?>, Syncer<?, ?, ?>> properties) {
        this.factory = factory;
        this.clazz = clazz;
        this.properties = properties;
    }

    Class<?> generateCompanion() {
        beginClass();

        makeFields();
        makeCLInit();
        makeApplyChanges();

        makeCheck();

        return finish();
    }

    private void beginClass() {
        className = SCReflection.nextDynamicClassName(BytecodeEmittingCompanionGenerator.class.getPackage());

        superClass = findAppropriateSuperClass();
        superName = Type.getInternalName(superClass);
        firstID = factory.getNextFreeIDFor(clazz);

        cw = new ClassWriter(COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC, className, null, superName, null);

        Method cstr = getMethod("void <init>()");
        GeneratorAdapter gen = new GeneratorAdapter(ACC_PUBLIC, cstr, null, null, cw);
        gen.visitCode();
        gen.loadThis();
        gen.invokeConstructor(Type.getObjectType(superName), cstr);
        gen.returnValue();
        gen.endMethod();
    }

    private Class<?> findAppropriateSuperClass() {
        Class<?> superClassCompanion = factory.getCompanionClass(clazz.getSuperclass());
        if (superClassCompanion == null) {
            return isIEEP() ? IEEPSyncCompanion.class : SyncCompanion.class;
        } else {
            return superClassCompanion;
        }
    }

    private boolean isIEEP() {
        return IExtendedEntityProperties.class.isAssignableFrom(clazz);
    }

    private boolean hasDefaultSuper() {
        return isIEEP() ? superClass == IEEPSyncCompanion.class : superClass == SyncCompanion.class;
    }

    private void makeFields() {
        for (Map.Entry<Property<?, ?>, Syncer<?, ?, ?>> entry : properties.entrySet()) {
            Property<?, ?> property = entry.getKey();
            Syncer<?, ?, ?> syncer = entry.getValue();

            String descSyncer = Type.getDescriptor(Syncer.class);
            String descPropAcc = Type.getDescriptor(PropertyAccess.class);

            cw.visitField(PRV_STC_FNL, getPropertyID(property, SYNCER), descSyncer, null, null);
            cw.visitField(PRV_STC_FNL, getPropertyID(property, PROP_ACC), descPropAcc, null, null);

            Class<?> companionType = syncer.companionType();
            if (companionType != null) {
                cw.visitField(PRV_STC_FNL, getPropertyID(property, COMP_ACC), descPropAcc, null, null);

                cw.visitField(ACC_PUBLIC, getPropertyID(property, COMPANION), Type.getDescriptor(companionType), null, null);
            }
        }
    }

    private void makeApplyChanges() {
        Method method = getMethod("int applyChanges(Object, de.take_weiland.mods.commons.internal.sync.SyncCompanion$ChangeIterator)");
        GeneratorAdapter gen = new GeneratorAdapter(ACC_PUBLIC, method, null, null, cw);
        gen.visitCode();

        final Type myType = Type.getObjectType(className);
        final Type superType = Type.getType(superClass);
        final Type changeItType = Type.getType(SyncCompanion.ChangeIterator.class);
        final Type syncerType = Type.getType(Syncer.class);
        final Type objectType = Type.getType(Object.class);
        final Type propertyAccessType = Type.getType(PropertyAccess.class);

        final int inStreamArg = 1;
        final int fieldID = gen.newLocal(Type.INT_TYPE);

        if (needCallSuper()) {
            gen.loadThis();
            gen.loadArg(0);
            gen.loadArg(1);
            gen.invokeConstructor(superType, method);
            gen.storeLocal(fieldID);
        } else {
            gen.loadArg(1);
            gen.invokeInterface(changeItType, getMethod("int fieldId()"));
            gen.storeLocal(fieldID);
        }

        Label start = gen.mark();

        int keyCount = properties.size();
        int[] keys = new int[keyCount];
        for (int i = 0; i < keyCount; i++) {
            keys[i] = i + firstID;
        }

        gen.loadLocal(fieldID);
        gen.tableSwitch(keys, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                Map.Entry<Property<?, ?>, Syncer<?, ?, ?>> entry = Iterables.get(properties.entrySet(), key - firstID);
                Property<?, ?> property = entry.getKey();
                Syncer<?, ?, ?> syncer = entry.getValue();

                gen.loadArg(1);
                gen.getStatic(myType, getPropertyID(property, SYNCER), syncerType);
                gen.loadArg(0);
                gen.getStatic(myType, getPropertyID(property, PROP_ACC), propertyAccessType);
                gen.loadThis();
                gen.getStatic(myType, getPropertyID(property, COMP_ACC), propertyAccessType);
                gen.invokeInterface(changeItType, new Method("apply", VOID_TYPE,
                        new Type[]{syncerType, objectType, propertyAccessType, objectType, propertyAccessType}));
                gen.goTo(end);
            }

            @Override
            public void generateDefault() {
                gen.loadLocal(fieldID); // unknown ID, either return control back to super caller or end of stream
                gen.returnValue();
            }
        });

        gen.loadArg(1);
        gen.invokeInterface(changeItType, getMethod("int fieldId()"));
        gen.storeLocal(fieldID);
        gen.goTo(start);

        gen.endMethod();
    }

    private void makeCheck() {
        Method method = getMethod("de.take_weiland.mods.commons.internal.sync.SyncEvent check(Object, boolean)");
        GeneratorAdapter gen = new GeneratorAdapter(ACC_PUBLIC, method, null, null, cw);
        gen.visitCode();

        Type myType = Type.getObjectType(className);
        Type superType = Type.getType(superClass);
        Type objectType = Type.getType(Object.class);
        Type syncEventType = Type.getType(SyncEvent.class);
        Type syncEventSubclass;
        if (TileEntity.class.isAssignableFrom(clazz)) {
            syncEventSubclass = Type.getType(SyncEvent.ForTE.class);
        } else if (Entity.class.isAssignableFrom(clazz)) {
            syncEventSubclass = Type.getType(SyncEvent.ForEntity.class);
        } else if (Container.class.isAssignableFrom(clazz)) {
            syncEventSubclass = Type.getType(SyncEvent.ForContainer.class);
        } else if (IExtendedEntityProperties.class.isAssignableFrom(clazz)) {
            syncEventSubclass = Type.getType(SyncEvent.ForIEEP.class);
        } else {
            throw new IllegalStateException("@Sync in invalid class " + clazz.getName());
        }
        Type syncerType = Type.getType(Syncer.class);
        Type syncerChangeType = Type.getType(Syncer.Change.class);
        Type changedValueType = Type.getType(ChangedValue.class);
        Type propertyAccessType = Type.getType(PropertyAccess.class);

        int eventSlot = gen.newLocal(syncEventType);
        int changeSlot = gen.newLocal(changedValueType);

        if (hasDefaultSuper()) {
            gen.push((String) null);
            gen.storeLocal(eventSlot);
        } else {
            gen.loadThis();
            gen.loadArg(0);
            gen.push(true);
            gen.invokeConstructor(superType, method);
            gen.storeLocal(eventSlot);
        }

        int fieldIndex = 0;
        Label next = null;

        for (Property<?, ?> property : properties.keySet()) {
            if (next != null) {
                gen.mark(next);
            }
            next = new Label();

            gen.getStatic(myType, getPropertyID(property, SYNCER), syncerType);
            gen.loadArg(0);
            gen.getStatic(myType, getPropertyID(property, PROP_ACC), propertyAccessType);
            gen.loadThis();
            gen.getStatic(myType, getPropertyID(property, COMP_ACC), propertyAccessType);

            gen.invokeInterface(syncerType, new Method("check", syncerChangeType,
                    new Type[]{objectType, propertyAccessType, objectType, propertyAccessType}));
            gen.storeLocal(changeSlot);

            gen.loadLocal(changeSlot);
            gen.ifNull(next);

            Label eventNotNull = new Label();
            gen.loadLocal(eventSlot);
            gen.ifNonNull(eventNotNull);

            gen.newInstance(syncEventSubclass);
            gen.dup();
            gen.loadArg(0);
            gen.invokeConstructor(syncEventSubclass, getMethod("void <init>(Object)"));
            gen.storeLocal(eventSlot);

            gen.mark(eventNotNull);
            gen.loadLocal(eventSlot);
            gen.push(firstID + fieldIndex);
            gen.loadLocal(changeSlot);
            gen.invokeVirtual(syncEventType, new Method("add", VOID_TYPE, new Type[]{INT_TYPE, changedValueType}));

            fieldIndex++;
        }

        if (next != null) {
            gen.mark(next);
        }

        Label end = new Label();
        gen.loadArg(1);
        gen.ifZCmp(NE, end);
        gen.loadLocal(eventSlot);
        gen.ifNull(end);

        gen.loadLocal(eventSlot);
        gen.loadArg(0);
        gen.invokeVirtual(syncEventType, getMethod("void send(Object)"));

        gen.mark(end);
        gen.loadLocal(eventSlot);
        gen.returnValue();
        gen.endMethod();
    }

    private boolean needCallSuper() {
        return superClass != SyncCompanion.class && superClass != IEEPSyncCompanion.class;
    }

    private void makeCLInit() {
        GeneratorAdapter gen = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, getMethod("void <clinit>()"), null, null, cw);
        gen.visitCode();

        Type myType = getObjectType(className);
        Type iteratorType = Type.getType(Iterator.class);
        Type objectArrType = Type.getType(Object[].class);
        Type objectType = Type.getType(Object.class);
        Type syncerType = Type.getType(Syncer.class);
        Type propertyAccessType = Type.getType(PropertyAccess.class);

        int iterator = gen.newLocal(iteratorType);

        gen.push(myType);
        gen.invokeStatic(Type.getType(BytecodeEmittingCompanionGenerator.class), getMethod("java.util.Iterator getStaticData(Class)"));
        gen.storeLocal(iterator);

        for (Property<?, ?> property : properties.keySet()) {
            gen.loadLocal(iterator);
            gen.invokeInterface(iteratorType, getMethod("Object next()"));
            gen.checkCast(objectArrType);

            gen.dup();
            gen.push(0);
            gen.arrayLoad(objectType);
            gen.checkCast(syncerType);
            gen.putStatic(myType, getPropertyID(property, SYNCER), syncerType);

            gen.dup();
            gen.push(1);
            gen.arrayLoad(objectType);
            gen.checkCast(propertyAccessType);
            gen.putStatic(myType, getPropertyID(property, PROP_ACC), propertyAccessType);

            gen.push(2);
            gen.arrayLoad(objectType);
            gen.checkCast(propertyAccessType);
            gen.putStatic(myType, getPropertyID(property, COMP_ACC), propertyAccessType);

        }

        gen.returnValue();
        gen.endMethod();
    }

    private static String getPropertyID(Property<?, ?> property, String role) {
        return getPropertyID(property) + "$" + role;
    }

    private static String getPropertyID(Property<?, ?> property) {
        return property.getName() + (property.getMember() instanceof Field ? "$f" : "$m");
    }

    private static Map<Property<?, ?>, Syncer<?, ?, ?>> staticProperties;

    private Class<?> finish() {
        Class<?> cls;

        // gross hack, please close your eyes
        synchronized (BytecodeEmittingCompanionGenerator.class) {
            // <clinit> of generated class calls getStaticData

            staticProperties = properties;
            try {
                cw.visitEnd();
                cls = SCReflection.defineDynamicClass(cw.toByteArray());
            } finally {
                staticProperties = null;
            }
        }

        return cls;
    }

    // called from <clinit> in generated classes, see #finish()
    @SuppressWarnings("unused")
    static Iterator<Object[]> getStaticData(Class<?> generatedClass) throws NoSuchFieldException, IllegalAccessException {
        return FluentIterable.from(staticProperties.entrySet())
                .transform(entry -> {
                    Property<?, ?> property = entry.getKey();
                    Syncer<?, ?, ?> syncer = entry.getValue();

                    PropertyAccess<?> propertyAccess = property.optimize();
                    PropertyAccess<?> compAccess;
                    if (syncer.companionType() != null) {
                        try {
                            Field compField = generatedClass.getDeclaredField(getPropertyID(property, COMPANION));
                            compAccess = AbstractProperty.newProperty(compField).optimize();
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        compAccess = PropertyAccess.EMPTY;
                    }

                    return new Object[]{
                            syncer,
                            propertyAccess,
                            compAccess
                    };
                })
                .iterator();
    }

}
