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
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

import java.util.ArrayList;
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
    private static final ItemTransform THIRD_PERSON = transform(75, 45, 0, 0, 2.5f, 0, 0.375f);
    private static final ItemTransform FIRST_PERSON = transform(0, 135, 0, 0, 0, 0, 0.4f);
    private static final ItemTransform GUI = transform(30, 225, 0, 0, 0, 0, 0.625f);
    private static final ItemTransform GROUND = transform(0, 0, 0, 0, 3, 0, 0.25f);
    private static final ItemTransform FIXED = transform(0, 0, 0, 0, 0, 0, 0.5f);
    private static final ItemTransforms ITEM_TRANSFORMS = new ItemTransforms(
            THIRD_PERSON, THIRD_PERSON, FIRST_PERSON, FIRST_PERSON, ItemTransform.NO_TRANSFORM,
            GUI, GROUND, FIXED);

    /**
     * The machine's IO configuration, threaded from the block entity to the model at re-mesh time.
     */
    public static final ModelProperty<IOConfig> IO_CONFIG = new ModelProperty<>();

    private static final Map<Direction, BlockElementFace> CUBE_FACES = new EnumMap<>(Direction.class);
    private static final BlockElement CUBE_ELEMENT;
    private final @Nullable IOConfig itemConfig;
    private final ItemOverrides overrides;

    static {
        for (Direction direction : Direction.values()) {
            CUBE_FACES.put(direction, new BlockElementFace(direction, -1, "texture", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0)));
        }
        CUBE_ELEMENT = new BlockElement(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), CUBE_FACES, null, true);
    }

    public NeoForgeMachineBakedModel(TextureProvider.BoundTextureProvider provider, MachineTextureBase.Bound base) {
        this(provider, base, null, true);
    }

    private NeoForgeMachineBakedModel(TextureProvider.BoundTextureProvider provider, MachineTextureBase.Bound base,
                                      @Nullable IOConfig itemConfig, boolean resolveItemData) {
        super(provider, base);
        this.itemConfig = itemConfig;
        this.overrides = resolveItemData ? new MachineItemOverrides() : ItemOverrides.EMPTY;
    }

    private static ItemTransform transform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        return new ItemTransform(new Vector3f(rx, ry, rz), new Vector3f(tx, ty, tz),
                new Vector3f(scale, scale, scale));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return this.getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // Item rendering requests the unculled (null-side) quad list. Block rendering requests one
        // cull face at a time, so preserve the per-side path for placed machines.
        if (side == null) {
            if (state != null) return Collections.emptyList();
            List<BakedQuad> quads = new ArrayList<>(Direction.values().length);
            for (Direction direction : Direction.values()) {
                BakedQuad quad = bakeFace(null, direction, data);
                if (quad != null) quads.add(quad);
            }
            return quads;
        }
        BakedQuad quad = bakeFace(state, side, data);
        return quad == null ? Collections.emptyList() : List.of(quad);
    }

    private @Nullable BakedQuad bakeFace(@Nullable BlockState state, Direction side, ModelData data) {
        Direction facing = state != null ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        BlockFace face = BlockFace.from(facing, side);
        if (face == null) return null;
        IOConfig config = state == null && data.get(IO_CONFIG) == null ? this.itemConfig : data.get(IO_CONFIG);
        TextureAtlasSprite sprite = getSprite(state, face, config);
        return UnbakedGeometryHelper.bakeElementFace(CUBE_ELEMENT, CUBE_FACES.get(side), sprite, side, BlockModelRotation.X0_Y0);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            return modelData.derive().with(IO_CONFIG, machine.getIOConfig()).build();
        }
        return modelData;
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return ITEM_TRANSFORMS;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return this.overrides;
    }

    private final class MachineItemOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack,
                                  @Nullable net.minecraft.client.multiplayer.ClientLevel level,
                                  @Nullable net.minecraft.world.entity.LivingEntity entity, int seed) {
            CustomData customData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            IOConfig config = null;
            if (!customData.isEmpty() && customData.getUnsafe().contains(
                    dev.galacticraft.machinelib.impl.Constant.Nbt.CONFIGURATION, Tag.TAG_BYTE_ARRAY)) {
                config = new IOConfig();
                config.readTag(new ByteArrayTag(customData.getUnsafe().getByteArray(
                        dev.galacticraft.machinelib.impl.Constant.Nbt.CONFIGURATION)));
            }
            return new NeoForgeMachineBakedModel(provider, base, config, false);
        }
    }
}
