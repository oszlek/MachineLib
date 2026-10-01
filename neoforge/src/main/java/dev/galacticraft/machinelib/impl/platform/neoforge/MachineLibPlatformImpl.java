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

package dev.galacticraft.machinelib.impl.platform.neoforge;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.menu.Tank;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.FluidConstants;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.machinelib.impl.storage.neoforge.ExposedItemStorageNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * NeoForge implementation of the {@code @ExpectPlatform} hooks. Storage exposure is capability-based:
 * machine block-entity types are collected here and registered against
 * {@link net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent} in the mod entry point.
 */
public final class MachineLibPlatformImpl {
    /**
     * Machine block-entity types awaiting capability registration (drained by the entry point).
     */
    public static final Set<BlockEntityType<? extends MachineBlockEntity>> MACHINE_TYPES = new CopyOnWriteArraySet<>();

    private MachineLibPlatformImpl() {
    }

    public static void registerMachineProviders(BlockEntityType<? extends MachineBlockEntity> type) {
        MACHINE_TYPES.add(type);
    }

    /**
     * {@return the machine's IO face config for the given side, or {@code null}}
     */
    public static @Nullable IOFace faceFor(MachineBlockEntity machine, @Nullable Direction direction) {
        if (direction == null) return null;
        Direction facing = machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return machine.getIOConfig().get(BlockFace.from(facing, direction));
    }

    private static ItemStack stackOf(ItemResourceSlot slot) {
        return slot.getResource() == null ? ItemStack.EMPTY : ItemStackUtil.of(slot.getResource(), slot.getComponents(), (int) slot.getAmount());
    }

