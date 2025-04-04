package com.homo.tadokoro;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.Bukkit;

import static com.homo.tadokoro.BetterMidPickPlugin.plugin;

public class PickUpHandler extends ChannelDuplexHandler {

    private final ServerPlayer player;

    public PickUpHandler(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerboundPickItemFromBlockPacket packet && handle(packet)) {
            return;
        }
        super.channelRead(ctx, msg);
    }

    private boolean handle(ServerboundPickItemFromBlockPacket packet) {
        if (!packet.includeData() || !player.getBukkitEntity().hasPermission("bettermidpick.copyall")) {
            return false;
        }

        ServerLevel serverLevel = this.player.serverLevel();
        BlockPos blockPos = packet.pos();
        if (!this.player.canInteractWithBlock(blockPos, 1.0)) {
            return false;
        }
        if (!serverLevel.isLoaded(blockPos)) {
            return false;
        }
        BlockState blockState = serverLevel.getBlockState(blockPos);
        boolean flag = this.player.hasInfiniteMaterials() && packet.includeData();
        ItemStack cloneItemStack = blockState.getCloneItemStack(serverLevel, blockPos, flag);
        if (cloneItemStack.isEmpty()) {
            return false;
        }
        if (flag) {
            addBlockDataToItem(blockState, serverLevel, blockPos, cloneItemStack);
            addBlockStateToItem(blockState, serverLevel, blockPos, cloneItemStack);
        }
        Bukkit.getScheduler().runTask(plugin, () -> this.tryPickItem(cloneItemStack));
        return true;
    }

    private static void addBlockDataToItem(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockItemStateProperties properties = BlockItemStateProperties.EMPTY;
        for (Property<?> property : state.getValues().keySet()) {
            properties = properties.with(property, state);
        }
        stack.set(DataComponents.BLOCK_STATE, properties);
    }

    private static void addBlockStateToItem(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (blockEntity != null) {
            CompoundTag compoundTag = blockEntity.saveCustomOnly(level.registryAccess());
            blockEntity.removeComponentsFromTag(compoundTag);
            BlockItem.setBlockEntityData(stack, blockEntity.getType(), compoundTag);
            stack.applyComponents(blockEntity.collectComponents());
        }
    }

    private void tryPickItem(ItemStack stack) {
        if (!stack.isItemEnabled(this.player.level().enabledFeatures())) {
            return;
        }

        Inventory inventory = this.player.getInventory();
        final int sourceSlot = inventory.findSlotMatchingItem(stack);
        final int targetSlot = Inventory.isHotbarSlot(sourceSlot) ? sourceSlot : inventory.getSuitableHotbarSlot();
        if (sourceSlot != -1) {
            if (Inventory.isHotbarSlot(sourceSlot) && Inventory.isHotbarSlot(targetSlot)) {
                inventory.selected = targetSlot;
            } else {
                inventory.pickSlot(sourceSlot, targetSlot);
            }
        } else if (this.player.hasInfiniteMaterials()) {
            inventory.addAndPickItem(stack, targetSlot);
        }

        this.player.connection.send(new ClientboundSetHeldSlotPacket(inventory.selected));
        this.player.inventoryMenu.broadcastChanges();
        if (io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.updateEquipmentOnPlayerActions)
            this.player.detectEquipmentUpdatesPublic(); // Paper - Force update attributes.
    }
}