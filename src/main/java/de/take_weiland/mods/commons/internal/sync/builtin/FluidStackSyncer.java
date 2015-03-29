package de.take_weiland.mods.commons.internal.sync.builtin;

import de.take_weiland.mods.commons.net.MCDataInput;
import de.take_weiland.mods.commons.net.MCDataOutput;
import de.take_weiland.mods.commons.sync.SimpleSyncer;
import de.take_weiland.mods.commons.util.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author diesieben07
 */
final class FluidStackSyncer implements SimpleSyncer<FluidStack, FluidStack> {
    @Override
    public Class<FluidStack> getValueType() {
        return FluidStack.class;
    }

    @Override
    public Class<FluidStack> getCompanionType() {
        return FluidStack.class;
    }

    @Override
    public boolean equal(FluidStack value, FluidStack companion) {
        return Fluids.identical(value, companion);
    }

    @Override
    public FluidStack writeAndUpdate(FluidStack value, FluidStack companion, MCDataOutput out) {
        out.writeFluidStack(value);
        return Fluids.clone(value);
    }

    @Override
    public FluidStack read(FluidStack oldValue, FluidStack companion, MCDataInput in) {
        return in.readFluidStack();
    }
}
