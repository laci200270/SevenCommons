package de.take_weiland.mods.commons.internal.prop;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import de.take_weiland.mods.commons.SerializationMethod;
import de.take_weiland.mods.commons.reflect.Property;
import de.take_weiland.mods.commons.reflect.PropertyAccess;
import de.take_weiland.mods.commons.util.JavaUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.invoke.MethodType.methodType;

/**
 * @author diesieben07
 */
public abstract class AbstractProperty<T, MEM extends AccessibleObject & Member & AnnotatedElement> implements Property<T, MEM> {

    public static Property<?, ?> newProperty(AnnotatedElement member) {
        if (member instanceof Field) {
            return new FieldProperty<>((Field) member);
        } else if (member instanceof Method) {
            return new MethodProperty<>((Method) member);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static List<Property<?, ?>> allProperties(Class<?> clazz, Class<? extends Annotation> annotation) {
        return allPropertiesLazy(clazz, annotation).toList();
    }

    public static FluentIterable<Property<?, ?>> allPropertiesLazy(Class<?> clazz, Class<? extends Annotation> annotation) {
        return getPropertySourceClasses(clazz)
                .transformAndConcat(sourceClass -> getDirectProperties(sourceClass, element -> element.isAnnotationPresent(annotation)));
    }

    private static Iterable<Property<?, ?>> getDirectProperties(Class<?> clazz, Predicate<AnnotatedElement> filter) {
        Iterable<AnnotatedElement> methods = asFluent(clazz.getDeclaredMethods());
        Iterable<AnnotatedElement> fields = asFluent(clazz.getDeclaredFields());
        return concat(methods, fields)
                .filter(filter)
                .transform(AbstractProperty::newProperty);
    }

    private static FluentIterable<Class<?>> getPropertySourceClasses(Class<?> clazz) {
        FluentIterable<Class<?>> newIfaces = asFluent(clazz.getInterfaces())
                .filter(input -> !input.isAssignableFrom(clazz.getSuperclass()));

        return concat(Collections.singleton(clazz), newIfaces);
    }

    @SafeVarargs
    private static <T> FluentIterable<T> asFluent(T... arr) {
        return FluentIterable.from(Arrays.asList(arr));
    }

    private static <T> FluentIterable<T> asFluent(Iterable<? extends T> it) {
        //noinspection unchecked
        return FluentIterable.from((Iterable<T>) it);
    }

    private static <T> FluentIterable<T> concat(Iterable<T> a, Iterable<T> b) {
        return asFluent(Iterables.concat(a, b));
    }

    final MEM member;
    private final SerializationMethod.Method desiredMethod;
    private TypeToken<T> genericType;
    private MethodHandle getter;
    private MethodHandle setter;
    private PropertyAccess<T> optimized;

    AbstractProperty(MEM member) {
        this.member = member;
        SerializationMethod annotation = member.getAnnotation(SerializationMethod.class);
        desiredMethod = annotation == null ? SerializationMethod.Method.DEFAULT : annotation.value();
        member.setAccessible(true);
    }

    @Override
    public final MEM getMember() {
        return member;
    }

    @Override
    public final SerializationMethod.Method getDesiredMethod() {
        return desiredMethod;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final TypeToken<T> getType() {
        return genericType == null ? (genericType = (TypeToken<T>) resolveType()) : genericType;
    }

    @Override
    public synchronized PropertyAccess<T> optimize() {
        if (optimized == null) {
            //noinspection unchecked
            optimized = doOptimize();
        }
        return optimized;
    }

    PropertyAccess<T> doOptimize() {
        MethodHandle get = getGetter().asType(methodType(Object.class, Object.class));
        MethodHandle set = getSetter().asType(methodType(void.class, Object.class, Object.class));
        return new PropertyAccess<T>() {
            @Override
            public T get(Object o) {
                try {
                    //noinspection unchecked
                    return (T) get.invokeExact(o);
                } catch (Throwable t) {
                    throw JavaUtils.throwUnchecked(t);
                }
            }

            @Override
            public void set(Object o, T val) {
                try {
                    set.invokeExact(o, val);
                } catch (Throwable t) {
                    throw JavaUtils.throwUnchecked(t);
                }
            }
        };
    }

    @Override
    public final MethodHandle getGetter() {
        if (getter == null) {
            try {
                getter = resolveGetter();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e); // impossible
            }
        }
        return getter;
    }

    @Override
    public MethodHandle getSetter() {
        if (setter == null) {
            try {
                setter = resolveSetter();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e); // impossible
            }
        }
        return setter;
    }

    abstract TypeToken<?> resolveType();

    abstract MethodHandle resolveGetter() throws IllegalAccessException;

    abstract MethodHandle resolveSetter() throws IllegalAccessException;

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return member.getAnnotation(annotationClass);
    }

    @Override
    public final boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return member.isAnnotationPresent(annotation);
    }

    @Override
    public final String toString() {
        return "TypeSpec of type " + getType();
    }

}
