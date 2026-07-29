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

import dev.galacticraft.machinelib.api.filter.ResourceFilter;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import net.minecraft.world.item.ItemStack;

/** Shared state and rate contract for NeoForge item-backed resource capabilities. */
public abstract class GenericItemBackedStorage<T> {
    protected final ItemStack stack;
    protected final ResourceFilter<T> filter;

    protected GenericItemBackedStorage(ItemStack stack, ResourceFilter<T> filter) {
        this.stack = stack;
        this.filter = filter;
    }

    protected GenericItemBackedStorage(ItemStack stack) {
        this(stack, ResourceFilters.any());
    }

    protected abstract long getRawMaxInput();

    protected abstract long getRawMaxOutput();

    protected abstract long getRawCapacity();

    protected abstract long getRawAmount();
}
