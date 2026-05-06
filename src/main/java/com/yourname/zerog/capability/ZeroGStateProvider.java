package com.yourname.zerog.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZeroGStateProvider implements ICapabilitySerializable<CompoundTag> {
    private final ZeroGState state = new ZeroGState();
    private final LazyOptional<ZeroGState> optional = LazyOptional.of(() -> state);

    @NotNull @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ZeroGCapability.ZERO_G_STATE) return optional.cast();
        return LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT() { return state.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt) { state.deserializeNBT(nbt); }
}