/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.advancedinscriber.block;

import javax.annotation.Nullable;

import dev.advancedinscriber.container.AdvancedInscriberContainer;
import dev.advancedinscriber.tile.AdvancedInscriberTileEntity;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;

public final class AdvancedInscriberBlock extends AEBaseTileBlock<AdvancedInscriberTileEntity> {

    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public AdvancedInscriberBlock(AbstractBlock.Properties properties) {
        super(properties);
        this.setTileEntity(AdvancedInscriberTileEntity.class, AdvancedInscriberTileEntity::new);
        this.setDefaultState(this.getDefaultState().with(WORKING, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(WORKING);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(BlockState currentState, AdvancedInscriberTileEntity tile) {
        return currentState.with(WORKING, tile.isWorking());
    }

    @Override
    public ActionResultType onActivated(World world, BlockPos pos, PlayerEntity player, Hand hand,
            @Nullable ItemStack heldItem, BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ActionResultType.PASS;
        }

        AdvancedInscriberTileEntity tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        if (!world.isRemote() && player instanceof ServerPlayerEntity) {
            SimpleNamedContainerProvider provider = new SimpleNamedContainerProvider(
                    (windowId, playerInventory, ignored) -> new AdvancedInscriberContainer(windowId, playerInventory,
                            tile),
                    new TranslationTextComponent("container.advancedinscriber.advanced_inscriber"));
            NetworkHooks.openGui((ServerPlayerEntity) player, provider, pos);
        }

        return ActionResultType.func_233537_a_(world.isRemote());
    }
}
