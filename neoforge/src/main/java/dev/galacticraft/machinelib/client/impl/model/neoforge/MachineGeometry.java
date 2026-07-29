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

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.machinelib.client.api.model.MachineTextureBase;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import dev.galacticraft.machinelib.client.impl.model.MachineModelRegistryImpl;
import dev.galacticraft.machinelib.impl.MachineLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * NeoForge unbaked geometry for a single machine model. Decodes the per-machine {@link TextureProvider}
 * and its base reference from the model JSON (via the {@code machinelib:machine} geometry loader) and,
 * at bake time, binds both to atlas sprites and produces a {@link NeoForgeMachineBakedModel}.
 *
 * <p>The base texture set is loaded on demand from the resource manager (and cached) so baking does not
 * depend on client reload-listener ordering.
 */
public final class MachineGeometry implements IUnbakedGeometry<MachineGeometry> {
    private static final Map<ResourceLocation, MachineTextureBase> BASE_CACHE = new ConcurrentHashMap<>();

    private final TextureProvider provider;
    private final @Nullable ResourceLocation baseId;

    public MachineGeometry(TextureProvider provider, @Nullable ResourceLocation baseId) {
        this.provider = provider;
        this.baseId = baseId;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        ResourceLocation base = this.baseId != null ? this.baseId : defaultBase(context);
        MachineTextureBase textureBase = loadBase(base);
        return new NeoForgeMachineBakedModel(this.provider.bind(spriteGetter), textureBase.bind(spriteGetter));
    }

    private static ResourceLocation defaultBase(IGeometryBakingContext context) {
        ResourceLocation model = ResourceLocation.tryParse(context.getModelName());
        String namespace = model != null ? model.getNamespace() : "machinelib";
        return ResourceLocation.fromNamespaceAndPath(namespace, MachineModelRegistryImpl.DEFAULT_MACHINE_BASE);
    }

    private static MachineTextureBase loadBase(ResourceLocation baseId) {
        return BASE_CACHE.computeIfAbsent(baseId, id -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json");
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                MachineLib.LOGGER.error("Missing machine base texture model: {}", file);
                return MachineTextureBase.prefixed(id.getNamespace(), "block/machine");
            }
            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                return MachineTextureBase.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load machine base texture model " + file, ex);
            }
        });
    }
}
