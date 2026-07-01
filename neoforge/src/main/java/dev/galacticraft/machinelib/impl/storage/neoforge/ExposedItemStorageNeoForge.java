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

import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges a neutral {@link MachineItemStorage} to NeoForge's {@link IItemHandler}, restricted by
 * the given {@link ResourceFlow}. {@code simulate} maps onto the neutral {@code try*}/commit split.
 */
public record ExposedItemStorageNeoForge(@NotNull MachineItemStorage storage,
                                         @NotNull ResourceFlow flow) implements IItemHandler {
    private boolean allowsInsertion() {
        return this.flow == ResourceFlow.INPUT || this.flow == ResourceFlow.BOTH;
    }

    private boolean allowsExtraction() {
        return this.flow == ResourceFlow.OUTPUT || this.flow == ResourceFlow.BOTH;
    }

    @Override
    public int getSlots() {
        return this.storage.size();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        ItemResourceSlot s = this.storage.slot(slot);
        Item resource = s.getResource();
        return resource == null ? ItemStack.EMPTY : ItemStackUtil.of(resource, s.getComponents(), (int) s.getAmount());
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!this.allowsInsertion() || stack.isEmpty() || !this.storage.isValid()) return stack;
        ItemResourceSlot s = this.storage.slot(slot);
        if (!s.getFilter().test(stack.getItem(), stack.getComponentsPatch())) return stack;
        long inserted = simulate
                ? s.tryInsert(stack.getItem(), stack.getComponentsPatch(), stack.getCount())
                : s.insert(stack.getItem(), stack.getComponentsPatch(), stack.getCount());
        if (inserted >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink((int) inserted);
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!this.allowsExtraction() || amount <= 0 || !this.storage.isValid()) return ItemStack.EMPTY;
        ItemResourceSlot s = this.storage.slot(slot);
        Item resource = s.getResource();
        if (resource == null) return ItemStack.EMPTY;
        var components = s.getComponents();
        long extracted = simulate ? s.tryExtract(amount) : s.extract(amount);
        return extracted == 0 ? ItemStack.EMPTY : ItemStackUtil.of(resource, components, (int) extracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) Math.min(this.storage.slot(slot).getCapacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.storage.slot(slot).getFilter().test(stack.getItem(), stack.getComponentsPatch());
    }
}
