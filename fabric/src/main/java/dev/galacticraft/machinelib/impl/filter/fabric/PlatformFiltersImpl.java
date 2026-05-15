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

package dev.galacticraft.machinelib.impl.filter.fabric;

import dev.galacticraft.machinelib.api.transfer.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import team.reborn.energy.api.EnergyStorage;

public final class PlatformFiltersImpl {
    private PlatformFiltersImpl() {
    }

    public static boolean canExtractEnergy(Item item, DataComponentPatch components) {
        EnergyStorage storage = ContainerItemContext.withConstant(ItemVariant.of(item, components), 1).find(EnergyStorage.ITEM);
        if (storage == null || !storage.supportsExtraction()) return false;
        try (Transaction test = Transaction.openNested(Transaction.getCurrentUnsafe())) { // SAFE: the transaction is immediately canceled
            if (storage.extract(1, test) == 1) return true;
        }
        return false;
    }

    public static boolean canInsertEnergy(Item item, DataComponentPatch components) {
        EnergyStorage storage = ContainerItemContext.withConstant(ItemVariant.of(item, components), 1).find(EnergyStorage.ITEM);
        if (storage == null || !storage.supportsInsertion()) return false;
        try (Transaction test = Transaction.openNested(Transaction.getCurrentUnsafe())) { // SAFE: the transaction is immediately canceled
            if (storage.insert(1, test) == 1) return true;
        }
        return false;
    }

    public static boolean canExtractFluid(Item item, DataComponentPatch itemComponents, Fluid fluid, DataComponentPatch fluidComponents) {
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(ItemVariant.of(item, itemComponents), 1).find(FluidStorage.ITEM);
        if (storage == null || !storage.supportsExtraction()) return false;
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            if (storage.extract(FluidVariant.of(fluid, fluidComponents), FluidConstants.BUCKET, transaction) > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean canExtractFluidTag(Item item, DataComponentPatch itemComponents, TagKey<Fluid> tag) {
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(ItemVariant.of(item, itemComponents), 1).find(FluidStorage.ITEM);
        if (storage == null || !storage.supportsExtraction()) return false;
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            for (StorageView<FluidVariant> view : storage) {
                FluidVariant resource = view.getResource();
                if (!resource.isBlank() && resource.getFluid().is(tag)) {
                    if (storage.extract(resource, FluidConstants.BUCKET, transaction) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean canInsertFluid(Item item, DataComponentPatch itemComponents, Fluid fluid, DataComponentPatch fluidComponents) {
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(ItemVariant.of(item, itemComponents), 1).find(FluidStorage.ITEM);
        if (storage == null || !storage.supportsInsertion()) return false;
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            if (storage.insert(FluidVariant.of(fluid, fluidComponents), FluidConstants.BUCKET, transaction) > 0) {
                return true;
            }
        }
        return false;
    }
}
