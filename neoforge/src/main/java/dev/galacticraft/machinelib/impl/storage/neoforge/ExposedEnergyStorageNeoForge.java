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

import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges the neutral {@link MachineEnergyStorage} to NeoForge's {@link IEnergyStorage}.
 *
 * <p>NeoForge's {@code simulate} boolean maps directly onto the neutral {@code try*}/commit split.
 * Energy is {@code long} internally but {@code int} in the NeoForge API, so values saturate at
 * {@link Integer#MAX_VALUE}.
 */
public record ExposedEnergyStorageNeoForge(@NotNull MachineEnergyStorage parent, long maxInsertion,
                                           long maxExtraction) implements IEnergyStorage {
    private static int saturate(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (this.maxInsertion <= 0 || !this.parent.isValid()) return 0;
        long amount = Math.min(toReceive, this.maxInsertion);
        return saturate(simulate ? this.parent.tryInsert(amount) : this.parent.insert(amount));
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (this.maxExtraction <= 0 || !this.parent.isValid()) return 0;
        long amount = Math.min(toExtract, this.maxExtraction);
        return saturate(simulate ? this.parent.tryExtract(amount) : this.parent.extract(amount));
    }

    @Override
    public int getEnergyStored() {
        return saturate(this.parent.getAmount());
    }

    @Override
    public int getMaxEnergyStored() {
        return saturate(this.parent.getCapacity());
    }

    @Override
    public boolean canExtract() {
        return this.maxExtraction > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxInsertion > 0;
    }
}
