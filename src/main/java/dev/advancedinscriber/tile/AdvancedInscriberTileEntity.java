/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Behavior adapted from AE2 Things 1.0.7 (MIT) to the real AE2 8.4.7 API.
 * Inventory, recipe and energy integration follows AE2 8.4.7 conventions.
 */
package dev.advancedinscriber.tile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.advancedinscriber.AdvancedInscriberMod;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.features.InscriberProcessType;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.core.Api;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.UpgradeInventory;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.misc.InscriberRecipes;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.item.AEItemStack;

public final class AdvancedInscriberTileEntity extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost, IConfigManagerHost {

    private static final int MAX_PROCESSING_TIME = 100;
    private static final int INTERNAL_POWER_CAPACITY = 1600;
    private static final int BASE_POWER_PER_TICK = 20;

    private final AppEngInternalInventory topInventory = new AppEngInternalInventory(this, 1, 64);
    private final AppEngInternalInventory bottomInventory = new AppEngInternalInventory(this, 1, 64);
    private final AppEngInternalInventory sideInventory = new AppEngInternalInventory(this, 2, 64);

    private final IItemHandlerModifiable internalInventory = new WrapperChainedItemHandler(
            this.topInventory, this.bottomInventory, this.sideInventory);

    private final IItemHandler externalInventory;
    private final Map<IItemHandler, ItemStack> lastStacks = new IdentityHashMap<>();
    private final UpgradeInventory upgrades;
    private final IConfigManager settings;

    @Nullable
    private InscriberRecipe cachedTask;
    private int processingTime;
    private boolean working;

