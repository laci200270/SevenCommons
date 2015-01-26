package de.take_weiland.mods.commons.internal.transformers.tonbt;

import de.take_weiland.mods.commons.asm.ASMVariable;
import de.take_weiland.mods.commons.asm.CodePiece;
import de.take_weiland.mods.commons.asm.CodePieces;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidTank;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.POP;

/**
 * @author diesieben07
 */
class FluidTankHandler extends ToNBTHandler {

    FluidTankHandler(ASMVariable var) {
        super(var);
    }

    @Override
    CodePiece makeNBT(MethodNode writeMethod) {
        return CodePieces.invokeVirtual(FluidTank.class, "writeToNBT", var.get(), NBTTagCompound.class,
                NBTTagCompound.class, CodePieces.instantiate(NBTTagCompound.class));
    }

    @Override
    CodePiece consumeNBT(CodePiece nbt, MethodNode readMethod) {
        return CodePieces.invokeVirtual(FluidTank.class, "readFromNBT", var.get(), FluidTank.class,
                NBTTagCompound.class, CodePieces.castTo(NBTTagCompound.class, nbt))
                .append(new InsnNode(POP));
    }
}