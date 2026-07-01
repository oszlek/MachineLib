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

package dev.galacticraft.machinelib.client.impl.model;

import dev.galacticraft.machinelib.api.machine.configuration.IOConfig;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.machinelib.client.api.model.MachineTextureBase;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Loader-neutral base for the dynamic machine block model. Holds the bound base and per-machine
 * sprites and implements the IO-configuration → per-face sprite selection shared by every loader.
 * <p>
 * Concrete subclasses supply the loader-specific render pipeline (Fabric's mesh emitter, NeoForge's
 * {@code IDynamicBakedModel#getQuads}) and item transforms.
 */
@ApiStatus.Internal
public abstract class MachineBakedModel implements BakedModel {
    protected final TextureProvider.BoundTextureProvider provider;
    protected final MachineTextureBase.Bound base;

    protected MachineBakedModel(TextureProvider.BoundTextureProvider provider, MachineTextureBase.Bound base) {
        this.provider = provider;
        this.base = base;
    }

    /**
     * {@return the sprite that should texture the given face, honoring the machine's IO configuration}
     *
     * @param state  the block state, or {@code null} when rendering an item
     * @param face   the logical machine face
     * @param config the machine's IO configuration, or {@code null} if unavailable
     */
    public TextureAtlasSprite getSprite(@Nullable BlockState state, @NotNull BlockFace face, @Nullable IOConfig config) {
        if (config != null) {
            IOFace ioFace = config.get(face);
            ResourceType type = ioFace.getType();
            if (type != ResourceType.NONE) {
                ResourceFlow flow = ioFace.getFlow();

                switch (flow) {
                    case INPUT -> {
                        switch (type) {
                            case ENERGY -> {
                                return this.base.machineEnergyIn();
                            }
                            case ITEM -> {
                                return this.base.machineItemIn();
                            }
                            case FLUID -> {
                                return this.base.machineFluidIn();
                            }
                            case ANY -> {
                                return this.base.machineAnyIn();
                            }
                        }
                    }
                    case OUTPUT -> {
                        switch (type) {
                            case ENERGY -> {
                                return this.base.machineEnergyOut();
                            }
                            case ITEM -> {
                                return this.base.machineItemOut();
                            }
                            case FLUID -> {
                                return this.base.machineFluidOut();
                            }
                            case ANY -> {
                                return this.base.machineAnyOut();
                            }
                        }
                    }
                    case BOTH -> {
                        switch (type) {
                            case ENERGY -> {
                                return this.base.machineEnergyBoth();
                            }
                            case ITEM -> {
                                return this.base.machineItemBoth();
                            }
                            case FLUID -> {
                                return this.base.machineFluidBoth();
                            }
                            case ANY -> {
                                return this.base.machineAnyBoth();
                            }
                        }
                    }
                }
            }
        }

        if (state == null) {
            TextureAtlasSprite override = this.provider.getItemOverride(face);
            if (override != null) return override;
        }
        TextureAtlasSprite sprite = this.provider.getSprite(face);
        return sprite == null ? this.base.base() : sprite;
    }

    /**
     * {@return the sprite for the given face as shown on the configuration GUI's face buttons}.
     */
    public TextureAtlasSprite getItemOverride(@Nullable BlockState state, @NotNull BlockFace face, @Nullable IOConfig config) {
        TextureAtlasSprite override = this.provider.getItemOverride(face);
        return override != null ? override : this.getSprite(state, face, config);
    }

    public TextureProvider.BoundTextureProvider getProvider() {
        return this.provider;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, @NotNull RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        TextureAtlasSprite particle = this.provider.getParticle();
        return particle != null ? particle : this.base.base();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
