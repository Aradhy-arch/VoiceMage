package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record SaveSettingsPayload(String configJson) implements CustomPacketPayload {
   public static final Type<SaveSettingsPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "save_settings"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SaveSettingsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SaveSettingsPayload::configJson, SaveSettingsPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
