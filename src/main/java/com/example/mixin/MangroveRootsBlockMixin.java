package com.example.mixin;


import com.example.MangroveDecayMod;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.OptionalInt;


@Mixin(MangroveRootsBlock.class)
public abstract class MangroveRootsBlockMixin extends Block implements Waterloggable {
    @Unique
    private static final IntProperty DISTANCE;
    @Unique
    private static final BooleanProperty PERSISTENT;

    public MangroveRootsBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void MangroveRootsBlock(AbstractBlock.Settings settings, CallbackInfo info) {
        this.setDefaultState(this.stateManager.getDefaultState().with(DISTANCE, MangroveDecayMod.DISTANCE).with(PERSISTENT, false).with(MangroveRootsBlock.WATERLOGGED, false));
    }

    @Unique
    protected boolean hasRandomTicks(BlockState state) {
        return state.get(DISTANCE) == MangroveDecayMod.DISTANCE && !(Boolean)state.get(PERSISTENT);
    }

    @Unique
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.shouldDecay(state)) {
            dropStacks(state, world, pos);
            world.removeBlock(pos, false);
        }

    }

    @Unique
    protected boolean shouldDecay(BlockState state) {
        return !(Boolean)state.get(PERSISTENT) && state.get(DISTANCE) == MangroveDecayMod.DISTANCE;
    }

    @Unique
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, updateDistanceFromLogs(state, world, pos), 3);
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("TAIL"))
    protected void getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> info) {
        int i = getDistanceFromLog(neighborState) + 1;
        if (i != 1 || state.get(DISTANCE) != i) {
            world.scheduleBlockTick(pos, this, 1);
        }
    }

    @Unique
    private static BlockState updateDistanceFromLogs(BlockState state, WorldAccess world, BlockPos pos) {
        int i = MangroveDecayMod.DISTANCE;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Direction[] var5 = Direction.values();

        for (Direction direction : var5) {
            mutable.set(pos, direction);
            i = Math.min(i, getDistanceFromLog(world.getBlockState(mutable)) + 1);
            if (i == 1) {
                break;
            }
        }

        return state.with(DISTANCE, i);
    }

    @Unique
    private static int getDistanceFromLog(BlockState state) {
        return getOptionalDistanceFromLog(state).orElse(MangroveDecayMod.DISTANCE);
    }

    @Unique
    private static OptionalInt getOptionalDistanceFromLog(BlockState state) {
        if (state.isIn(BlockTags.LOGS)) {
            return OptionalInt.of(0);
        } else {
            return state.contains(DISTANCE) ? OptionalInt.of(state.get(DISTANCE)) : OptionalInt.empty();
        }
    }

    @Inject(method = "appendProperties", at = @At("TAIL"))
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo info) {
        builder.add(DISTANCE, PERSISTENT);
    }

    /**
     * @author First-Name-Last-Name
     * @reason Necessary replacement of return value; no other content needed from original method
     */
    @Overwrite
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState blockState = this.getDefaultState().with(PERSISTENT, true).with(MangroveRootsBlock.WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        return updateDistanceFromLogs(blockState, ctx.getWorld(), ctx.getBlockPos());
    }

    static {
        DISTANCE = IntProperty.of("distance", 1, MangroveDecayMod.DISTANCE);
        PERSISTENT = Properties.PERSISTENT;
    }
}
