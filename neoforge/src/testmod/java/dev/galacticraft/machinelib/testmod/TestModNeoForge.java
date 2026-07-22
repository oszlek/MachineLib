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

package dev.galacticraft.machinelib.testmod;

import dev.galacticraft.machinelib.testmod.block.TestModBlocks;
import dev.galacticraft.machinelib.testmod.block.entity.TestModBlockEntityTypes;
import dev.galacticraft.machinelib.testmod.client.TestModNeoForgeClient;
import dev.galacticraft.machinelib.testmod.data.TestModData;
import dev.galacticraft.machinelib.testmod.item.TestModItems;
import dev.galacticraft.machinelib.testmod.menu.TestModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

@Mod("machinelib_testmod")
public final class TestModNeoForge {
    public TestModNeoForge(IEventBus modBus) {
        Constant.LOGGER.info("Initializing MachineLib test mod (NeoForge)");
        TestModBlocks.BLOCKS.register();
        TestModItems.COMPONENTS.register();
        TestModItems.ITEMS.register();
        TestModItems.TABS.register();
        TestModBlockEntityTypes.TYPES.register();
        TestModMenuTypes.MENUS.register();

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(TestModData::onGatherData);
        if (FMLEnvironment.dist.isClient()) {
            TestModNeoForgeClient.init(modBus);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> new ComponentEnergyStorage(stack, TestModItems.ENERGY.get(), TestModItems.BATTERY_CAPACITY, TestModItems.BATTERY_TRANSFER, TestModItems.BATTERY_TRANSFER),
                TestModItems.BATTERY.get());
    }
}