    public AdvancedInscriberTileEntity() {
        super(AdvancedInscriberMod.ADVANCED_INSCRIBER_TILE.get());

        this.lastStacks.put(this.topInventory, ItemStack.EMPTY);
        this.lastStacks.put(this.bottomInventory, ItemStack.EMPTY);
        this.lastStacks.put(this.sideInventory, ItemStack.EMPTY);

        this.sideInventory.setMaxStackSize(1, 64);

        IAEItemFilter externalFilter = new ExternalInventoryFilter();
        IItemHandler topExternal = new WrapperFilteredItemHandler(this.topInventory, externalFilter);
        IItemHandler bottomExternal = new WrapperFilteredItemHandler(this.bottomInventory, externalFilter);
        IItemHandler sideExternal = new WrapperFilteredItemHandler(this.sideInventory, externalFilter);
        this.externalInventory = new WrapperChainedItemHandler(topExternal, bottomExternal, sideExternal);

        this.settings = new ConfigManager(this);
        this.upgrades = new UpgradeInventory(this, 5) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 5 : 0;
            }
        };

        this.getProxy().setValidSides(EnumSet.allOf(Direction.class));
        this.getProxy().setIdlePowerUsage(0);
        this.setPowerSides(EnumSet.allOf(Direction.class));
        this.setInternalMaxPower(INTERNAL_POWER_CAPACITY);
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation direction) {
        return AECableType.COVERED;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        this.upgrades.writeToNBT(data, "upgrades");
        this.settings.writeToNBT(data);
        data.putInt("processingTime", this.processingTime);
        data.putBoolean("working", this.working);
        return data;
    }

    @Override
    public void read(BlockState blockState, CompoundNBT data) {
        super.read(blockState, data);
        this.upgrades.readFromNBT(data, "upgrades");
        this.settings.readFromNBT(data);
        this.processingTime = Math.max(0, Math.min(MAX_PROCESSING_TIME, data.getInt("processingTime")));
        this.working = data.getBoolean("working");
        this.cachedTask = null;
        this.refreshLastStacks();
    }

    private void refreshLastStacks() {
        this.lastStacks.put(this.topInventory, this.topInventory.getStackInSlot(0).copy());
        this.lastStacks.put(this.bottomInventory, this.bottomInventory.getStackInSlot(0).copy());
        this.lastStacks.put(this.sideInventory, this.sideInventory.getStackInSlot(0).copy());
    }

    @Override
    protected boolean readFromStream(PacketBuffer data) throws IOException {
        boolean changed = super.readFromStream(data);
        int slotFlags = data.readUnsignedByte();
        boolean newWorking = (slotFlags & 64) != 0;
        changed |= this.working != newWorking;
        this.working = newWorking;

        for (int slot = 0; slot < this.internalInventory.getSlots(); slot++) {
            ItemStack oldStack = this.internalInventory.getStackInSlot(slot);
            ItemStack newStack = ItemStack.EMPTY;
            if ((slotFlags & (1 << slot)) != 0) {
                IAEItemStack aeStack = AEItemStack.fromPacket(data);
                if (aeStack != null) {
                    newStack = aeStack.createItemStack();
                }
            }
            if (!ItemStack.areItemStacksEqual(oldStack, newStack)) {
                changed = true;
            }
            this.internalInventory.setStackInSlot(slot, newStack);
        }

        this.cachedTask = null;
        this.refreshLastStacks();
        return changed;
    }

    @Override
    protected void writeToStream(PacketBuffer data) throws IOException {
        super.writeToStream(data);
        int slotFlags = this.working ? 64 : 0;
        for (int slot = 0; slot < this.internalInventory.getSlots(); slot++) {
            if (!this.internalInventory.getStackInSlot(slot).isEmpty()) {
                slotFlags |= 1 << slot;
            }
        }

        data.writeByte(slotFlags);
        for (int slot = 0; slot < this.internalInventory.getSlots(); slot++) {
            if ((slotFlags & (1 << slot)) != 0) {
                IAEItemStack stack = AEItemStack.fromItemStack(this.internalInventory.getStackInSlot(slot));
                if (stack != null) {
                    stack.writeToPacket(data);
                }
            }
        }
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (ItemStack upgrade : this.upgrades) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade);
            }
        }
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return this.internalInventory;
    }

    @Nonnull
    @Override
    protected IItemHandler getItemHandlerForSide(@Nonnull Direction side) {
        return this.externalInventory;
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        if (slot == 0 && this.lastStacks.containsKey(inventory)) {
            boolean wasEmpty = this.lastStacks.get(inventory).isEmpty();
            boolean isEmpty = inventory.getStackInSlot(0).isEmpty();
            this.lastStacks.put(inventory, inventory.getStackInSlot(0).copy());
            if (wasEmpty != isEmpty) {
                this.processingTime = 0;
            }
        }

        if (!this.working) {
            this.markForUpdate();
        }

        this.cachedTask = null;
        try {
            this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
        } catch (GridAccessException ignored) {
            // The machine may be placed before its grid node has joined a grid.
        }
    }

    @Nullable
    public InscriberRecipe getTask() {
        if (this.cachedTask == null && this.world != null) {
            ItemStack input = this.sideInventory.getStackInSlot(0).copy();
            if (input.isEmpty()) {
                return null;
            }
            input.setCount(1);

            ItemStack top = this.topInventory.getStackInSlot(0);
            ItemStack bottom = this.bottomInventory.getStackInSlot(0);
            this.cachedTask = InscriberRecipes.findRecipe(this.world, input, top, bottom, true);
        }
        return this.cachedTask;
    }

    private boolean hasWork() {
        if (this.getTask() != null) {
            return true;
        }
        this.processingTime = 0;
        return false;
    }

    private void updateWorkingState() {
        boolean shouldWork = this.hasWork();
        if (this.working != shouldWork) {
            this.working = shouldWork;
            this.markForUpdate();
        }
    }

    private boolean isInternalInventoryEmpty() {
        for (int slot = 0; slot < this.internalInventory.getSlots(); slot++) {
            if (!this.internalInventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        boolean hasOutput = !this.sideInventory.getStackInSlot(1).isEmpty();
        boolean sleep = !this.hasWork() && !hasOutput;
        return new TickingRequest(TickRates.Inscriber.getMin(), TickRates.Inscriber.getMax(), sleep, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        this.updateWorkingState();

        if (this.getTask() != null) {
            this.consumePowerAndAdvance(ticksSinceLastCall);
            this.tryFinishRecipe();
        }

        this.trySendOutputToNetwork();
        this.updateWorkingState();

        if (this.hasWork()) {
            return TickRateModulation.URGENT;
        }
        if (!this.sideInventory.getStackInSlot(1).isEmpty() || !this.isInternalInventoryEmpty()) {
            return TickRateModulation.SLOWER;
        }
        return TickRateModulation.SLEEP;
    }

    private void consumePowerAndAdvance(int ticksSinceLastCall) {
        try {
            IEnergyGrid gridEnergy = this.getProxy().getEnergy();
            IEnergySource source = this;
            int speedFactor = 1 + this.upgrades.getInstalledUpgrades(Upgrades.SPEED) * 3;
            int powerConsumption = BASE_POWER_PER_TICK * speedFactor;
            double threshold = powerConsumption - 0.01;

            double available = this.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            if (available <= threshold) {
                source = gridEnergy;
                available = gridEnergy.extractAEPower(powerConsumption, Actionable.SIMULATE,
                        PowerMultiplier.CONFIG);
            }

            if (available > threshold) {
                source.extractAEPower(powerConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
                int elapsedTicks = this.processingTime == 0 ? 1 : Math.max(1, ticksSinceLastCall);
                this.processingTime += elapsedTicks * speedFactor;
            }
        } catch (GridAccessException ignored) {
            // No connected grid means there is no external AE power source.
        }
    }

    private void tryFinishRecipe() {
        if (this.processingTime <= MAX_PROCESSING_TIME) {
            return;
        }

        this.processingTime = MAX_PROCESSING_TIME;
        InscriberRecipe recipe = this.getTask();
        if (recipe == null) {
            return;
        }

        ItemStack output = recipe.getOutput().copy();
        if (!this.sideInventory.insertItem(1, output, true).isEmpty()) {
            return;
        }

        this.sideInventory.insertItem(1, output, false);
        this.processingTime = 0;
        if (recipe.getProcessType() == InscriberProcessType.PRESS) {
            this.topInventory.extractItem(0, 1, false);
            this.bottomInventory.extractItem(0, 1, false);
        }
        this.sideInventory.extractItem(0, 1, false);
        this.cachedTask = null;
        this.saveChanges();
    }

    private void trySendOutputToNetwork() {
        ItemStack output = this.sideInventory.getStackInSlot(1);
        if (output.isEmpty() || output.getItem() == Items.AIR) {
            return;
        }

        try {
            IMEInventory<IAEItemStack> networkInventory = this.getProxy().getStorage()
                    .getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
            IAEItemStack offered = AEItemStack.fromItemStack(output);
            if (offered == null) {
                return;
            }

            long offeredAmount = offered.getStackSize();
            IAEItemStack remainder = networkInventory.injectItems(offered, Actionable.MODULATE,
                    new MachineSource(this));
            long remainderAmount = remainder == null ? 0 : remainder.getStackSize();
            int inserted = (int) Math.max(0, Math.min(Integer.MAX_VALUE, offeredAmount - remainderAmount));
            if (inserted > 0) {
                this.sideInventory.extractItem(1, inserted, false);
                this.saveChanges();
            }
        } catch (GridAccessException ignored) {
            // Keep the output in the exposed slot for a hopper, pipe or later retry.
        }
    }

    public boolean isWorking() {
        return this.working;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    public int getMaxProcessingTime() {
        return MAX_PROCESSING_TIME;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.settings;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("inv".equals(name)) {
            return this.internalInventory;
        }
        if ("upgrades".equals(name)) {
            return this.upgrades;
        }
        return null;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return this.upgrades.getInstalledUpgrades(upgrade);
    }

    @Override
    public void updateSetting(IConfigManager manager, Settings setting, Enum<?> newValue) {
    }

    private final class ExternalInventoryFilter implements IAEItemFilter {

        @Override
        public boolean allowExtract(IItemHandler inventory, int slot, int amount) {
            return inventory == AdvancedInscriberTileEntity.this.sideInventory && slot == 1;
        }

        @Override
        public boolean allowInsert(IItemHandler inventory, int slot, ItemStack stack) {
            if (inventory == AdvancedInscriberTileEntity.this.sideInventory && slot == 1) {
                return false;
            }
            if (AdvancedInscriberTileEntity.this.world == null || stack.isEmpty()) {
                return false;
            }

            boolean topOrBottom = inventory == AdvancedInscriberTileEntity.this.topInventory
                    || inventory == AdvancedInscriberTileEntity.this.bottomInventory;
            if (topOrBottom && Api.instance().definitions().materials().namePress().isSameAs(stack)) {
                return true;
            }

            ItemStack top = AdvancedInscriberTileEntity.this.topInventory.getStackInSlot(0);
            ItemStack bottom = AdvancedInscriberTileEntity.this.bottomInventory.getStackInSlot(0);
            ItemStack middle = AdvancedInscriberTileEntity.this.sideInventory.getStackInSlot(0);

            if (inventory == AdvancedInscriberTileEntity.this.topInventory) {
                top = stack;
            } else if (inventory == AdvancedInscriberTileEntity.this.bottomInventory) {
                bottom = stack;
            } else if (inventory == AdvancedInscriberTileEntity.this.sideInventory && slot == 0) {
                middle = stack;
            }

            if (inventory == AdvancedInscriberTileEntity.this.sideInventory
                    && (Api.instance().definitions().materials().namePress().isSameAs(top)
                            || Api.instance().definitions().materials().namePress().isSameAs(bottom))) {
                return true;
            }

            for (InscriberRecipe recipe : InscriberRecipes.getRecipes(AdvancedInscriberTileEntity.this.world)) {
                if (!middle.isEmpty() && !recipe.getMiddleInput().test(middle)) {
                    continue;
                }

                if (bottom.isEmpty() && top.isEmpty()) {
                    return true;
                }
                if (bottom.isEmpty()) {
                    if (recipe.getTopOptional().test(top) || recipe.getBottomOptional().test(top)) {
                        return true;
                    }
                } else if (top.isEmpty()) {
                    if (recipe.getBottomOptional().test(bottom) || recipe.getTopOptional().test(bottom)) {
                        return true;
                    }
                } else if (recipe.getTopOptional().test(top) && recipe.getBottomOptional().test(bottom)
                        || recipe.getBottomOptional().test(top) && recipe.getTopOptional().test(bottom)) {
                    return true;
                }
            }
            return false;
        }
    }
}