    public static void chargeFromItem(MachineItemStorage items, int slot, MachineEnergyStorage energy) {
        ItemResourceSlot s = items.slot(slot);
        ItemStack stack = stackOf(s);
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy != null && itemEnergy.canExtract()) {
            int extracted = itemEnergy.extractEnergy(saturate(energy.tryInsert(energy.externalInsertionRate())), false);
            if (extracted > 0) {
                energy.insert(extracted);
                s.set(stack.getItem(), stack.getComponentsPatch(), stack.getCount());
            }
        }
    }

    public static void chargeFromContainerItem(Container container, int slot, MachineEnergyStorage energy) {
        ItemStack stack = container.getItem(slot);
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy != null && itemEnergy.canExtract()) {
            int extracted = itemEnergy.extractEnergy(saturate(energy.tryInsert(energy.externalInsertionRate())), false);
            if (extracted > 0) {
                energy.insert(extracted);
                container.setItem(slot, stack);
                container.setChanged();
            }
        }
    }

    public static void drainPowerToItem(MachineItemStorage items, int slot, MachineEnergyStorage energy) {
        ItemResourceSlot s = items.slot(slot);
        ItemStack stack = stackOf(s);
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy != null && itemEnergy.canReceive()) {
            int received = itemEnergy.receiveEnergy(saturate(energy.tryExtract(energy.externalExtractionRate())), false);
            if (received > 0) {
                energy.extract(received);
                s.set(stack.getItem(), stack.getComponentsPatch(), stack.getCount());
            }
        }
    }

    public static void takeFluidFromItem(MachineItemStorage items, int slot, FluidResourceSlot tank, int drainRate, @Nullable Fluid fluid) {
        ItemResourceSlot s = items.slot(slot);
        ItemStack stack = stackOf(s);
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        drainRate = drainRate / 81;
        if (item == null) return;
        FluidStack drained = fluid == null
                ? item.drain(drainRate, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
                : item.drain(new FluidStack(fluid, drainRate), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        long droplets = (long) drained.getAmount() * 81;
        long inserted = tank.tryInsert(drained.getFluid(), drained.getComponentsPatch(), droplets);
        if (inserted <= 0) return;
        int mb = (int) (inserted / 81);
        FluidStack actuallyDrained = item.drain(drained.copyWithAmount(mb), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (!actuallyDrained.isEmpty()) {
            tank.insert(actuallyDrained.getFluid(), actuallyDrained.getComponentsPatch(), (long) actuallyDrained.getAmount() * 81);
            s.set(item.getContainer().getItem(), item.getContainer().getComponentsPatch(), item.getContainer().getCount());
        }
    }

    public static void drainFluidToItem(MachineItemStorage items, int slot, FluidResourceSlot tank, int drainRate) {
        ItemResourceSlot s = items.slot(slot);
        ItemStack stack = stackOf(s);
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null || tank.getResource() == null) return;
        int mb = (int) (Math.min(tank.getAmount(), drainRate) / 81);
        if (mb <= 0) return;
        FluidStack toFill = new FluidStack(tank.getResource(), mb);
        toFill.applyComponents(tank.getComponents());
        int filled = item.fill(toFill, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            tank.extract((long) filled * 81);
            s.set(item.getContainer().getItem(), item.getContainer().getComponentsPatch(), item.getContainer().getCount());
        }
    }

    public static void interactTank(ServerPlayer player, AbstractContainerMenu menu, Tank tank) {
        ItemStack cursor = menu.getCarried();
        IFluidHandlerItem item = cursor.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null || !(tank.getSlot() instanceof FluidResourceSlot slot)) return;
        if (tank.getInputType().playerInsertion()) {
            FluidStack drained = item.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty() && slot.getFilter().test(drained.getFluid(), drained.getComponentsPatch())) {
                long inserted = slot.tryInsert(drained.getFluid(), drained.getComponentsPatch(), (long) drained.getAmount() * 81);
                int mb = (int) (inserted / 81);
                if (mb > 0) {
                    FluidStack real = item.drain(drained.copyWithAmount(mb), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    slot.insert(real.getFluid(), real.getComponentsPatch(), (long) real.getAmount() * 81);
                    menu.setCarried(item.getContainer());
                }
            }
        } else if (tank.getInputType().playerExtraction() && slot.getResource() != null) {
            int mb = (int) (slot.getAmount() / 81);
            FluidStack toFill = new FluidStack(slot.getResource(), mb);
            toFill.applyComponents(slot.getComponents());
            int filled = item.fill(toFill, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                slot.extract((long) filled * 81);
                menu.setCarried(item.getContainer());
            }
        }
    }

    private static int saturate(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    public static void spreadEnergy(ServerLevel level, BlockPos pos, Direction direction, MachineEnergyStorage storage) {
        IEnergyStorage target = Capabilities.EnergyStorage.BLOCK.getCapability(level, pos.relative(direction), null, null, direction.getOpposite());
        if (target != null && target.canReceive()) {
            int accepted = target.receiveEnergy(saturate(storage.tryExtract(storage.externalExtractionRate())), false);
            if (accepted > 0) storage.extract(accepted);
        }
    }

    public static void spreadFluid(ServerLevel level, BlockPos pos, Direction direction, MachineFluidStorage storage) {
        IFluidHandler target = Capabilities.FluidHandler.BLOCK.getCapability(level, pos.relative(direction), null, null, direction.getOpposite());
        if (target == null) return;
        int maxMb = (int) (FluidConstants.BUCKET / 81);
        for (int i = 0; i < storage.size(); i++) {
            FluidResourceSlot slot = storage.slot(i);
            Fluid fluid = slot.getResource();
            if (fluid == null || !slot.transferMode().externalExtraction()) continue;
            int mb = (int) Math.min(slot.getAmount() / 81, maxMb);
            if (mb <= 0) continue;
            FluidStack stack = new FluidStack(fluid, mb);
            stack.applyComponents(slot.getComponents());
            int filled = target.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) slot.extract((long) filled * 81);
        }
    }

    public static void spreadItems(ServerLevel level, BlockPos pos, Direction direction, MachineItemStorage storage) {
        IItemHandler target = Capabilities.ItemHandler.BLOCK.getCapability(
                level, pos.relative(direction), null, null, direction.getOpposite());
        if (target == null) return;

        ExposedItemStorageNeoForge source = new ExposedItemStorageNeoForge(storage,
                dev.galacticraft.machinelib.api.transfer.ResourceFlow.OUTPUT);
        int remaining = 16;
        for (int sourceSlot = 0; sourceSlot < source.getSlots() && remaining > 0; sourceSlot++) {
            ItemStack available = source.extractItem(sourceSlot, remaining, true);
            if (available.isEmpty()) continue;

            ItemStack toInsert = available;
            for (int targetSlot = 0; targetSlot < target.getSlots() && !toInsert.isEmpty(); targetSlot++) {
                toInsert = target.insertItem(targetSlot, toInsert, true);
            }
            int accepted = available.getCount() - toInsert.getCount();
            if (accepted <= 0) continue;

            ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
            ItemStack remainder = extracted;
            for (int targetSlot = 0; targetSlot < target.getSlots() && !remainder.isEmpty(); targetSlot++) {
                remainder = target.insertItem(targetSlot, remainder, false);
            }
            if (!remainder.isEmpty()) {
                storage.slot(sourceSlot).insert(remainder.getItem(), remainder.getComponentsPatch(), remainder.getCount());
            }
            remaining -= extracted.getCount() - remainder.getCount();
        }
    }
}
