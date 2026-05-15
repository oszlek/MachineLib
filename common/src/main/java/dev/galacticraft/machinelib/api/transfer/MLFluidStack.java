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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

/**
 * A loader-neutral representation of a fluid and its data components.
 * Replaces Fabric's {@code FluidVariant} in the loader-agnostic {@code common} module.
 *
 * @param fluid the fluid
 * @param components the data components associated with the fluid
 */
public record MLFluidStack(@NotNull Fluid fluid, @NotNull DataComponentPatch components) {
    public static final Codec<MLFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(MLFluidStack::fluid),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(MLFluidStack::components)
    ).apply(instance, MLFluidStack::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MLFluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(BuiltInRegistries.FLUID.key()),
            MLFluidStack::fluid,
            DataComponentPatch.STREAM_CODEC,
            MLFluidStack::components,
            MLFluidStack::new
    );

    public MLFluidStack(@NotNull Fluid fluid) {
        this(fluid, DataComponentPatch.EMPTY);
    }
}
