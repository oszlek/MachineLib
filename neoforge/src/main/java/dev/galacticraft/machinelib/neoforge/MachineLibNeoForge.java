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

package dev.galacticraft.machinelib.neoforge;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.impl.MachineLib;
import dev.galacticraft.machinelib.impl.platform.neoforge.MachineLibPlatformImpl;
import dev.galacticraft.machinelib.impl.storage.neoforge.ExposedEnergyStorageNeoForge;
import dev.galacticraft.machinelib.impl.storage.neoforge.ExposedFluidStorageNeoForge;
import dev.galacticraft.machinelib.impl.storage.neoforge.ExposedItemStorageNeoForge;
import dev.galacticraft.machinelib.neoforge.client.MachineLibNeoForgeClient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * NeoForge platform entry point. Delegates loader-agnostic setup to {@link MachineLib#init()} and
 * registers each machine block entity's item/fluid/energy capabilities via
 * {@link RegisterCapabilitiesEvent}.
 */
@Mod("machinelib")
public final class MachineLibNeoForge {
    public MachineLibNeoForge(IEventBus modBus) {
        MachineLib.init();
        modBus.addListener(this::registerCapabilities);
        if (FMLEnvironment.dist.isClient()) {
            MachineLibNeoForgeClient.init(modBus);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<? extends MachineBlockEntity> type : MachineLibPlatformImpl.MACHINE_TYPES) {
            register(event, type);
        }
    }

    private static <BE extends MachineBlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (machine, direction) -> {
            IOFace face = MachineLibPlatformImpl.faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.ENERGY)) return null;
            ResourceFlow flow = face.getFlow();
            long ins = flow.canFlowIn(ResourceFlow.INPUT) ? machine.energyStorage().externalInsertionRate() : 0;
            long ext = flow.canFlowIn(ResourceFlow.OUTPUT) ? machine.energyStorage().externalExtractionRate() : 0;
            if (ins == 0 && ext == 0) return null;
            return new ExposedEnergyStorageNeoForge(machine.energyStorage(), ins, ext);
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (machine, direction) -> {
            IOFace face = MachineLibPlatformImpl.faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.ITEM)) return null;
            return new ExposedItemStorageNeoForge(machine.itemStorage(), face.getFlow());
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (machine, direction) -> {
            IOFace face = MachineLibPlatformImpl.faceFor(machine, direction);
            if (face == null || !face.getType().willAcceptResource(ResourceType.FLUID)) return null;
            return new ExposedFluidStorageNeoForge(machine.fluidStorage(), face.getFlow());
        });
    }
}
