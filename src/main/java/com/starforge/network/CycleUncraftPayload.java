package com.starforge.network;

import com.starforge.Starforge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：拆解台界面切换上一条 / 下一条匹配配方。
 */
public record CycleUncraftPayload(int delta) implements CustomPacketPayload {

    public static final Type<CycleUncraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Starforge.MOD_ID, "cycle_uncraft"));

    public static final StreamCodec<FriendlyByteBuf, CycleUncraftPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CycleUncraftPayload decode(FriendlyByteBuf buffer) {
                    return new CycleUncraftPayload(buffer.readInt());
                }

                @Override
                public void encode(FriendlyByteBuf buffer, CycleUncraftPayload payload) {
                    buffer.writeInt(payload.delta());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
