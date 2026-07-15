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

package dev.galacticraft.machinelib.testmod.menu;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.menu.SynchronizedMenuType;
import dev.galacticraft.machinelib.testmod.Constant;
import dev.galacticraft.machinelib.testmod.block.entity.GeneratorBlockEntity;
import dev.galacticraft.machinelib.testmod.block.entity.MelterBlockEntity;
import dev.galacticraft.machinelib.testmod.block.entity.MixerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class TestModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Constant.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<MachineMenu<GeneratorBlockEntity>>> GENERATOR = MENUS.register(Constant.GENERATOR,
            () -> SynchronizedMenuType.<GeneratorBlockEntity, MachineMenu<GeneratorBlockEntity>>createSimple(MachineMenu::new));
    public static final RegistrySupplier<MenuType<MachineMenu<MixerBlockEntity>>> MIXER = MENUS.register(Constant.MIXER,
            () -> SynchronizedMenuType.<MixerBlockEntity, MachineMenu<MixerBlockEntity>>createSimple(MachineMenu::new));
    public static final RegistrySupplier<MenuType<MachineMenu<MelterBlockEntity>>> MELTER = MENUS.register(Constant.MELTER,
            () -> SynchronizedMenuType.<MelterBlockEntity, MachineMenu<MelterBlockEntity>>createSimple(MachineMenu::new));

    private TestModMenuTypes() {
    }
}
