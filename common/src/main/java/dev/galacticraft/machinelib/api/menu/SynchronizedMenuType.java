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

package dev.galacticraft.machinelib.api.menu;

import dev.galacticraft.machinelib.api.block.entity.BaseBlockEntity;
import dev.galacticraft.machinelib.impl.platform.MachineLibPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Factory helpers for creating {@link MenuType}s for synchronized machine menus.
 *
 * <p>The actual loader-specific extended menu type — which carries the extra {@link BlockPos}
 * opening data and calls {@link SynchronizedMenu#registerData} on the freshly created menu — is
 * produced by the platform via {@link MachineLibPlatform#createMenuType}.
 */
public final class SynchronizedMenuType {
    private SynchronizedMenuType() {
    }

    @Contract("_ -> new")
    public static <BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> @NotNull MenuType<Menu> create(Factory<BE, Menu> factory) {
        return MachineLibPlatform.createMenuType(factory);
    }

    @Contract("_ -> new")
    public static <BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> @NotNull MenuType<Menu> createSimple(InventoryFactory<BE, Menu> factory) {
        return create(factory, 8, 84);
    }

    @Contract("_, _ -> new")
    public static <BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> @NotNull MenuType<Menu> create(InventoryFactory<BE, Menu> factory, int invY) {
        return create(factory, 8, invY);
    }

    @Contract("_, _, _ -> new")
    public static <BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> @NotNull MenuType<Menu> create(InventoryFactory<BE, Menu> factory, int invX, int invY) {
        Factory<BE, Menu> delegate = (type, syncId, inventory, pos) -> factory.create(type, syncId, inventory, pos, invX, invY);
        return create(delegate);
    }

    /**
     * A factory for creating machine menus.
     *
     * @param <BE> The type of machine block entity.
     * @param <Menu> The type of machine menu.
     */
    @FunctionalInterface
    public interface Factory<BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> {
        /**
         * Creates a new menu.
         *
         * @param type the menu type
         * @param syncId the synchronization ID of the menu
         * @param inventory the player's inventory
         * @param pos the position of the block being opened
         * @return the created menu
         */
        Menu create(MenuType<Menu> type, int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos);
    }

    @FunctionalInterface
    public interface InventoryFactory<BE extends BaseBlockEntity, Menu extends SynchronizedMenu<BE>> {
        Menu create(MenuType<Menu> type, int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos, int invX, int invY);
    }
}
