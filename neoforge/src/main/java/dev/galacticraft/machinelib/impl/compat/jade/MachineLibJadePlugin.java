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

package dev.galacticraft.machinelib.impl.compat.jade;

import com.mojang.authlib.GameProfile;
import dev.galacticraft.machinelib.api.block.MachineBlock;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.block.entity.RecipeMachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.configuration.RedstoneMode;
import dev.galacticraft.machinelib.api.machine.configuration.SecuritySettings;
import dev.galacticraft.machinelib.impl.Constant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

import java.util.List;
import java.util.Optional;

/** Jade equivalent of MachineLib's Fabric WTHIT integration. */
@WailaPlugin
public final class MachineLibJadePlugin implements IWailaPlugin {
    private static final ResourceLocation INFO = Constant.id("machine_info");
    private static final ResourceLocation PROGRESS = Constant.id("machine_progress");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, MachineBlock.class);
        registration.registerProgress(Progress.INSTANCE, RecipeMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ClientComponent.INSTANCE, MachineBlock.class);
    }

    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof MachineBlockEntity machine) {
                data.put("security", machine.getSecurity().createTag());
                data.put("redstone", machine.getRedstoneMode().createTag());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return INFO;
        }
    }

    private enum ClientComponent implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("redstone") || !data.contains("security")) return;
            RedstoneMode redstone = RedstoneMode.readTag(data.get("redstone"));
            SecuritySettings security = new SecuritySettings();
            security.readTag(data.getCompound("security"));
            tooltip.add(Component.translatable(Constant.TranslationKey.REDSTONE_MODE_TOOLTIP,
                    redstone.getName()).setStyle(Constant.Text.RED_STYLE));
            if (security.getOwner() != null) {
                Optional<GameProfile> profile = SkullBlockEntity.fetchGameProfile(security.getOwner()).getNow(null);
                if (profile != null && profile.isPresent()) {
                    tooltip.add(Component.translatable(Constant.TranslationKey.OWNER_TOOLTIP,
                            Component.literal(profile.get().getName()).setStyle(Constant.Text.WHITE_STYLE))
                            .setStyle(Constant.Text.AQUA_STYLE));
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return INFO;
        }
    }

    private enum Progress implements IServerExtensionProvider<CompoundTag> {
        INSTANCE;

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (accessor.getTarget() instanceof RecipeMachineBlockEntity machine
                    && machine.getActiveRecipe() != null) {
                return List.of(new ViewGroup<>(List.of(ProgressView.create(machine.getProgressRatio()))));
            }
            return List.of();
        }

        @Override
        public ResourceLocation getUid() {
            return PROGRESS;
        }
    }
}
