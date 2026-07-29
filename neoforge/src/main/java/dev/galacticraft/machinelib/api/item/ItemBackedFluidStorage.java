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

package dev.galacticraft.machinelib.api.item;

import dev.galacticraft.machinelib.api.component.MLDataComponents;
import dev.galacticraft.machinelib.api.filter.ResourceFilter;
import dev.galacticraft.machinelib.api.transfer.MLFluidStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

/** NeoForge item fluid capability backed by MachineLib data components. */
public class ItemBackedFluidStorage extends GenericItemBackedStorage<Fluid> implements IFluidHandlerItem {
    protected static final long DROPLETS_PER_MB = 81;

    public ItemBackedFluidStorage(ItemStack stack, ResourceFilter<Fluid> filter) {
        super(stack, filter);
    }

    protected ItemBackedFluidStorage(ItemStack stack) {
        super(stack);
    }

    @Override
    protected long getRawMaxInput() {
        return component(MLDataComponents.MAX_INPUT.get());
    }

    @Override
    protected long getRawMaxOutput() {
        return component(MLDataComponents.MAX_OUTPUT.get());
    }

    @Override
    protected long getRawCapacity() {
        return component(MLDataComponents.CAPACITY.get());
    }

    @Override
    protected long getRawAmount() {
        return component(MLDataComponents.AMOUNT.get());
    }

    protected FluidStack getRawFluid() {
        MLFluidStack fluid = this.stack.get(MLDataComponents.FLUID.get());
        if (fluid == null) return FluidStack.EMPTY;
        FluidStack result = new FluidStack(fluid.fluid(), (int) (getRawAmount() / DROPLETS_PER_MB));
        result.applyComponents(fluid.components());
        return result;
    }

    protected void setRawContents(FluidStack fluid, long amount) {
        if (amount <= 0) {
            this.stack.remove(MLDataComponents.FLUID.get());
            this.stack.remove(MLDataComponents.AMOUNT.get());
        } else {
            this.stack.set(MLDataComponents.FLUID.get(),
                    new MLFluidStack(fluid.getFluid(), fluid.getComponentsPatch()));
            this.stack.set(MLDataComponents.AMOUNT.get(), amount);
        }
    }

    private long component(DataComponentType<Long> type) {
        return this.stack.getOrDefault(type, 0L);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return tank == 0 ? getRawFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? (int) Math.min(getRawCapacity() / DROPLETS_PER_MB, Integer.MAX_VALUE) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack resource) {
        return tank == 0 && this.filter.test(resource.getFluid(), resource.getComponentsPatch());
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        FluidStack stored = getRawFluid();
        if (!stored.isEmpty() && !FluidStack.isSameFluidSameComponents(stored, resource)) return 0;
        long free = Math.max(0, getRawCapacity() - getRawAmount());
        long accepted = Math.min((long) resource.getAmount() * DROPLETS_PER_MB,
                Math.min(free, getRawMaxInput()));
        accepted = accepted / DROPLETS_PER_MB * DROPLETS_PER_MB;
        if (accepted > 0 && action.execute()) setRawContents(resource, getRawAmount() + accepted);
        return (int) (accepted / DROPLETS_PER_MB);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        FluidStack stored = getRawFluid();
        if (resource.isEmpty() || stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        FluidStack stored = getRawFluid();
        if (maxDrain <= 0 || stored.isEmpty()) return FluidStack.EMPTY;
        long extracted = Math.min((long) maxDrain * DROPLETS_PER_MB,
                Math.min(getRawAmount(), getRawMaxOutput()));
        extracted = extracted / DROPLETS_PER_MB * DROPLETS_PER_MB;
        if (extracted <= 0) return FluidStack.EMPTY;
        FluidStack result = stored.copyWithAmount((int) (extracted / DROPLETS_PER_MB));
        if (action.execute()) setRawContents(stored, getRawAmount() - extracted);
        return result;
    }

    @Override
    public @NotNull ItemStack getContainer() {
        return this.stack;
    }
}
