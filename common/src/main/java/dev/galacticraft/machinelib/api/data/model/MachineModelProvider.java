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

package dev.galacticraft.machinelib.api.data.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.galacticraft.machinelib.api.block.MachineBlock;
import dev.galacticraft.machinelib.client.api.model.MachineTextureBase;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import dev.galacticraft.machinelib.client.impl.data.model.MachineModelData;
import dev.galacticraft.machinelib.client.impl.data.model.MachineTextureBaseData;
import dev.galacticraft.machinelib.client.impl.model.MachineModelRegistryImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A loader-neutral {@link DataProvider} that generates the JSON assets for machine blocks: the shared
 * base texture set, per-machine models (in the {@code machinelib:machine} marker format), the machine
 * block states, and item models.
 * <p>
 * Unlike the Fabric-only {@code MachineModelGenerator} (which drives Fabric's access-widened
 * {@code BlockModelGenerators}), this provider writes the JSON directly through the vanilla data
 * pipeline, so it works identically on Fabric and NeoForge. Register it from each loader's datagen
 * entry point (Fabric {@code DataGeneratorEntrypoint}, NeoForge {@code GatherDataEvent}).
 * <p>
 * Subclasses implement {@link #generate(Output)} and call the helpers on the supplied {@link Output}.
 */
public abstract class MachineModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider blockStates;

    protected MachineModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
    }

    /**
     * {@return the machine model location for a block} ({@code <namespace>:machine/<path>}).
     */
    public static ResourceLocation machineModelLocation(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).withPrefix("machine/");
    }

    /**
     * Generates the machine assets. Call the {@link Output} helpers to emit base texture sets and
     * machines.
     */
    protected abstract void generate(Output output);

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        this.generate(new Output() {
            @Override
            public void base(ResourceLocation baseId, MachineTextureBase base) {
                futures.add(DataProvider.saveStable(cachedOutput, new MachineTextureBaseData(base).get(), models.json(baseId)));
            }

            @Override
            public void base(String namespace, MachineTextureBase base) {
                this.base(ResourceLocation.fromNamespaceAndPath(namespace, MachineModelRegistryImpl.DEFAULT_MACHINE_BASE), base);
            }

            @Override
            public ResourceLocation machineModel(ResourceLocation modelId, @Nullable ResourceLocation base, TextureProvider textures) {
                futures.add(DataProvider.saveStable(cachedOutput, new MachineModelData(base, textures).get(), models.json(modelId)));
                return modelId;
            }

            @Override
            public void machine(Block block, @Nullable ResourceLocation base, TextureProvider textures) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                ResourceLocation modelId = blockId.withPrefix("machine/");
                JsonElement model = new MachineModelData(base, textures).get();
                futures.add(DataProvider.saveStable(cachedOutput, model, models.json(modelId)));
                // Reuse the machine model as the item model so the machine renders in hand/inventory on
                // NeoForge (the geometry loader bakes it); on Fabric the model resolver supplies it.
                futures.add(DataProvider.saveStable(cachedOutput, model, models.json(blockId.withPrefix("item/"))));
                futures.add(DataProvider.saveStable(cachedOutput, singleVariantBlockState(modelId), blockStates.json(blockId)));
            }

            @Override
            public void machineWithActive(Block block, @Nullable ResourceLocation base, TextureProvider inactive, TextureProvider active) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                ResourceLocation inactiveId = blockId.withPrefix("machine/");
                ResourceLocation activeId = inactiveId.withSuffix("_active");
                JsonElement inactiveModel = new MachineModelData(base, inactive).get();
                futures.add(DataProvider.saveStable(cachedOutput, inactiveModel, models.json(inactiveId)));
                futures.add(DataProvider.saveStable(cachedOutput, new MachineModelData(base, active).get(), models.json(activeId)));
                futures.add(DataProvider.saveStable(cachedOutput, inactiveModel, models.json(blockId.withPrefix("item/"))));
                futures.add(DataProvider.saveStable(cachedOutput, activeBlockState(inactiveId, activeId), blockStates.json(blockId)));
            }
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonElement singleVariantBlockState(ResourceLocation model) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model.toString());
        JsonObject variants = new JsonObject();
        variants.add("", variant);
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private static JsonElement activeBlockState(ResourceLocation inactive, ResourceLocation active) {
        String property = MachineBlock.ACTIVE.getName();
        JsonObject off = new JsonObject();
        off.addProperty("model", inactive.toString());
        JsonObject on = new JsonObject();
        on.addProperty("model", active.toString());
        JsonObject variants = new JsonObject();
        variants.add(property + "=false", off);
        variants.add(property + "=true", on);
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    @Override
    public String getName() {
        return "Machine Models";
    }

    /**
     * Emitter for machine assets, supplied to {@link #generate(Output)}.
     */
    public interface Output {
        /**
         * Emits a base texture set at the given model id.
         */
        void base(ResourceLocation baseId, MachineTextureBase base);

        /**
         * Emits a base texture set at {@code <namespace>:machine/base}.
         */
        void base(String namespace, MachineTextureBase base);

        /**
         * Emits a standalone machine model (no block state or item model) at the given id.
         */
        ResourceLocation machineModel(ResourceLocation modelId, @Nullable ResourceLocation base, TextureProvider textures);

        /**
         * Emits a machine model, item model, and single-variant block state for the block.
         */
        void machine(Block block, @Nullable ResourceLocation base, TextureProvider textures);

        /**
         * Emits inactive/active machine models, an item model, and a block state dispatching on
         * {@link MachineBlock#ACTIVE}.
         */
        void machineWithActive(Block block, @Nullable ResourceLocation base, TextureProvider inactive, TextureProvider active);

        /**
         * Emits a machine model, item model, and single-variant block state using the default base.
         */
        default void machine(Block block, TextureProvider textures) {
            this.machine(block, null, textures);
        }

        /**
         * Emits inactive/active models + block state using the default base.
         */
        default void machineWithActive(Block block, TextureProvider inactive, TextureProvider active) {
            this.machineWithActive(block, null, inactive, active);
        }
    }
}
