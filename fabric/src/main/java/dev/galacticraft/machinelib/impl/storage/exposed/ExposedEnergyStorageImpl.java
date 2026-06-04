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

package dev.galacticraft.machinelib.impl.storage.exposed;

import dev.galacticraft.machinelib.api.compat.transfer.ExposedEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.NotNull;
import team.reborn.energy.api.EnergyStorage;

/**
 * An {@link EnergyStorage energy storage} that restricts input and output, bridging the neutral
 * {@link MachineEnergyStorage} to Fabric's transaction system.
 *
 * <p>Simulation uses the neutral {@code tryInsert}/{@code tryExtract}; commits use the neutral
 * {@code insert}/{@code extract} (which mark the block entity changed); rollback restores the
 * captured energy amount.
 *
 * @see EnergyStorage
 */
public final class ExposedEnergyStorageImpl extends SnapshotParticipant<Long> implements ExposedEnergyStorage {
    private final @NotNull MachineEnergyStorage parent;
    private final long maxInsertion;
    private final long maxExtraction;

    public ExposedEnergyStorageImpl(@NotNull MachineEnergyStorage parent, long maxInsertion, long maxExtraction) {
        this.parent = parent;
        this.maxInsertion = maxInsertion;
        this.maxExtraction = maxExtraction;
    }

    @Override
    public boolean supportsInsertion() {
        return this.parent.isValid() && this.maxInsertion > 0;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (this.supportsInsertion()) {
            long moved = this.parent.tryInsert(Math.min(this.maxInsertion, maxAmount));
            if (moved > 0) {
                this.updateSnapshots(transaction);
                this.parent.insert(moved);
            }
            return moved;
        }
        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return this.parent.isValid() && this.maxExtraction > 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (this.supportsExtraction()) {
            long moved = this.parent.tryExtract(Math.min(this.maxExtraction, maxAmount));
            if (moved > 0) {
                this.updateSnapshots(transaction);
                this.parent.extract(moved);
            }
            return moved;
        }
        return 0;
    }

    @Override
    public long getAmount() {
        return this.parent.getAmount();
    }

    @Override
    public long getCapacity() {
        return this.parent.getCapacity();
    }

    @Override
    protected Long createSnapshot() {
        return this.parent.getAmount();
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        this.parent.setEnergy(snapshot);
    }
}
