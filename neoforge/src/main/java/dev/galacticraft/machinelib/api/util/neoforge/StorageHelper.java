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

package dev.galacticraft.machinelib.api.util.neoforge;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** NeoForge fluid-handler transfer helpers. */
public final class StorageHelper {
    private StorageHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static int move(IFluidHandler from, IFluidHandler to, int maxAmount) {
        FluidStack simulated = from.drain(maxAmount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) return 0;
        int accepted = to.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;
        FluidStack drained = from.drain(simulated.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        return drained.isEmpty() ? 0 : to.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }
}
