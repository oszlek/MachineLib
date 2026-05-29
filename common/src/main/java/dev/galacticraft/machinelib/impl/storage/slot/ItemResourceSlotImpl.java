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

package dev.galacticraft.machinelib.impl.storage.slot;
import dev.galacticraft.machinelib.impl.platform.MachineLibPlatform;

import dev.galacticraft.machinelib.api.filter.ResourceFilter;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.display.ItemSlotDisplay;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.machinelib.impl.util.Utils;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemResourceSlotImpl extends ResourceSlotImpl<Item> implements ItemResourceSlot {
    private static final String RECIPES_KEY = "Recipes";
    private final @Nullable ItemSlotDisplay display;

    private @Nullable Set<ResourceLocation> recipes = null;

    public ItemResourceSlotImpl(@NotNull TransferType transferType, @Nullable ItemSlotDisplay display, @NotNull ResourceFilter<Item> externalFilter, int capacity) {
        super(transferType, externalFilter, capacity);
        assert capacity > 0 && capacity <= 64;
        this.display = display;
    }

    @Override
    public long getRealCapacity() {
        assert this.isSane();
        return Math.min(this.capacity, this.resource == null ? 64 : this.resource.getDefaultMaxStackSize());
    }

    @Override
    public long getCapacityFor(@NotNull Item item, @NotNull DataComponentPatch components) {
        Optional<? extends Integer> optional = components.get(DataComponents.MAX_STACK_SIZE);
        if (optional != null && optional.isPresent()) {
            return optional.get();
        }
        return Math.min(this.capacity, item.getDefaultMaxStackSize());
    }

    @Override
    public @Nullable Item consumeOne() {
        DataComponentPatch tag = this.components;
        Item resource = this.extractOne();
        if (resource == null) return null;
        if (resource.hasCraftingRemainingItem()) {
            this.insertRemainder(resource, tag, 1);
        }
        return resource;
    }

    @Override
    public boolean consumeOne(@NotNull Item resource, @Nullable DataComponentPatch components) {
        DataComponentPatch actual = this.components;
        if (this.extractOne(resource, components)) {
            this.insertRemainder(resource, actual, 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long consume(long amount) {
        Item item = this.resource;
        if (item == null) return 0;
        DataComponentPatch components = this.components;
        long consumed = this.extract(amount);
        if (consumed > 0) {
            this.insertRemainder(item, components, 1);
            return consumed;
        }
        return consumed;
    }

    @Override
    public long consume(@NotNull Item resource, @Nullable DataComponentPatch components, long amount) {
        DataComponentPatch actual = this.components;
        long consumed = this.extract(resource, components, amount);
        if (consumed > 0) {
            this.insertRemainder(resource, actual, (int) consumed);
        }
        return consumed;
    }

    private void insertRemainder(@NotNull Item resource, @NotNull DataComponentPatch tag, int extracted) {
        if (resource.hasCraftingRemainingItem()) {
            if (this.isEmpty()) {
                ItemStack remainder = MachineLibPlatform.recipeRemainder(ItemStackUtil.of(resource, tag, extracted));
                if (!remainder.isEmpty()) {
                    this.insert(remainder.getItem(), remainder.getComponentsPatch(), remainder.getCount());
                }
            }
        }
    }

    @Override
    public @Nullable ItemSlotDisplay getDisplay() {
        return this.display;
    }

    @Override
    public @NotNull CompoundTag createTag() {
        CompoundTag tag = new CompoundTag();

        // If the slot is empty, return an empty tag
        if (this.isEmpty()) return tag;

        tag.putString(RESOURCE_KEY, BuiltInRegistries.ITEM.getKey(this.resource).toString());
        tag.putInt(AMOUNT_KEY, (int) this.amount);

        // Only write the components if we have components
        if (!this.components.isEmpty()) {
            tag.put(COMPONENTS_KEY, DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, this.components).getOrThrow());
        }

        // Only write the recipes if we have recipes
        if (this.transferMode() == TransferType.OUTPUT && this.recipes != null) {
            ListTag recipeTag = new ListTag();
            for (ResourceLocation entry : this.recipes) {
                recipeTag.add(StringTag.valueOf(entry.toString()));
            }
            tag.put(RECIPES_KEY, recipeTag);
        }
        return tag;
    }

    @Override
    public void readTag(@NotNull CompoundTag tag) {
        if (tag.isEmpty()) {
            this.setEmpty();
            return;
        }

        this.set(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(RESOURCE_KEY))),
                tag.contains(COMPONENTS_KEY) ? DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, tag.get(COMPONENTS_KEY)).getOrThrow() : DataComponentPatch.EMPTY,
                tag.getInt(AMOUNT_KEY)
        );

        if (this.transferMode() == TransferType.OUTPUT && tag.contains(RECIPES_KEY, Tag.TAG_COMPOUND)) {
            ListTag list = tag.getList(RECIPES_KEY, Tag.TAG_STRING);
            if (!list.isEmpty()) {
                this.recipes = new HashSet<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    this.recipes.add(ResourceLocation.parse(list.getString(i)));
                }
            }
        }

    }

    @Override
    public void writePacket(@NotNull RegistryFriendlyByteBuf buf) {
        if (this.amount > 0) {
            buf.writeInt((int) this.amount);
            buf.writeUtf(Utils.getShortId(BuiltInRegistries.ITEM.getKey(this.resource)));
            DataComponentPatch.STREAM_CODEC.encode(buf, this.components);
        } else {
            buf.writeInt(0);
        }
    }

    @Override
    public void readPacket(@NotNull RegistryFriendlyByteBuf buf) {
        int amount = buf.readInt();
        if (amount == 0) {
            this.setEmpty();
        } else {
            Item resource = BuiltInRegistries.ITEM.get(ResourceLocation.parse(buf.readUtf()));
            DataComponentPatch tag = DataComponentPatch.STREAM_CODEC.decode(buf);
            this.set(resource, tag, amount);
        }
    }

    @Override
    public boolean isSane() {
        return super.isSane() && this.resource != Items.AIR && this.amount <= Integer.MAX_VALUE;
    }

    @Override
    public @Nullable Set<ResourceLocation> takeRecipes() {
        Set<ResourceLocation> recipes1 = this.recipes;
        this.recipes = null;
        return recipes1;
    }

    @Override
    public void recipeCrafted(@NotNull ResourceLocation id) {
        if (this.recipes == null) {
            if (this.transferMode() == TransferType.OUTPUT) {
                this.recipes = new HashSet<>();
            } else {
                return;
            }
        }
        this.recipes.add(id);
    }
}
