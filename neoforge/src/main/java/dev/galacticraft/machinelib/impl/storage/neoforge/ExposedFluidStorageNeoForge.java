/*
 * Copyright (c) 2021-2026 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.machinelib.impl.storage.neoforge;

import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges a neutral {@link MachineFluidStorage} to NeoForge's {@link IFluidHandler}, restricted by
 * the given {@link ResourceFlow}.
 *
 * <p>MachineLib stores fluid in droplets (81000/bucket); NeoForge uses millibuckets (1000/bucket),
 * so {@code 1 mB = 81 droplets}. Amounts reported to NeoForge floor to whole mB; sub-mB remainders
 * stay in the neutral store.
 */
public record ExposedFluidStorageNeoForge(@NotNull MachineFluidStorage storage,
                                          @NotNull ResourceFlow flow) implements IFluidHandler {
    private static final long DROPLETS_PER_MB = 81;

    private static @NotNull FluidStack makeStack(@NotNull Fluid fluid, int amount, @NotNull DataComponentPatch components) {
        FluidStack stack = new FluidStack(fluid, amount);
        stack.applyComponents(components);
        return stack;
    }

    private boolean allowsInsertion() {
        return this.flow == ResourceFlow.INPUT || this.flow == ResourceFlow.BOTH;
    }

    private boolean allowsExtraction() {
        return this.flow == ResourceFlow.OUTPUT || this.flow == ResourceFlow.BOTH;
    }

    @Override
    public int getTanks() {
        return this.storage.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        FluidResourceSlot s = this.storage.slot(tank);
        Fluid fluid = s.getResource();
        return fluid == null ? FluidStack.EMPTY : makeStack(fluid, (int) (s.getAmount() / DROPLETS_PER_MB), s.getComponents());
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) Math.min(this.storage.slot(tank).getCapacity() / DROPLETS_PER_MB, Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return this.storage.slot(tank).getFilter().test(stack.getFluid(), stack.getComponentsPatch());
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (!this.allowsInsertion() || resource.isEmpty() || !this.storage.isValid()) return 0;
        long droplets = (long) resource.getAmount() * DROPLETS_PER_MB;
        long total = 0;
        for (int i = 0; i < this.storage.size() && total < droplets; i++) {
            FluidResourceSlot s = this.storage.slot(i);
            if (!s.getFilter().test(resource.getFluid(), resource.getComponentsPatch())) continue;
            long remaining = droplets - total;
            total += action.simulate()
                    ? s.tryInsert(resource.getFluid(), resource.getComponentsPatch(), remaining)
                    : s.insert(resource.getFluid(), resource.getComponentsPatch(), remaining);
        }
        return (int) (total / DROPLETS_PER_MB);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (!this.allowsExtraction() || resource.isEmpty() || !this.storage.isValid()) return FluidStack.EMPTY;
        long droplets = (long) resource.getAmount() * DROPLETS_PER_MB;
        long total = 0;
        for (int i = 0; i < this.storage.size() && total < droplets; i++) {
            FluidResourceSlot s = this.storage.slot(i);
            if (s.getResource() != resource.getFluid() || !s.getComponents().equals(resource.getComponentsPatch())) continue;
            total += action.simulate() ? s.tryExtract(droplets - total) : s.extract(droplets - total);
        }
        return total == 0 ? FluidStack.EMPTY : makeStack(resource.getFluid(), (int) (total / DROPLETS_PER_MB), resource.getComponentsPatch());
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        if (!this.allowsExtraction() || maxDrain <= 0 || !this.storage.isValid()) return FluidStack.EMPTY;
        for (int i = 0; i < this.storage.size(); i++) {
            FluidResourceSlot s = this.storage.slot(i);
            Fluid fluid = s.getResource();
            if (fluid != null) {
                return this.drain(makeStack(fluid, maxDrain, s.getComponents()), action);
            }
        }
        return FluidStack.EMPTY;
    }
}
