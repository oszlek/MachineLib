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

package dev.galacticraft.machinelib.testmod.gametest;

import dev.galacticraft.machinelib.testmod.block.TestModBlocks;
import dev.galacticraft.machinelib.testmod.block.entity.GeneratorBlockEntity;
import dev.galacticraft.machinelib.testmod.item.TestModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("machinelib")
@PrefixGameTestTemplate(false)
public final class NeoForgeParityGameTests {
    private static final BlockPos MACHINE_POS = new BlockPos(1, 2, 1);

    private NeoForgeParityGameTests() {
    }

    @GameTest(template = "3x3")
    public static void machineCanBePlaced(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TestModBlocks.GENERATOR.get());
        GeneratorBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
        if (machine == null) {
            helper.fail("Generator block did not create its block entity", MACHINE_POS);
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "3x3", timeoutTicks = 10)
    public static void generatorDrainsEnergyToItem(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TestModBlocks.GENERATOR.get());
        GeneratorBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
        machine.energyStorage().setEnergy(machine.energyStorage().getCapacity());
        machine.itemStorage().slot(GeneratorBlockEntity.BATTERY_SLOT).set(TestModItems.BATTERY.get(), 1);
        helper.runAfterDelay(1, () -> {
            if (machine.energyStorage().isFull()) {
                helper.fail("Generator did not drain energy to its battery", MACHINE_POS);
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "3x3", timeoutTicks = 20)
    public static void generatorProducesEnergy(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TestModBlocks.GENERATOR.get());
        GeneratorBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
        machine.itemStorage().slot(GeneratorBlockEntity.FUEL_SLOT).set(Items.COAL, 1);
        helper.runAfterDelay(10, () -> {
            if (machine.energyStorage().getAmount() != GeneratorBlockEntity.GENERATION_RATE * 10) {
                helper.fail("Generator did not produce the expected energy", MACHINE_POS);
            } else {
                helper.succeed();
            }
        });
    }
}
