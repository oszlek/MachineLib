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

package dev.galacticraft.machinelib.api.transfer;

/**
 * Platform-neutral fluid amount constants, expressed in droplets (1/81000 of a bucket).
 *
 * <p>MachineLib stores fluid amounts internally in droplets — the same unit Fabric's transfer
 * API uses — so these values match Fabric's {@code FluidConstants} exactly. Platform adapters
 * convert to their loader's native unit at the boundary (e.g. NeoForge millibuckets = droplets / 81).
 */
public final class FluidConstants {
    /**
     * The number of droplets in a bucket.
     */
    public static final long BUCKET = 81000;
    /**
     * The number of droplets in a block of fluid.
     */
    public static final long BLOCK = BUCKET;
    /**
     * The number of droplets in a potion bottle (1/3 of a bucket).
     */
    public static final long BOTTLE = BUCKET / 3;
    /**
     * The number of droplets in an ingot's worth of fluid (1/9 of a bucket).
     */
    public static final long INGOT = BUCKET / 9;
    /**
     * The number of droplets in a nugget's worth of fluid (1/81 of a bucket, i.e. one millibucket).
     */
    public static final long NUGGET = BUCKET / 81;

    private FluidConstants() {
        throw new UnsupportedOperationException();
    }
}
