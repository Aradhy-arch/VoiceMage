package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record OpenSpellsPayload(String configJson) implements CustomPacketPayload {
   public static final Type<OpenSpellsPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "open_spells"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpellsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, OpenSpellsPayload::configJson, OpenSpellsPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
