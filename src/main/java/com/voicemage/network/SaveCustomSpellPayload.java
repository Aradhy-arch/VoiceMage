package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record SaveCustomSpellPayload(String phrase, String particleName, float manaCost, float damageOrEffect, String javaCode) implements CustomPacketPayload {
   public static final Type<SaveCustomSpellPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("voicemage", "save_custom_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SaveCustomSpellPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SaveCustomSpellPayload::phrase,
      ByteBufCodecs.STRING_UTF8, SaveCustomSpellPayload::particleName,
      ByteBufCodecs.FLOAT, SaveCustomSpellPayload::manaCost,
      ByteBufCodecs.FLOAT, SaveCustomSpellPayload::damageOrEffect,
      ByteBufCodecs.STRING_UTF8, SaveCustomSpellPayload::javaCode,
      SaveCustomSpellPayload::new
   );

   public SaveCustomSpellPayload(String phrase, String particleName, float manaCost, float damageOrEffect) {
      this(phrase, particleName, manaCost, damageOrEffect, "");
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
