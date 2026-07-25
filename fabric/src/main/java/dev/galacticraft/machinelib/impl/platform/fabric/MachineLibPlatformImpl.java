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

package dev.galacticraft.machinelib.impl.platform.fabric;

import dev.galacticraft.machinelib.api.block.entity.BaseBlockEntity;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.compat.transfer.ExposedEnergyStorage;
import dev.galacticraft.machinelib.api.compat.transfer.ExposedStorage;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.menu.Tank;
import com.google.common.base.Predicates;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.machinelib.api.util.StorageHelper;
import dev.galacticraft.machinelib.impl.compat.transfer.ExposedItemSlotImpl;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/**
 * Fabric implementation of the {@link dev.galacticraft.machinelib.impl.platform.MachineLibPlatform}
 * {@code @ExpectPlatform} hooks.
 */
public final class MachineLibPlatformImpl {
    private MachineLibPlatformImpl() {
    }

    public static void registerMachineProviders(BlockEntityType<? extends MachineBlockEntity> type) {
        EnergyStorage.SIDED.registerForBlockEntity((machine, direction) -> {
            IOFace face = faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.ENERGY)) return null;
            ResourceFlow flow = face.getFlow();
            long ins = flow.canFlowIn(ResourceFlow.INPUT) ? machine.energyStorage().externalInsertionRate() : 0;
            long ext = flow.canFlowIn(ResourceFlow.OUTPUT) ? machine.energyStorage().externalExtractionRate() : 0;
            if (ins == 0 && ext == 0) return null;
            return ExposedEnergyStorage.create(machine.energyStorage(), ins, ext);
        }, type);
        ItemStorage.SIDED.registerForBlockEntity((machine, direction) -> {
            IOFace face = faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.ITEM)) return null;
            return ExposedStorage.of(machine.itemStorage(), face.getFlow());
        }, type);
        FluidStorage.SIDED.registerForBlockEntity((machine, direction) -> {
            IOFace face = faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.FLUID)) return null;
            return ExposedStorage.of(machine.fluidStorage(), face.getFlow());
        }, type);
    }

    private static @Nullable IOFace faceFor(MachineBlockEntity machine, @Nullable Direction direction) {
        if (direction == null) return null;
        Direction facing = machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return machine.getIOConfig().get(BlockFace.from(facing, direction));
    }

    private static ContainerItemContext contextOf(MachineItemStorage items, int slot) {
        // Internal exposure: the machine charges/exchanges the item in its own slot, so bypass the
        // external flow/filter/PROCESSING restrictions that would otherwise block the capability exchange.
        return ContainerItemContext.ofSingleSlot(new ExposedItemSlotImpl(items.slot(slot), ResourceFlow.BOTH, true));
    }

    public static void chargeFromItem(MachineItemStorage items, int slot, MachineEnergyStorage energy) {
        EnergyStorage itemEnergy = contextOf(items, slot).find(EnergyStorage.ITEM);
        if (itemEnergy != null) {
            EnergyStorageUtil.move(itemEnergy, ExposedEnergyStorage.create(energy, energy.externalInsertionRate(), 0), energy.externalInsertionRate(), null);
        }
    }

    public static void drainPowerToItem(MachineItemStorage items, int slot, MachineEnergyStorage energy) {
        EnergyStorage itemEnergy = contextOf(items, slot).find(EnergyStorage.ITEM);
        if (itemEnergy != null) {
            EnergyStorageUtil.move(ExposedEnergyStorage.create(energy, 0, energy.externalExtractionRate()), itemEnergy, energy.externalExtractionRate(), null);
        }
    }

    public static void takeFluidFromItem(MachineItemStorage items, int slot, FluidResourceSlot tank, @Nullable Fluid fluid) {
        Storage<FluidVariant> storage = contextOf(items, slot).find(FluidStorage.ITEM);
        if (storage != null) {
            if (fluid != null) {
                StorageHelper.move(FluidVariant.of(fluid), storage, tank, Long.MAX_VALUE, null);
            } else {
                StorageHelper.move(storage, tank, Long.MAX_VALUE, null);
            }
        }
    }

    public static void drainFluidToItem(MachineItemStorage items, int slot, FluidResourceSlot tank) {
        Storage<FluidVariant> storage = contextOf(items, slot).find(FluidStorage.ITEM);
        if (storage != null) {
            StorageHelper.move(tank, storage, Long.MAX_VALUE, null);
        }
    }

    public static void interactTank(ServerPlayer player, AbstractContainerMenu menu, Tank tank) {
        Storage<FluidVariant> item = ContainerItemContext.ofPlayerCursor(player, menu).find(FluidStorage.ITEM);
        if (item != null && tank.getSlot() instanceof FluidResourceSlot slot) {
            if (item.supportsExtraction() && tank.getInputType().playerInsertion()) {
                StorageHelper.move(item, slot, Long.MAX_VALUE, null);
            } else if (item.supportsInsertion() && tank.getInputType().playerExtraction()) {
                StorageHelper.move(slot, item, Long.MAX_VALUE, null);
            }
        }
    }

    public static void spreadEnergy(ServerLevel level, BlockPos pos, Direction direction, MachineEnergyStorage storage) {
        EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
        if (target != null) {
            EnergyStorageUtil.move(ExposedEnergyStorage.create(storage, 0, storage.externalExtractionRate()), target, storage.externalExtractionRate(), null);
        }
    }

    public static void spreadFluid(ServerLevel level, BlockPos pos, Direction direction, MachineFluidStorage storage) {
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
        if (target != null) {
            StorageUtil.move(ExposedStorage.of(storage, ResourceFlow.OUTPUT), target, Predicates.alwaysTrue(), FluidConstants.BUCKET, null);
        }
    }
}
