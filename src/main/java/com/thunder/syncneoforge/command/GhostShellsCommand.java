package com.thunder.syncneoforge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.kir.sync.api.shell.Shell;
import dev.kir.sync.api.shell.ShellState;
import dev.kir.sync.api.shell.ShellStateContainer;
import dev.kir.sync.util.WorldUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class GhostShellsCommand implements Command {
    private static final SimpleCommandExceptionType INVALID_ACTION_TYPE = new SimpleCommandExceptionType(Text.translatable("command.sync.ghostshells.invalid_action"));

    @Override
    public String getName() {
        return "ghostshells";
    }

    @Override
    public boolean hasPermissions(ServerCommandSource commandSource) {
        final int OP_LEVEL = 2;
        return commandSource.hasPermissionLevel(OP_LEVEL) || commandSource.getServer().isSingleplayer();
    }

    @Override
    public void build(ArgumentBuilder<ServerCommandSource, ?> builder) {
        builder.then(CommandManager.argument("type", StringArgumentType.word())
            .suggests((a, b) -> CommandSource.suggestMatching(Set.of("sync", "remove", "repair"), b))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .executes(GhostShellsCommand::execute)
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(GhostShellsCommand::execute)
                )
            )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String type = StringArgumentType.getString(context, "type");
        boolean repair;
        boolean canSkip;

        switch (type) {
            case "sync" -> {
                repair = true;
                canSkip = true;
            }
            case "remove" -> {
                repair = false;
                canSkip = true;
            }
            case "repair" -> {
                repair = true;
                canSkip = false;
            }
            default -> throw INVALID_ACTION_TYPE.create();
        }

        Consumer<Text> logger = x -> context.getSource().sendFeedback(x, false);
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        BlockPos pos;
        try {
            pos = BlockPosArgumentType.getBlockPos(context, "pos");
        } catch (IllegalArgumentException e) {
            pos = null;
        }

        if (pos == null) {
            for (ServerPlayerEntity player : players) {
                updateShells(player, repair, canSkip, logger);
            }
        } else {
            BlockPos finalPos = pos;
            for (ServerPlayerEntity player : players) {
                ShellState shellState = ((Shell)player).getAvailableShellStates().filter(x -> x.getPos().equals(finalPos)).findAny().orElse(null);
                if (shellState == null) {
                    logger.accept(Text.translatable("command.sync.ghostshells.not_found", player.getName().getString(), pos.toShortString()));
                } else {
                    updateShell(player, shellState, repair, canSkip, logger);
                }
            }
        }
        return 1;
    }

    private static void updateShells(ServerPlayerEntity player, boolean shouldRepair, boolean skipOnFailure, Consumer<Text> logger) {
        for (ShellState shellState : (Iterable<ShellState>)((Shell)player).getAvailableShellStates()::iterator) {
            updateShell(player, shellState, shouldRepair, skipOnFailure, logger);
        }
    }

    private static void updateShell(ServerPlayerEntity player, ShellState shellState, boolean shouldRepair, boolean skipOnFailure, Consumer<Text> logger) {
        if (shellExists(player.server, shellState)) {
            return;
        }

        if (shouldRepair) {
            if (tryRepair(player.server, shellState)) {
                logger.accept(Text.translatable("command.sync.ghostshells.repaired", player.getName().getString(), shellState.getPos().toShortString()));
                return;
            }

            if (!skipOnFailure) {
                logger.accept(Text.translatable("command.sync.ghostshells.failed", player.getName().getString(), shellState.getPos().toShortString()));
                return;
            }
        }

        ((Shell)player).remove(shellState);
        logger.accept(Text.translatable("command.sync.ghostshells.removed", player.getName().getString(), shellState.getPos().toShortString()));
    }

    private static boolean shellExists(MinecraftServer server, ShellState shellState) {
        return getShellContainer(server, shellState).map(x -> shellState.equals(x.getShellState())).orElse(Boolean.FALSE);
    }

    private static boolean tryRepair(MinecraftServer server, ShellState shellState) {
        ShellStateContainer shellContainer = getShellContainer(server, shellState).orElse(null);
        if (shellContainer == null) {
            return false;
        }

        if (shellContainer.getShellState() == null) {
            shellContainer.setShellState(shellState);
        }

        return shellState.equals(shellContainer.getShellState());
    }

    private static Optional<ShellStateContainer> getShellContainer(MinecraftServer server, ShellState shellState) {
        ServerWorld world = WorldUtil.findWorld(server.getWorlds(), shellState.getWorld()).orElse(null);
        if (world == null) {
            return Optional.empty();
        }

        Chunk chunk = world.getChunk(shellState.getPos());
        if (chunk == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ShellStateContainer.find(world, shellState));
    }
}