package com.starforge.network;

import com.starforge.Starforge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: "switch ability {@code ability} to {@code value}".
 *
 * <p>The payload only carries an index and a boolean, everything else is looked up again on the
 * server, so a client cannot smuggle in arbitrary data.</p>
 */
public record TogglePayload(int ability, boolean value) implements CustomPacketPayload {

    public static final Type<TogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Starforge.MOD_ID, "toggle_ability"));

    public static final StreamCodec<ByteBuf, TogglePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TogglePayload decode(ByteBuf buffer) {
            return new TogglePayload(buffer.readInt(), buffer.readBoolean());
        }

        @Override
        public void encode(ByteBuf buffer, TogglePayload payload) {
            buffer.writeInt(payload.ability());
            buffer.writeBoolean(payload.value());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
