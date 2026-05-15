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

package dev.galacticraft.machinelib.impl.filter;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Platform bridge for the capability-inspecting {@code ResourceFilters} factories.
 * Each loader implements these by querying its native item energy/fluid capabilities.
 */
@ApiStatus.Internal
public final class PlatformFilters {
    private PlatformFilters() {
    }

    @ExpectPlatform
    public static boolean canExtractEnergy(@NotNull Item item, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canInsertEnergy(@NotNull Item item, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canExtractFluid(@NotNull Item item, @NotNull DataComponentPatch itemComponents, @NotNull Fluid fluid, @NotNull DataComponentPatch fluidComponents) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canExtractFluidTag(@NotNull Item item, @NotNull DataComponentPatch itemComponents, @NotNull TagKey<Fluid> tag) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canInsertFluid(@NotNull Item item, @NotNull DataComponentPatch itemComponents, @NotNull Fluid fluid, @NotNull DataComponentPatch fluidComponents) {
        throw new AssertionError();
    }
}
