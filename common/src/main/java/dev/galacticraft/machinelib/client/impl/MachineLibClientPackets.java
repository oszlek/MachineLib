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

package dev.galacticraft.machinelib.client.impl;

import dev.architectury.networking.NetworkManager;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.client.api.event.MachineStatusEvents;
import dev.galacticraft.machinelib.impl.network.s2c.BaseMachineUpdatePayload;
import dev.galacticraft.machinelib.impl.network.s2c.MachineStatusUpdatePayload;
import dev.galacticraft.machinelib.impl.network.s2c.MenuSyncPayload;
import dev.galacticraft.machinelib.impl.network.s2c.SideConfigurationUpdatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/**
 * Registers the client-side (S2C) network receivers. Kept in the client entry point because the
 * handlers reference client-only types.
 */
public final class MachineLibClientPackets {
    private MachineLibClientPackets() {
    }

    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BaseMachineUpdatePayload.TYPE, BaseMachineUpdatePayload.CODEC, (payload, context) -> context.queue(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine) {
                payload.config().copyInto(machine.getIOConfig());
                machine.setActive(payload.active());
                machine.setChanged();
                machine.requestRerender();
            }
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MachineStatusUpdatePayload.TYPE, MachineStatusUpdatePayload.CODEC, (payload, context) -> context.queue(() ->
                MachineStatusEvents.MACHINE_STATUS_CHANGED.invoker().onMachineStatusChanged(Minecraft.getInstance(), Minecraft.getInstance().player, payload.pos(), payload.status(), payload.oldStatus())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SideConfigurationUpdatePayload.TYPE, SideConfigurationUpdatePayload.CODEC, (payload, context) -> context.queue(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine) {
                machine.getIOConfig().get(payload.face()).setOption(payload.resource(), payload.flow());
                machine.setChanged();
            }
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MenuSyncPayload.TYPE, MenuSyncPayload.CODEC, (payload, context) -> context.queue(() -> {
            RegistryFriendlyByteBuf buf = payload.buf();
            int syncId = buf.readVarInt();
            Player player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof MachineMenu<?> menu && syncId == menu.containerId) {
                menu.getData().handle(buf);
            }
        }));
    }
}
