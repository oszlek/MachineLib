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

package dev.galacticraft.machinelib.neoforge.client;

import dev.galacticraft.machinelib.client.impl.MachineLibClientPackets;
import dev.galacticraft.machinelib.client.impl.model.neoforge.MachineGeometryLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * NeoForge client bootstrap. Registers the machine geometry loader and the client-side (S2C) network
 * receivers. Invoked from {@link dev.galacticraft.machinelib.neoforge.MachineLibNeoForge} only on the
 * physical client.
 */
public final class MachineLibNeoForgeClient {
    private MachineLibNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(MachineLibNeoForgeClient::registerGeometryLoaders);
        MachineLibClientPackets.registerClient();
    }

    private static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(MachineGeometryLoader.ID, MachineGeometryLoader.INSTANCE);
    }
}
