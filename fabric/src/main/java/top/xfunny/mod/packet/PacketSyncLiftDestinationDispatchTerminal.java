package top.xfunny.mod.packet;

import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.MinecraftServerHelper;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import top.xfunny.mod.Init;
import top.xfunny.mod.block.base.LiftDestinationDispatchTerminalBase;

/**
 * 同步 LiftDestinationDispatchTerminal 的屏幕显示状态。
 * <p>
 * C2S: 交互玩家客户端 → 服务端（更新 BlockEntity，广播给所有玩家）
 * S2C: 服务端 → 所有附近玩家客户端（更新本地 BlockEntity 显示）
 */
public final class PacketSyncLiftDestinationDispatchTerminal extends PacketHandler {

    private final BlockPos blockPos;
    private final String screenId;
    private final String displayText;

    public PacketSyncLiftDestinationDispatchTerminal(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        screenId = packetBufferReceiver.readString();
        displayText = packetBufferReceiver.readString();
    }

    public PacketSyncLiftDestinationDispatchTerminal(BlockPos blockPos, String screenId, String displayText) {
        this.blockPos = blockPos;
        this.screenId = screenId;
        this.displayText = displayText;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(screenId);
        packetBufferSender.writeString(displayText);
    }

    @Override
    public void runClient() {
        // 客户端收到 S2C 包：更新本地 BlockEntity 的显示状态
        final ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) {
            return;
        }
        final BlockEntity blockEntity = clientWorld.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof LiftDestinationDispatchTerminalBase.BlockEntityBase) {
            ((LiftDestinationDispatchTerminalBase.BlockEntityBase) blockEntity.data)
                    .applyDisplayState(screenId, displayText);
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        // 服务端收到 C2S 包：更新服务端 BlockEntity，广播给所有玩家
        final World world = serverPlayerEntity.getEntityWorld();
        final ServerWorld serverWorld = serverPlayerEntity.getServerWorld();
        if (!Init.isChunkLoaded(world, blockPos)) {
            return;
        }

        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof LiftDestinationDispatchTerminalBase.BlockEntityBase) {
            final LiftDestinationDispatchTerminalBase.BlockEntityBase terminalData =
                    (LiftDestinationDispatchTerminalBase.BlockEntityBase) blockEntity.data;
            terminalData.applyDisplayState(screenId, displayText);
            terminalData.markDirty2();

            // 广播给该世界所有其他玩家（跳过发送者自身）
            MinecraftServerHelper.iteratePlayers(serverWorld, player -> {
                if (!player.getUuid().equals(serverPlayerEntity.getUuid())) {
                    Init.REGISTRY.sendPacketToClient(player,
                            new PacketSyncLiftDestinationDispatchTerminal(blockPos, screenId, displayText));
                }
            });
        }
    }
}
