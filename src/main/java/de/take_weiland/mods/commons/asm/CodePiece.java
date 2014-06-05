package de.take_weiland.mods.commons.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * <p>Represents a piece of Bytecode.</p>
 * @see de.take_weiland.mods.commons.asm.CodePieces The CodePieces class for working with CodePieces
 *
 * @author diesieben07
 */
public interface CodePiece {

	/**
	 * <p>Create an InsnList that contains a copy of the instructions in this CodePiece.</p>
	 * @return a new InsnList
	 */
	InsnList build();

	/**
	 * <p>Appends the instructions in this CodePiece to the given InsnList.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 */
	void appendTo(InsnList to);

	/**
	 * <p>Prepends the instructions in this CodePiece to the given InsnList.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 */
	void prependTo(InsnList to);

	/**
	 * <p>Lets the given MethodVisitor visit all instructions in this CodePiece.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param mv the MethodVisitor to append to
	 */
	void appendTo(MethodVisitor mv);

	/**
	 * <p>Inserts the instructions in this CodePiece after the given location, which must be part of the InsnList.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 * @param location the position where to insert the code, must be part of the InsnList
	 */
	void insertAfter(InsnList to, AbstractInsnNode location);

	/**
	 * <p>Inserts the instructions in this CodePiece before the given location, which must be part of the InsnList.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 * @param location the position where to insert the code, must be part of the InsnList
	 */
	void insertBefore(InsnList to, AbstractInsnNode location);

	/**
	 * <p>Inserts the instructions in this CodePiece after the given location.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 * @param location the position where to insert the code, must be part of the InsnList
	 */
	void insertAfter(CodeLocation location);

	/**
	 * <p>Inserts the instructions in this CodePiece before the given location.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 * @param location the position where to insert the code, must be part of the InsnList
	 */
	void insertBefore(CodeLocation location);

	/**
	 * <p>Replaces the given CodeLocation with the instructions in this CodePiece, making the location invalid.</p>
	 * <p>This method must only be used to build the "final" list of instructions for e.g. a method.
	 * Intermediate operations (e.g. concatenating various CodePieces) must use {@link #append(CodePiece)}, etc.</p>
	 * @param to the list to append to
	 * @param location the position where to insert the code, must be part of the InsnList
	 */
	void replace(CodeLocation location);

	/**
	 * <p>Append the given instruction to this CodePiece.</p>
	 * <p>The instruction must not be used in any InsnList.</p>
	 * @param node the instruction to append
	 * @return this, for convenience
	 */
	CodePiece append(AbstractInsnNode node);

	/**
	 * <p>Append the given InsnList to this CodePiece.</p>
	 * <p>The list must not be elsewhere.</p>
	 * @param node the instruction to append
	 * @return this, for convenience
	 */
	CodePiece append(InsnList insns);

	CodePiece append(CodePiece other);

	CodePiece prepend(CodePiece other);

	CodePiece prepend(AbstractInsnNode node);

	CodePiece prepend(InsnList insns);

	int size();

}
