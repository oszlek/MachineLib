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

package dev.galacticraft.machinelib.client.impl.platform.fabric;

import dev.galacticraft.machinelib.client.impl.model.MachineBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric implementation of the client-side fluid metadata/rendering hooks, backed by the Fabric
 * Transfer API's fluid variant system.
 */
public final class MachineLibClientPlatformImpl {
    private MachineLibClientPlatformImpl() {
    }

    public static Component fluidName(Fluid fluid, DataComponentPatch components) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid, components));
    }

    public static TextureAtlasSprite fluidSprite(Fluid fluid, DataComponentPatch components) {
        return FluidVariantRendering.getSprite(FluidVariant.of(fluid, components));
    }

    public static int fluidColor(Fluid fluid, DataComponentPatch components) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid, components));
    }

    public static boolean fluidLighterThanAir(Fluid fluid, DataComponentPatch components) {
        return FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid, components));
    }

    public static @Nullable MachineBakedModel machineModel(BakedModel model) {
        return WrapperBakedModel.unwrap(model) instanceof MachineBakedModel machine ? machine : null;
    }
}
