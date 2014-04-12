package de.take_weiland.mods.commons.asm;

import com.google.common.collect.Sets;

import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
* @author diesieben07
*/
abstract class AbstractClassInfo implements ClassInfo {

	private Set<String> supers;

	private void buildSupers(Set<String> collector) {
		if (superName() != null) {
			collector.add(superName());
			collector.addAll(superclass().getSupers());
		}
		for (String iface : interfaces()) {
			if (!collector.add(iface)) {
				collector.addAll(ASMUtils.getClassInfo(iface).getSupers());
			}
		}
	}

	@Override
	public Set<String> getSupers() {
		if (supers == null) {
			supers = Sets.newHashSet();
			buildSupers(supers);
		}
		return supers;
	}

	@Override
	public boolean isAssignableFrom(ClassInfo child) {
		// some cheap tests first
		if (child.internalName().equals("java/lang/Object")) {
			// Object is only assignable to itself
			return internalName().equals("java/lang/Object");
		}
		if (internalName().equals("java/lang/Object") // everything is assignable to Object
				|| child.internalName().equals(internalName()) // we are the same
				|| internalName().equals(child.superName()) // we are the superclass of child
				|| child.interfaces().contains(internalName())) { // we are an interface that child implements
			return true;
		}

		if (!isInterface() && child.isInterface()) {
			return false;
		}
		// need to compute supers now
		return child.getSupers().contains(internalName());
	}

	private ClassInfo zuper;

	@Override
	public ClassInfo superclass() {
		if (zuper != null) {
			return zuper;
		}
		if (superName() == null) {
			return null;
		}
		return (zuper = ASMUtils.getClassInfo(superName()));
	}


	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof ClassInfo && internalName().equals(((ClassInfo) o).internalName());

	}

	@Override
	public int hashCode() {
		return internalName().hashCode();
	}

	@Override
	public boolean isEnum() {
		return hasModifier(ACC_ENUM);
	}

	@Override
	public boolean isAbstract() {
		return hasModifier(ACC_ABSTRACT);
	}

	@Override
	public boolean isInterface() {
		return hasModifier(ACC_INTERFACE);
	}

	private boolean hasModifier(int mod) {
		return (getModifiers() & mod) == mod;
	}
}