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

package dev.galacticraft.machinelib.impl.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.galacticraft.machinelib.impl.network.c2s.AccessLevelPayload;
import dev.galacticraft.machinelib.impl.network.c2s.RedstoneModePayload;
import dev.galacticraft.machinelib.impl.network.c2s.SideConfigurationClickPayload;
import dev.galacticraft.machinelib.impl.network.c2s.TankInteractionPayload;
import dev.galacticraft.machinelib.impl.network.s2c.BaseMachineUpdatePayload;
import dev.galacticraft.machinelib.impl.network.s2c.MachineStatusUpdatePayload;
import dev.galacticraft.machinelib.impl.network.s2c.MenuSyncPayload;
import dev.galacticraft.machinelib.impl.network.s2c.SideConfigurationUpdatePayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-agnostic network registration via Architectury's {@link NetworkManager}.
 *
 * <p>{@link #registerCommon()} registers the server-to-client payload types (so the server can send)
 * and the client-to-server receivers (server-side handlers). The client-side S2C receivers are
 * registered separately in the client entry point, since they reference client-only types.
 */
public final class MachineLibPackets {
    private MachineLibPackets() {
    }

    public static void registerCommon() {
        // Server -> client payload types. On the physical client the type is registered together with
        // its receiver in the client entry point (MachineLibClientPackets), so only register the send-
        // side type here on the dedicated server to avoid a double registration.
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(BaseMachineUpdatePayload.TYPE, BaseMachineUpdatePayload.CODEC);
            NetworkManager.registerS2CPayloadType(MachineStatusUpdatePayload.TYPE, MachineStatusUpdatePayload.CODEC);
            NetworkManager.registerS2CPayloadType(SideConfigurationUpdatePayload.TYPE, SideConfigurationUpdatePayload.CODEC);
            NetworkManager.registerS2CPayloadType(MenuSyncPayload.TYPE, MenuSyncPayload.CODEC);
        }

        // Client -> server receivers (server-side handlers).
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, AccessLevelPayload.TYPE, AccessLevelPayload.CODEC,
                (payload, context) -> context.queue(() -> payload.apply((ServerPlayer) context.getPlayer())));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RedstoneModePayload.TYPE, RedstoneModePayload.CODEC,
                (payload, context) -> context.queue(() -> payload.apply((ServerPlayer) context.getPlayer())));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SideConfigurationClickPayload.TYPE, SideConfigurationClickPayload.CODEC,
                (payload, context) -> context.queue(() -> payload.apply((ServerPlayer) context.getPlayer())));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TankInteractionPayload.TYPE, TankInteractionPayload.CODEC,
                (payload, context) -> context.queue(() -> payload.apply((ServerPlayer) context.getPlayer())));
    }
}
