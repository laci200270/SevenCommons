package de.take_weiland.mods.commons.internal.transformers.tonbt;

import de.take_weiland.mods.commons.asm.ASMVariable;
import de.take_weiland.mods.commons.asm.CodePiece;
import de.take_weiland.mods.commons.asm.CodePieces;
import de.take_weiland.mods.commons.nbt.NBTData;
import net.minecraft.nbt.NBTBase;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author diesieben07
 */
final class EnumHandler extends ToNBTHandler {

    EnumHandler(ASMVariable var) {
        super(var);
    }

    @Override
    CodePiece makeNBT(MethodNode writeMethod) {
        return CodePieces.invokeStatic(NBTData.class, "writeEnum", NBTBase.class,
                Enum.class, var.get());
    }

    @Override
    CodePiece consumeNBT(CodePiece nbt, MethodNode readMethod) {
        CodePiece rawEnum = CodePieces.invokeStatic(NBTData.class, "readEnum", Enum.class,
                NBTBase.class, nbt,
                Class.class, CodePieces.constant(var.getType()));
        return var.set(CodePieces.castTo(var.getType(), rawEnum));
    }

}