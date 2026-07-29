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

package dev.galacticraft.machinelib.testmod.block.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.testmod.Constant;
import dev.galacticraft.machinelib.testmod.block.TestModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class TestModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Constant.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            register(Constant.GENERATOR, GeneratorBlockEntity::new, TestModBlocks.GENERATOR);
    public static final RegistrySupplier<BlockEntityType<MixerBlockEntity>> MIXER =
            register(Constant.MIXER, MixerBlockEntity::new, TestModBlocks.MIXER);
    public static final RegistrySupplier<BlockEntityType<MelterBlockEntity>> MELTER =
            register(Constant.MELTER, MelterBlockEntity::new, TestModBlocks.MELTER);

    private static <T extends MachineBlockEntity> RegistrySupplier<BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, RegistrySupplier<net.minecraft.world.level.block.Block> block) {
        RegistrySupplier<BlockEntityType<T>> supplier = TYPES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        supplier.listen(MachineBlockEntity::registerProviders);
        return supplier;
    }

    private TestModBlockEntityTypes() {
    }
}
