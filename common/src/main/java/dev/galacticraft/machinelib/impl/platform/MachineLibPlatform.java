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

package dev.galacticraft.machinelib.impl.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.galacticraft.machinelib.api.block.entity.BaseBlockEntity;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.menu.MenuData;
import dev.galacticraft.machinelib.api.menu.SynchronizedMenu;
import dev.galacticraft.machinelib.api.menu.SynchronizedMenuType;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platform bridge for the loader-specific behavior that {@code common} block entities and blocks rely on:
 * native capability exposure, item &lt;-&gt; machine transfer, menu opening and network sends.
 * <p>
 * Each loader supplies the implementation via Architectury {@link ExpectPlatform}.
 */
@ApiStatus.Internal
public final class MachineLibPlatform {
    private MachineLibPlatform() {
    }

    /**
     * Registers the item/fluid/energy storages of every machine of the given block-entity type as the
     * platform's native capability, honoring the machine's per-side IO configuration.
     *
     * @param type the machine block-entity type
     */
    @ExpectPlatform
    public static void registerMachineProviders(@NotNull BlockEntityType<? extends MachineBlockEntity> type) {
        throw new AssertionError();
    }

    /**
     * Extracts energy from the item in the given slot into the machine's energy storage.
     */
    @ExpectPlatform
    public static void chargeFromItem(@NotNull MachineItemStorage items, int slot, @NotNull MachineEnergyStorage energy) {
        throw new AssertionError();
    }

    /**
     * Inserts energy from the machine's energy storage into the item in the given slot.
     */
    @ExpectPlatform
    public static void drainPowerToItem(@NotNull MachineItemStorage items, int slot, @NotNull MachineEnergyStorage energy) {
        throw new AssertionError();
    }

    /**
     * Extracts fluid from the item in the given slot into the tank. If {@code fluid} is {@code null}, any fluid the
     * tank accepts is moved.
     */
    @ExpectPlatform
    public static void takeFluidFromItem(@NotNull MachineItemStorage items, int slot, @NotNull FluidResourceSlot tank, @Nullable Fluid fluid) {
        throw new AssertionError();
    }

    /**
     * Inserts fluid from the tank into the item in the given slot.
     */
    @ExpectPlatform
    public static void drainFluidToItem(@NotNull MachineItemStorage items, int slot, @NotNull FluidResourceSlot tank) {
        throw new AssertionError();
    }

    /**
     * Opens the menu for the given block entity for the player, sending any extra sync data the menu requires.
     */
    @ExpectPlatform
    public static void openMenu(@NotNull ServerPlayer player, @NotNull BaseBlockEntity be) {
        throw new AssertionError();
    }

    /**
     * Sends a custom payload to the given player.
     */
    @ExpectPlatform
    public static void sendToPlayer(@NotNull ServerPlayer player, @NotNull CustomPacketPayload payload) {
        throw new AssertionError();
    }

    /**
     * Creates the platform's server-side menu synchronization data holder for the given player.
     */
    @ExpectPlatform
    public static @NotNull MenuData createMenuData(@NotNull ServerPlayer player, int syncId) {
        throw new AssertionError();
    }

    /**
     * Creates the platform's client-side menu synchronization data holder.
     */
    @ExpectPlatform
    public static @NotNull MenuData createMenuDataClient(int syncId) {
        throw new AssertionError();
    }

    /**
     * Creates the registered {@link MenuType} for a synchronized machine menu, wiring the extra
     * block-position sync data the menu requires (Fabric: an {@code ExtendedScreenHandlerType}).
     */
    @ExpectPlatform
    public static <BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> @NotNull MenuType<Menu> createMenuType(@NotNull SynchronizedMenuType.Factory<BE, Menu> factory) {
        throw new AssertionError();
    }

    /**
     * Appends the fluid tooltip lines for the given tank contents (loader-specific fluid naming).
     */
    @ExpectPlatform
    public static void fluidTooltip(@NotNull List<Component> out, @Nullable Fluid fluid, @NotNull DataComponentPatch components, long amount, long capacity) {
        throw new AssertionError();
    }

    /**
     * Wraps the given text component to the given pixel width, splitting it into lines (client font).
     */
    @ExpectPlatform
    public static @NotNull List<Component> wrapText(@NotNull Component text, int width) {
        throw new AssertionError();
    }

    /**
     * {@return the crafting/recipe remainder for the given stack} (Fabric: the stack-aware
     * {@code FabricItem#getRecipeRemainder}; NeoForge: {@code ItemStack#getCraftingRemainingItem}).
     */
    @ExpectPlatform
    public static @NotNull ItemStack recipeRemainder(@NotNull ItemStack stack) {
        throw new AssertionError();
    }
}
