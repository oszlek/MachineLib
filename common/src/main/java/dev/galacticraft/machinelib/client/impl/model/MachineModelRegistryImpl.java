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

package dev.galacticraft.machinelib.client.impl.model;

/**
 * Loader-neutral constants for the machine-model JSON convention. Machine and base models are tagged
 * with the {@link #MARKER} field so the per-loader model resolver can dispatch on type.
 */
public final class MachineModelRegistryImpl {
    public static final String MARKER = "machinelib:type";
    public static final String BASE_TYPE = "base";
    public static final String MACHINE_TYPE = "machine";
    public static final String DEFAULT_MACHINE_BASE = "machine/base";
    /**
     * NeoForge custom-geometry loader id written into machine model JSON. Inert on Fabric (whose model
     * resolver dispatches by id before parsing); consumed by NeoForge's {@code IGeometryLoader}.
     */
    public static final String NEOFORGE_LOADER = "machinelib:machine";

    private MachineModelRegistryImpl() {
    }
}
