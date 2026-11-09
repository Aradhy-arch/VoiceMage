package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record OpenSettingsPayload(String configJson) implements CustomPacketPayload {
   public static final Type<OpenSettingsPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "open_settings"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenSettingsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, OpenSettingsPayload::configJson, OpenSettingsPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
