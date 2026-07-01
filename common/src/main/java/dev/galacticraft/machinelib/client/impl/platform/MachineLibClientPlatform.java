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

package dev.galacticraft.machinelib.client.impl.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.galacticraft.machinelib.client.impl.model.MachineBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side platform bridge for the loader-specific fluid metadata and rendering lookups that the
 * neutral {@code client} GUI code relies on.
 * <p>
 * Each loader supplies the implementation via Architectury {@link ExpectPlatform}. These hooks are
 * only ever invoked on the render thread, never from server logic or unit tests.
 */
@ApiStatus.Internal
public final class MachineLibClientPlatform {
    private MachineLibClientPlatform() {
    }

    /**
     * {@return the display name of the given fluid variant} (Fabric: {@code FluidVariantAttributes};
     * NeoForge: {@code FluidStack#getHoverName}).
     */
    @ExpectPlatform
    public static @NotNull Component fluidName(@NotNull Fluid fluid, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    /**
     * {@return the still texture sprite of the given fluid variant, or {@code null} if none}.
     */
    @ExpectPlatform
    public static @Nullable TextureAtlasSprite fluidSprite(@NotNull Fluid fluid, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    /**
     * {@return the ARGB tint color of the given fluid variant}.
     */
    @ExpectPlatform
    public static int fluidColor(@NotNull Fluid fluid, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    /**
     * {@return whether the given fluid variant is lighter than air} (rendered filling from the top).
     */
    @ExpectPlatform
    public static boolean fluidLighterThanAir(@NotNull Fluid fluid, @NotNull DataComponentPatch components) {
        throw new AssertionError();
    }

    /**
     * {@return the {@link MachineBakedModel} backing the given baked model, or {@code null} if it is
     * not a machine model} (unwraps the loader's model-wrapper indirection).
     */
    @ExpectPlatform
    public static @Nullable MachineBakedModel machineModel(@NotNull BakedModel model) {
        throw new AssertionError();
    }
}
