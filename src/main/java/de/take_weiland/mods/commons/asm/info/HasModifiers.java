package de.take_weiland.mods.commons.asm.info;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

/**
 * <p>Something that has modifiers like a method, field or class.</p>
 *
 * @author diesieben07
 */
public abstract class HasModifiers {

    /**
     * <p>Get all Java modifiers present on this element.</p>
     *
     * @return the modifiers
     * @see java.lang.reflect.Modifier
     */
    public abstract int modifiers();

    /**
     * <p>Determine if the given Java language modifier is set on this element.</p>
     *
     * @param mod the modifier to check
     * @return true if the given modifier is set
     * @see java.lang.reflect.Modifier
     */
    public boolean hasModifier(int mod) {
        return (modifiers() & mod) == mod;
    }

    /**
     * <p>Determine if this element has public visibility.</p>
     *
     * @return true if this element has public visibility
     */
    public boolean isPublic() {
        return hasModifier(ACC_PUBLIC);
    }

    /**
     * <p>Determine if this element has protected visibility.</p>
     *
     * @return true if this element has protected visibility
     */
    public boolean isProtected() {
        return hasModifier(ACC_PROTECTED);
    }

    /**
     * <p>Determine if this element has private visibility.</p>
     *
     * @return true if this element has private visibility
     */
    public boolean isPrivate() {
        return hasModifier(ACC_PRIVATE);
    }

    /**
     * <p>Determine if this element has package-private (default) visibility.</p>
     *
     * @return true if this element has package-private (default) visibility
     */
    public boolean isPackagePrivate() {
        return !isPrivate() && !isPublic() && !isProtected();
    }

    /**
     * <p>Determine if this element is final.</p>
     *
     * @return true if this element is final
     */
    public boolean isFinal() {
        return hasModifier(ACC_FINAL);
    }

    /**
     * <p>Determine if this element is a synthetic element generated by the compiler.</p>
     *
     * @return true if this element is synthetic
     */
    public boolean isSynthetic() {
        return hasModifier(ACC_SYNTHETIC);
    }

}
