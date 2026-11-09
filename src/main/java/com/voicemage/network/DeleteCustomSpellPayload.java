package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record DeleteCustomSpellPayload(String phrase) implements CustomPacketPayload {
   public static final Type<DeleteCustomSpellPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "delete_custom_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DeleteCustomSpellPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, DeleteCustomSpellPayload::phrase, DeleteCustomSpellPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
