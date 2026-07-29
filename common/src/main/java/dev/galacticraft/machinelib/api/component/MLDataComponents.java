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

package dev.galacticraft.machinelib.api.component;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.galacticraft.machinelib.api.transfer.MLFluidStack;
import dev.galacticraft.machinelib.impl.Constant;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.UnaryOperator;

/**
 * MachineLib's data component types. Registered through Architectury's {@link DeferredRegister} so the
 * timing is correct on both loaders — NeoForge freezes {@code BuiltInRegistries} before {@code init()}
 * runs, so a direct {@code Registry.register} at class-init would crash the mod on NeoForge.
 */
public class MLDataComponents {
    private static final StreamCodec<ByteBuf, Long> LONG_STREAM_CODEC = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Constant.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Long>> AMOUNT = register("amount", b -> b
            .persistent(Codec.LONG)
            .networkSynchronized(LONG_STREAM_CODEC)
    );

    public static final RegistrySupplier<DataComponentType<Long>> CAPACITY = register("capacity", b -> b
            .persistent(Codec.LONG)
            .networkSynchronized(LONG_STREAM_CODEC)
    );

    public static final RegistrySupplier<DataComponentType<Long>> MAX_INPUT = register("max_input", b -> b
            .persistent(Codec.LONG)
            .networkSynchronized(LONG_STREAM_CODEC)
    );

    public static final RegistrySupplier<DataComponentType<Long>> MAX_OUTPUT = register("max_output", b -> b
            .persistent(Codec.LONG)
            .networkSynchronized(LONG_STREAM_CODEC)
    );

    public static final RegistrySupplier<DataComponentType<MLFluidStack>> FLUID = register("fluid", b -> b
            .persistent(MLFluidStack.CODEC)
            .networkSynchronized(MLFluidStack.STREAM_CODEC)
    );

    private static <T> RegistrySupplier<DataComponentType<T>> register(String id, UnaryOperator<DataComponentType.Builder<T>> op) {
        return COMPONENTS.register(id, () -> op.apply(DataComponentType.builder()).build());
    }

    public static void init() {
        COMPONENTS.register();
    }
}
