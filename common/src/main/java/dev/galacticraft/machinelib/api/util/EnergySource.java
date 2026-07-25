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

package dev.galacticraft.machinelib.api.util;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.configuration.IOConfig;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.impl.Constant;
import dev.galacticraft.machinelib.impl.platform.MachineLibPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Pushes a machine's stored energy into adjacent energy-accepting blocks through each of the machine's
 * energy-output faces. The neighbor lookup and transfer is loader-specific (Fabric: team-reborn
 * {@code EnergyStorage.SIDED}; NeoForge: {@code Capabilities.EnergyStorage.BLOCK}), bridged via
 * {@link MachineLibPlatform#spreadEnergy}.
 */
public class EnergySource {
    private final IOConfig config;
    private final MachineEnergyStorage storage;

    public EnergySource(IOConfig config, MachineEnergyStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    public EnergySource(MachineBlockEntity machine) {
        this.config = machine.getIOConfig();
        this.storage = machine.energyStorage();
    }

    public void trySpreadEnergy(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (Direction direction : Constant.Cache.DIRECTIONS) {
            IOFace face = this.config.get(BlockFace.from(facing, direction));
            if (face.getType().willAcceptResource(ResourceType.ENERGY) && face.getFlow().canFlowIn(ResourceFlow.OUTPUT)) {
                MachineLibPlatform.spreadEnergy(level, pos, direction, this.storage);
            }
        }
    }
}
