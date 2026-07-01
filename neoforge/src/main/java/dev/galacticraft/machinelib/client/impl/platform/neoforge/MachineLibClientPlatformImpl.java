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

package dev.galacticraft.machinelib.client.impl.platform.neoforge;

import dev.galacticraft.machinelib.client.impl.model.MachineBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge implementation of the client-side fluid metadata/rendering and model-unwrap hooks, backed
 * by {@link IClientFluidTypeExtensions} and the fluid type system.
 */
public final class MachineLibClientPlatformImpl {
    private MachineLibClientPlatformImpl() {
    }

    private static FluidStack stackOf(Fluid fluid, DataComponentPatch components) {
        FluidStack stack = new FluidStack(fluid, 1);
        stack.applyComponents(components);
        return stack;
    }

    public static Component fluidName(Fluid fluid, DataComponentPatch components) {
        return stackOf(fluid, components).getHoverName();
    }

    public static @Nullable TextureAtlasSprite fluidSprite(Fluid fluid, DataComponentPatch components) {
        ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture(stackOf(fluid, components));
        return still == null ? null : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
    }

    public static int fluidColor(Fluid fluid, DataComponentPatch components) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor(stackOf(fluid, components));
    }

    public static boolean fluidLighterThanAir(Fluid fluid, DataComponentPatch components) {
        return stackOf(fluid, components).getFluidType().isLighterThanAir();
    }

    public static @Nullable MachineBakedModel machineModel(BakedModel model) {
        return model instanceof MachineBakedModel machine ? machine : null;
    }
}
