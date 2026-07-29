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
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fixed item-backed handler that accepts one fluid variant and stores only its amount. */
public class SingleVariantFixedItemBackedFluidStorage extends FixedItemBackedFluidStorage {
    private final FluidStack variant;

    public SingleVariantFixedItemBackedFluidStorage(ItemStack stack, long maxIn, long maxOut,
                                                    long capacity, FluidStack variant) {
        super(stack, ResourceFilters.ofResource(variant.getFluid(), variant.getComponentsPatch()),
                maxIn, maxOut, capacity);
        this.variant = variant.copyWithAmount(1);
    }

    @Override
    protected FluidStack getRawFluid() {
        return getRawAmount() > 0
                ? this.variant.copyWithAmount((int) (getRawAmount() / DROPLETS_PER_MB))
                : FluidStack.EMPTY;
    }

    @Override
    protected void setRawContents(FluidStack fluid, long amount) {
        if (amount <= 0) this.stack.remove(MLDataComponents.AMOUNT.get());
        else this.stack.set(MLDataComponents.AMOUNT.get(), amount);
    }
}
