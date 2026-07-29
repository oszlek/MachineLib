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

package dev.galacticraft.machinelib.testmod.data;

import dev.galacticraft.machinelib.api.data.model.MachineModelProvider;
import dev.galacticraft.machinelib.client.api.model.MachineTextureBase;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import dev.galacticraft.machinelib.testmod.Constant;
import dev.galacticraft.machinelib.testmod.block.TestModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.world.level.block.Blocks;

/**
 * Generates the testmod's machine models, block states, and item models through the loader-neutral
 * {@link MachineModelProvider} — the same code path a NeoForge {@code GatherDataEvent} would use.
 */
public class TestModMachineModelProvider extends MachineModelProvider {
    public TestModMachineModelProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void generate(Output output) {
        output.base(Constant.MOD_ID, MachineTextureBase.prefixed(Constant.MOD_ID, "block/machine"));

        output.machineWithActive(TestModBlocks.MELTER,
                TextureProvider.builder()
                        .front(TextureMapping.getBlockTexture(Blocks.BLAST_FURNACE, "_front"))
                        .back(TextureMapping.getBlockTexture(Blocks.BLAST_FURNACE, "_front"))
                        .build(),
                TextureProvider.builder()
                        .front(TextureMapping.getBlockTexture(Blocks.BLAST_FURNACE, "_front_on"))
                        .back(TextureMapping.getBlockTexture(Blocks.BLAST_FURNACE, "_front_on"))
                        .build());

        output.machine(TestModBlocks.GENERATOR, TextureProvider.builder()
                .front(TextureMapping.getBlockTexture(Blocks.FURNACE, "_front"))
                .topOverride(TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
                .build());

        output.machine(TestModBlocks.MIXER, TextureProvider.none());
    }
}
