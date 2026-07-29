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

package dev.galacticraft.machinelib.api.util.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** NeoForge adjacent sided-capability cache matching the Fabric utility's behavior. */
public final class AdjacentBlockApiCache<T> {
    private final Map<Direction, BlockCapabilityCache<T, @Nullable Direction>> caches;

    private AdjacentBlockApiCache(BlockCapability<T, @Nullable Direction> capability,
                                  ServerLevel level, BlockPos origin) {
        this.caches = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            this.caches.put(direction, BlockCapabilityCache.create(capability, level,
                    origin.relative(direction), direction.getOpposite()));
        }
    }

    public static <T> AdjacentBlockApiCache<T> create(BlockCapability<T, @Nullable Direction> capability,
                                                       ServerLevel level, BlockPos origin) {
        return new AdjacentBlockApiCache<>(capability, level, origin);
    }

    public @Nullable T find(Direction direction) {
        return this.caches.get(direction).getCapability();
    }
}
