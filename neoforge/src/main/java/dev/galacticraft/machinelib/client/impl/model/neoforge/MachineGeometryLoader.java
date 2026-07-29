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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.machinelib.client.api.model.TextureProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge geometry loader keyed {@code machinelib:machine}. Reads the machine model JSON (the same
 * marker format the Fabric model resolver consumes) into a {@link MachineGeometry}.
 */
public final class MachineGeometryLoader implements IGeometryLoader<MachineGeometry> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("machinelib", "machine");
    public static final MachineGeometryLoader INSTANCE = new MachineGeometryLoader();

    private MachineGeometryLoader() {
    }

    @Override
    public MachineGeometry read(JsonObject json, JsonDeserializationContext context) {
        TextureProvider provider = TextureProvider.CODEC.decode(JsonOps.INSTANCE, json.get("data")).getOrThrow().getFirst();
        JsonElement baseElement = json.get("base");
        @Nullable ResourceLocation base = baseElement == null ? null : ResourceLocation.parse(baseElement.getAsString());
        return new MachineGeometry(provider, base);
    }
}
