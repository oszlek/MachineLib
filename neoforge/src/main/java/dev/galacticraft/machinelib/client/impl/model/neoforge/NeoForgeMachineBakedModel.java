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

package dev.galacticraft.machinelib.client.impl.model.neoforge;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.configuration.IOConfig;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.machinelib.client.api.model.MachineTextureBase;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import dev.galacticraft.machinelib.client.impl.model.MachineBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge implementation of the dynamic machine model. Builds a per-face re-textured unit cube using
 * vanilla's face bakery, reading the machine's IO configuration from {@link ModelData} (supplied by
 * {@link #getModelData}). The face-to-sprite selection is inherited from {@link MachineBakedModel}.
 */
public final class NeoForgeMachineBakedModel extends MachineBakedModel implements IDynamicBakedModel {
    /**
     * The machine's IO configuration, threaded from the block entity to the model at re-mesh time.
     */
    public static final ModelProperty<IOConfig> IO_CONFIG = new ModelProperty<>();

    private static final Map<Direction, BlockElementFace> CUBE_FACES = new EnumMap<>(Direction.class);
    private static final BlockElement CUBE_ELEMENT;

    static {
        for (Direction direction : Direction.values()) {
            CUBE_FACES.put(direction, new BlockElementFace(direction, -1, "texture", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0)));
        }
        CUBE_ELEMENT = new BlockElement(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), CUBE_FACES, null, true);
    }

    public NeoForgeMachineBakedModel(TextureProvider.BoundTextureProvider provider, MachineTextureBase.Bound base) {
        super(provider, base);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (side == null) return Collections.emptyList();
        Direction facing = state != null ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        BlockFace face = BlockFace.from(facing, side);
        if (face == null) return Collections.emptyList();
        TextureAtlasSprite sprite = getSprite(state, face, data.get(IO_CONFIG));
        BakedQuad quad = UnbakedGeometryHelper.bakeElementFace(CUBE_ELEMENT, CUBE_FACES.get(side), sprite, side, BlockModelRotation.X0_Y0);
        return List.of(quad);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            return modelData.derive().with(IO_CONFIG, machine.getIOConfig()).build();
        }
        return modelData;
    }
}
