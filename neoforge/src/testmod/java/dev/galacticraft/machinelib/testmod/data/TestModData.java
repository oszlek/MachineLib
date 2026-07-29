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

package dev.galacticraft.machinelib.testmod.data;

import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * NeoForge datagen entry point (fired by the {@code testmodData} run; the listener is registered on
 * the mod bus in {@link dev.galacticraft.machinelib.testmod.TestModNeoForge}). Demonstrates that the
 * loader-neutral {@link dev.galacticraft.machinelib.api.data.model.MachineModelProvider} works on
 * NeoForge's {@code GatherDataEvent}.
 */
public final class TestModData {
    private TestModData() {
    }

    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        // Machine models are client assets, but run unconditionally so `runTestmodData` regenerates them
        // even when the run does not request client data explicitly.
        generator.addProvider(true, new TestModMachineModelProvider(generator.getPackOutput()));
    }
}
