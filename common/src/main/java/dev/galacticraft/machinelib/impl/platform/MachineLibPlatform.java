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

package dev.galacticraft.machinelib.impl.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.menu.Tank;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platform bridge for the loader-specific behavior that {@code common} block entities and blocks rely on:
 * native capability exposure, item &lt;-&gt; machine transfer, menu opening and network sends.
 * <p>
 * Each loader supplies the implementation via Architectury {@link ExpectPlatform}.
 */
@ApiStatus.Internal
public final class MachineLibPlatform {
    private MachineLibPlatform() {
    }

    /**
     * Registers the item/fluid/energy storages of every machine of the given block-entity type as the
     * platform's native capability, honoring the machine's per-side IO configuration.
     *
     * @param type the machine block-entity type
     */
    @ExpectPlatform
    public static void registerMachineProviders(@NotNull BlockEntityType<? extends MachineBlockEntity> type) {
        throw new AssertionError();
    }

    /**
     * Extracts energy from the item in the given slot into the machine's energy storage.
     */
    @ExpectPlatform
    public static void chargeFromItem(@NotNull MachineItemStorage items, int slot, @NotNull MachineEnergyStorage energy) {
        throw new AssertionError();
    }

    /**
     * Extracts energy from an item in a vanilla container into a neutral machine energy storage.
     */
    @ExpectPlatform
    public static void chargeFromContainerItem(@NotNull Container container, int slot, @NotNull MachineEnergyStorage energy) {
        throw new AssertionError();
    }

    /**
     * Inserts energy from the machine's energy storage into the item in the given slot.
     */
    @ExpectPlatform
    public static void drainPowerToItem(@NotNull MachineItemStorage items, int slot, @NotNull MachineEnergyStorage energy) {
        throw new AssertionError();
    }

    /**
     * Extracts fluid from the item in the given slot into the tank. If {@code fluid} is {@code null}, any fluid the
     * tank accepts is moved.
     */
    @ExpectPlatform
    public static void takeFluidFromItem(@NotNull MachineItemStorage items, int slot, @NotNull FluidResourceSlot tank, @Nullable Fluid fluid) {
        throw new AssertionError();
    }

    /**
     * Inserts fluid from the tank into the item in the given slot.
     */
    @ExpectPlatform
    public static void drainFluidToItem(@NotNull MachineItemStorage items, int slot, @NotNull FluidResourceSlot tank) {
        throw new AssertionError();
    }

    /**
     * Moves fluid between the player's held item and the given tank (platform item-fluid capability).
     */
    @ExpectPlatform
    public static void interactTank(@NotNull ServerPlayer player, @NotNull AbstractContainerMenu menu, @NotNull Tank tank) {
        throw new AssertionError();
    }

    /**
     * Pushes energy from the machine's storage into the energy-accepting block adjacent to the given
     * side (loader-native block energy capability). Called once per energy-output face.
     */
    @ExpectPlatform
    public static void spreadEnergy(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull Direction direction, @NotNull MachineEnergyStorage storage) {
        throw new AssertionError();
    }

    /**
     * Pushes fluid from the machine's storage into the fluid-accepting block adjacent to the given side
     * (loader-native block fluid capability). Called once per fluid-output face.
     */
    @ExpectPlatform
    public static void spreadFluid(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull Direction direction, @NotNull MachineFluidStorage storage) {
        throw new AssertionError();
    }

    /**
     * Pushes up to sixteen items from the machine into the item-accepting block adjacent to the
     * given side.
     */
    @ExpectPlatform
    public static void spreadItems(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull Direction direction, @NotNull MachineItemStorage storage) {
        throw new AssertionError();
    }
}
