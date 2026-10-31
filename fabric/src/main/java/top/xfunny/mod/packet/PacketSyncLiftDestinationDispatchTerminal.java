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
import top.xfunny.mod.block.SchindlerPortTerminal;
import top.xfunny.mod.block.base.LiftDestinationDispatchTerminalBase;

/**
 * Synchronizes the visible terminal page and optional PORT floor-group settings.
 *
 * C2S: interacting client -> server, where the terminal state is validated and saved.
 * S2C: server -> other clients in the same world.
 */
public final class PacketSyncLiftDestinationDispatchTerminal extends PacketHandler {

    private final BlockPos blockPos;
    private final String screenId;
    private final String displayText;
    private final String portGroups;
    private final long selectedGroupFloor;

    public PacketSyncLiftDestinationDispatchTerminal(PacketBufferReceiver packetBufferReceiver) {
        blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        screenId = packetBufferReceiver.readString();
        displayText = packetBufferReceiver.readString();
        portGroups = packetBufferReceiver.readString();
        selectedGroupFloor = packetBufferReceiver.readLong();
    }

    public PacketSyncLiftDestinationDispatchTerminal(BlockPos blockPos, String screenId, String displayText) {
        this(blockPos, screenId, displayText, "", -1L);
    }

    public PacketSyncLiftDestinationDispatchTerminal(BlockPos blockPos, String screenId, String displayText,
                                                       String portGroups, long selectedGroupFloor) {
        this.blockPos = blockPos;
        this.screenId = screenId;
        this.displayText = displayText;
        this.portGroups = portGroups == null ? "" : portGroups;
        this.selectedGroupFloor = selectedGroupFloor;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(screenId == null ? "" : screenId);
        packetBufferSender.writeString(displayText == null ? "" : displayText);
        packetBufferSender.writeString(portGroups);
        packetBufferSender.writeLong(selectedGroupFloor);
    }

    @Override
    public void runClient() {
        final ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) {
            return;
        }
        final BlockEntity blockEntity = clientWorld.getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof LiftDestinationDispatchTerminalBase.BlockEntityBase) {
            final LiftDestinationDispatchTerminalBase.BlockEntityBase terminalData =
                    (LiftDestinationDispatchTerminalBase.BlockEntityBase) blockEntity.data;
            terminalData.applyDisplayState(screenId, displayText);
            if (terminalData instanceof SchindlerPortTerminal.BlockEntity) {
                ((SchindlerPortTerminal.BlockEntity) terminalData).applyGroupSync(portGroups, selectedGroupFloor);
            }
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
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
            if (terminalData instanceof SchindlerPortTerminal.BlockEntity) {
                ((SchindlerPortTerminal.BlockEntity) terminalData).applyGroupSync(portGroups, selectedGroupFloor);
            }
            terminalData.markDirty2();

            MinecraftServerHelper.iteratePlayers(serverWorld, player -> {
                if (!player.getUuid().equals(serverPlayerEntity.getUuid())) {
                    Init.REGISTRY.sendPacketToClient(player,
                            new PacketSyncLiftDestinationDispatchTerminal(blockPos, screenId, displayText,
                                    portGroups, selectedGroupFloor));
                }
            });
        }
    }
}
