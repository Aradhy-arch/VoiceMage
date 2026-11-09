package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record CastCustomSpellPayload(String phrase) implements CustomPacketPayload {
   public static final Type<CastCustomSpellPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "cast_custom_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CastCustomSpellPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CastCustomSpellPayload::phrase, CastCustomSpellPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
