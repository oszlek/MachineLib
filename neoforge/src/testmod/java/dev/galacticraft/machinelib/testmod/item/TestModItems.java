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

package dev.galacticraft.machinelib.testmod.item;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.galacticraft.machinelib.testmod.Constant;
import dev.galacticraft.machinelib.testmod.block.TestModBlocks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TestModItems {
    public static final int BATTERY_CAPACITY = 15000;
    public static final int BATTERY_TRANSFER = BATTERY_CAPACITY / 50;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Constant.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Constant.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Constant.MOD_ID, Registries.CREATIVE_MODE_TAB);

    /**
     * Stored-energy component for the battery item; read/written by its NeoForge energy capability.
     */
    public static final RegistrySupplier<DataComponentType<Integer>> ENERGY = COMPONENTS.register("energy",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final RegistrySupplier<Item> GENERATOR = ITEMS.register(Constant.GENERATOR, () -> new BlockItem(TestModBlocks.GENERATOR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MIXER = ITEMS.register(Constant.MIXER, () -> new BlockItem(TestModBlocks.MIXER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MELTER = ITEMS.register(Constant.MELTER, () -> new BlockItem(TestModBlocks.MELTER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> BATTERY = ITEMS.register("battery", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("testmod", () -> CreativeModeTab.builder()
            .title(Component.literal("MachineLib Test"))
            .icon(() -> new ItemStack(GENERATOR.get()))
            .displayItems((params, output) -> {
                output.accept(GENERATOR.get());
                output.accept(MIXER.get());
                output.accept(MELTER.get());
                output.accept(BATTERY.get());
            })
            .build());

    private TestModItems() {
    }
}
