package com.starforge.network;

import com.starforge.Starforge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：切换右键模式（Z 键）。
 *
 * <p>模式存在物品的 custom_data 里，属于服务端权威数据，所以按键只负责通知服务端。</p>
 */
public record CycleModePayload() implements CustomPacketPayload {

    public static final Type<CycleModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Starforge.MOD_ID, "cycle_mode"));

    public static final StreamCodec<FriendlyByteBuf, CycleModePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> new CycleModePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
