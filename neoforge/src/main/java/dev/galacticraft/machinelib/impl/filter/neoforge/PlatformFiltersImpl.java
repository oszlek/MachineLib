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

package dev.galacticraft.machinelib.impl.filter.neoforge;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * NeoForge implementation of the capability-inspecting resource filters (via item capabilities).
 */
public final class PlatformFiltersImpl {
    private PlatformFiltersImpl() {
    }

    private static ItemStack stackOf(Item item, DataComponentPatch components) {
        ItemStack stack = new ItemStack(item);
        stack.applyComponents(components);
        return stack;
    }

    private static FluidStack fluidStack(Fluid fluid, int amount, DataComponentPatch components) {
        FluidStack stack = new FluidStack(fluid, amount);
        stack.applyComponents(components);
        return stack;
    }

    public static boolean canExtractEnergy(Item item, DataComponentPatch components) {
        IEnergyStorage energy = stackOf(item, components).getCapability(Capabilities.EnergyStorage.ITEM);
        return energy != null && energy.canExtract() && energy.extractEnergy(1, true) > 0;
    }

    public static boolean canInsertEnergy(Item item, DataComponentPatch components) {
        IEnergyStorage energy = stackOf(item, components).getCapability(Capabilities.EnergyStorage.ITEM);
        return energy != null && energy.canReceive() && energy.receiveEnergy(1, true) > 0;
    }

    public static boolean canExtractFluid(Item item, DataComponentPatch itemComponents, Fluid fluid, DataComponentPatch fluidComponents) {
        IFluidHandlerItem handler = stackOf(item, itemComponents).getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null && !handler.drain(fluidStack(fluid, Integer.MAX_VALUE, fluidComponents), FluidAction.SIMULATE).isEmpty();
    }

    public static boolean canExtractFluidTag(Item item, DataComponentPatch itemComponents, TagKey<Fluid> tag) {
        IFluidHandlerItem handler = stackOf(item, itemComponents).getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return false;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack in = handler.getFluidInTank(i);
            if (!in.isEmpty() && in.is(tag) && !handler.drain(in, FluidAction.SIMULATE).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canInsertFluid(Item item, DataComponentPatch itemComponents, Fluid fluid, DataComponentPatch fluidComponents) {
        IFluidHandlerItem handler = stackOf(item, itemComponents).getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null && handler.fill(fluidStack(fluid, 1000, fluidComponents), FluidAction.SIMULATE) > 0;
    }
}
