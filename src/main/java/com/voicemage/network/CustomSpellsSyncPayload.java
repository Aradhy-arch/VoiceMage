package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record CustomSpellsSyncPayload(String json) implements CustomPacketPayload {
   public static final Type<CustomSpellsSyncPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "custom_spells_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CustomSpellsSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CustomSpellsSyncPayload::json, CustomSpellsSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
