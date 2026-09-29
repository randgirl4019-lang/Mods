/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This backport is based on the MIT-licensed Advanced Inscriber from
 * AE2 Things 1.0.7 and the LGPL-licensed AE2 8.4.7 inscriber implementation.
 */
package dev.advancedinscriber;

import dev.advancedinscriber.block.AdvancedInscriberBlock;
import dev.advancedinscriber.container.AdvancedInscriberContainer;
import dev.advancedinscriber.tile.AdvancedInscriberTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.config.Upgrades;
import appeng.block.AEBaseBlock;

@Mod(AdvancedInscriberMod.MOD_ID)
public final class AdvancedInscriberMod {

    public static final String MOD_ID = "advancedinscriber";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister
            .create(ForgeRegistries.TILE_ENTITIES, MOD_ID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister
            .create(ForgeRegistries.CONTAINERS, MOD_ID);

    public static final RegistryObject<AdvancedInscriberBlock> ADVANCED_INSCRIBER = BLOCKS.register(
            "advanced_inscriber",
            () -> new AdvancedInscriberBlock(AEBaseBlock.defaultProps(Material.IRON)
                    .hardnessAndResistance(4.0F, 11.0F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)
                    .setRequiresTool()));

    public static final RegistryObject<Item> ADVANCED_INSCRIBER_ITEM = ITEMS.register(
            "advanced_inscriber",
            () -> new BlockItem(ADVANCED_INSCRIBER.get(), new Item.Properties().group(ItemGroup.REDSTONE)));

    public static final RegistryObject<TileEntityType<AdvancedInscriberTileEntity>> ADVANCED_INSCRIBER_TILE = TILE_ENTITIES
            .register("advanced_inscriber",
                    () -> TileEntityType.Builder
                            .create(AdvancedInscriberTileEntity::new, ADVANCED_INSCRIBER.get())
                            .build(null));

    public static final RegistryObject<ContainerType<AdvancedInscriberContainer>> ADVANCED_INSCRIBER_CONTAINER = CONTAINERS
            .register("advanced_inscriber",
                    () -> IForgeContainerType.create(AdvancedInscriberContainer::fromNetwork));

    public AdvancedInscriberMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TILE_ENTITIES.register(modBus);
        CONTAINERS.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> Upgrades.SPEED.registerItem(ADVANCED_INSCRIBER_ITEM.get(), 5));
    }
}
