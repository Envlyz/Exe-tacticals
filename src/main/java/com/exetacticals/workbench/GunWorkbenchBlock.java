package com.exetacticals.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunWorkbenchBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {
    public enum Part implements StringRepresentable {
        MAIN("main", 0, 0), RIGHT("right", 1, 0), TOP("top", 0, 1), TOP_RIGHT("top_right", 1, 1);

        private final String name;
        final int right;
        final int up;

        Part(String name, int right, int up) {
            this.name = name;
            this.right = right;
            this.up = up;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final java.util.Map<Direction, VoxelShape> LOWER = new java.util.EnumMap<>(Direction.class);
    private static final java.util.Map<Direction, VoxelShape> UPPER = new java.util.EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape wall = switch (facing) {
                case NORTH -> Block.box(0, 0, 13, 16, 16, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
                case EAST -> Block.box(0, 0, 0, 3, 16, 16);
                default -> Block.box(13, 0, 0, 16, 16, 16);
            };
            UPPER.put(facing, wall);
            LOWER.put(facing, net.minecraft.world.phys.shapes.Shapes.or(Block.box(0, 0, 0, 16, 15.5, 16), wall));
        }
    }

    private final WorkbenchType type;

    public GunWorkbenchBlock(Properties properties, WorkbenchType type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.MAIN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public WorkbenchType type() {
        return type;
    }

    private Part[] parts() {
        return type.width >= 2 ? Part.values() : new Part[]{Part.MAIN, Part.TOP};
    }

    public static BlockPos partPos(BlockPos main, Direction facing, Part part) {
        return main.relative(facing.getCounterClockWise(), part.right).above(part.up);
    }

    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Part part = state.getValue(PART);
        return pos.relative(facing.getCounterClockWise(), -part.right).below(part.up);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos main = ctx.getClickedPos();
        Level level = ctx.getLevel();
        for (Part part : parts()) {
            BlockPos pos = partPos(main, facing, part);
            if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() >= level.getMaxBuildHeight()
                    || !level.getBlockState(pos).canBeReplaced(ctx)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, Part.MAIN);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (Part part : parts()) {
            if (part != Part.MAIN) {
                level.setBlock(partPos(pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide) {
            BlockPos main = mainPos(pos, state);
            if (state.getValue(PART) != Part.MAIN && !player.isCreative()) {
                BlockState mainState = level.getBlockState(main);
                if (mainState.is(this)) {
                    Block.dropResources(mainState, level, main, null, player, player.getMainHandItem());
                }
            }
            removeOthers(level, main, state.getValue(FACING), pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private void removeOthers(Level level, BlockPos main, Direction facing, BlockPos except) {
        for (Part part : parts()) {
            BlockPos p = partPos(main, facing, part);
            if (!p.equals(except) && level.getBlockState(p).is(this)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbor,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        BlockPos main = mainPos(pos, state);
        Direction facing = state.getValue(FACING);
        for (Part part : parts()) {
            if (partPos(main, facing, part).equals(neighborPos) && !neighbor.is(this)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos main = mainPos(pos, state);
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, p) -> new GunWorkbenchMenu(id, inventory, main),
                    Component.translatable(getDescriptionId()));
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeBlockPos(main));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART).up == 0 ? LOWER.get(facing) : UPPER.get(facing);
    }

    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return type.keepsOnTable && state.getValue(PART) == Part.MAIN ? new GunWorkbenchBlockEntity(pos, state) : null;
    }

    @Nullable
    public static GunWorkbenchBlockEntity blockEntity(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunWorkbenchBlock)) {
            return null;
        }
        return level.getBlockEntity(mainPos(pos, state)) instanceof GunWorkbenchBlockEntity be ? be : null;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState,
                         boolean moved) {
        if (!state.is(newState.getBlock()) && state.getValue(PART) == Part.MAIN
                && level.getBlockEntity(pos) instanceof GunWorkbenchBlockEntity be && be.isOccupied()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, be.take());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
