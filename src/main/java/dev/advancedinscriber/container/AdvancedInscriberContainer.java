/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.advancedinscriber.container;

import dev.advancedinscriber.AdvancedInscriberMod;
import dev.advancedinscriber.tile.AdvancedInscriberTileEntity;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

import appeng.api.definitions.IItemDefinition;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.OutputSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.core.Api;
import appeng.tile.misc.InscriberRecipes;

public final class AdvancedInscriberContainer extends UpgradeableContainer implements IProgressProvider {

    private final AdvancedInscriberTileEntity tile;
    private final Slot topSlot;
    private final Slot middleSlot;
    private final Slot bottomSlot;

    @GuiSync(2)
    public int maxProcessingTime = -1;

    @GuiSync(3)
    public int processingTime = -1;

    public AdvancedInscriberContainer(int windowId, PlayerInventory playerInventory,
            AdvancedInscriberTileEntity tile) {
        super(AdvancedInscriberMod.ADVANCED_INSCRIBER_CONTAINER.get(), windowId, playerInventory, tile);
        this.tile = tile;

        IItemHandler inventory = tile.getInternalInventory();
        this.topSlot = this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.INSCRIBER_PLATE, inventory, 0),
                SlotSemantic.INSCRIBER_PLATE_TOP);
        this.bottomSlot = this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.INSCRIBER_PLATE, inventory, 1),
                SlotSemantic.INSCRIBER_PLATE_BOTTOM);
        this.middleSlot = this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.INSCRIBER_INPUT, inventory, 2),
                SlotSemantic.MACHINE_INPUT);
        this.addSlot(new OutputSlot(inventory, 3, null), SlotSemantic.MACHINE_OUTPUT);
    }

    public static AdvancedInscriberContainer fromNetwork(int windowId, PlayerInventory playerInventory,
            PacketBuffer data) {
        BlockPos pos = data.readBlockPos();
        TileEntity tile = playerInventory.player.world.getTileEntity(pos);
        if (!(tile instanceof AdvancedInscriberTileEntity)) {
            throw new IllegalStateException("Advanced Inscriber tile entity is missing at " + pos);
        }
        return new AdvancedInscriberContainer(windowId, playerInventory, (AdvancedInscriberTileEntity) tile);
    }

    @Override
    protected void setupConfig() {
        this.setupUpgrades();
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 5;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServer()) {
            this.maxProcessingTime = this.tile.getMaxProcessingTime();
            this.processingTime = this.tile.getProcessingTime();
        }
        this.standardDetectAndSendChanges();
    }

    @Override
    public boolean isValidForSlot(Slot slot, ItemStack stack) {
        IItemHandler inventory = this.tile.getInternalInventory();
        ItemStack top = inventory.getStackInSlot(0);
        ItemStack bottom = inventory.getStackInSlot(1);

        if (slot == this.middleSlot) {
            IItemDefinition namePress = Api.instance().definitions().materials().namePress();
            if (namePress.isSameAs(top) || namePress.isSameAs(bottom)) {
                return !namePress.isSameAs(stack);
            }
            return InscriberRecipes.findRecipe(this.tile.getWorld(), stack, top, bottom, false) != null;
        }

        if (slot == this.topSlot && !bottom.isEmpty() || slot == this.bottomSlot && !top.isEmpty()) {
            ItemStack other = slot == this.topSlot ? this.bottomSlot.getStack() : this.topSlot.getStack();
            IItemDefinition namePress = Api.instance().definitions().materials().namePress();
            if (namePress.isSameAs(other)) {
                return namePress.isSameAs(stack);
            }
            return InscriberRecipes.isValidOptionalIngredientCombination(this.tile.getWorld(), stack, other);
        }
        return true;
    }

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProcessingTime;
    }
}
