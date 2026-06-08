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

package dev.galacticraft.machinelib.api.compat.transfer;

import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.ResourceStorage;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.impl.compat.transfer.ExposedFluidSlotImpl;
import dev.galacticraft.machinelib.impl.compat.transfer.ExposedItemSlotImpl;
import dev.galacticraft.machinelib.impl.compat.transfer.ExposedStorageImpl;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a resource storage exposed to adjacent blocks or items.
 * Has additional restrictions on the flow of resources compared to {@link ResourceStorage}s.
 *
 * @param <Resource> the type of resource stored in the storage
 * @param <Variant> the type of variant associated with the resource
 */
public interface ExposedStorage<Resource, Variant extends TransferVariant<Resource>> extends Storage<Variant> {
    /**
     * {@return a Fabric {@code Storage} view over the given item storage restricted to the given flow}
     */
    @SuppressWarnings("unchecked")
    static @NotNull ExposedStorage<Item, ItemVariant> of(@NotNull MachineItemStorage storage, @NotNull ResourceFlow flow) {
        ExposedSlot<Item, ItemVariant>[] slots = new ExposedSlot[storage.size()];
        for (int i = 0; i < storage.size(); i++) {
            slots[i] = new ExposedItemSlotImpl(storage.slot(i), flow);
        }
        return new ExposedStorageImpl<>(storage, slots);
    }

    /**
     * {@return a Fabric {@code Storage} view over the given fluid storage restricted to the given flow}
     */
    @SuppressWarnings("unchecked")
    static @NotNull ExposedStorage<Fluid, FluidVariant> of(@NotNull MachineFluidStorage storage, @NotNull ResourceFlow flow) {
        ExposedSlot<Fluid, FluidVariant>[] slots = new ExposedSlot[storage.size()];
        for (int i = 0; i < storage.size(); i++) {
            slots[i] = new ExposedFluidSlotImpl(storage.slot(i), flow);
        }
        return new ExposedStorageImpl<>(storage, slots);
    }
}
