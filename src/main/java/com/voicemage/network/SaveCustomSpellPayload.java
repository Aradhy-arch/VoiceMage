package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record SaveCustomSpellPayload(
   String phrase,
   String particleName,
   float manaCost,
   float damageOrEffect,
   String javaCode,
   boolean chantRequired,
   String chant,
   int tickDelay
) implements CustomPacketPayload {
   public static final Type<SaveCustomSpellPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("voicemage", "save_custom_spell"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SaveCustomSpellPayload> STREAM_CODEC = new StreamCodec<>() {
      @Override
      public SaveCustomSpellPayload decode(RegistryFriendlyByteBuf buf) {
         String phrase = ByteBufCodecs.STRING_UTF8.decode(buf);
         String particleName = ByteBufCodecs.STRING_UTF8.decode(buf);
         float manaCost = ByteBufCodecs.FLOAT.decode(buf);
         float damageOrEffect = ByteBufCodecs.FLOAT.decode(buf);
         String javaCode = ByteBufCodecs.STRING_UTF8.decode(buf);
         boolean chantRequired = ByteBufCodecs.BOOL.decode(buf);
         String chant = ByteBufCodecs.STRING_UTF8.decode(buf);
         int tickDelay = ByteBufCodecs.VAR_INT.decode(buf);
         return new SaveCustomSpellPayload(phrase, particleName, manaCost, damageOrEffect, javaCode, chantRequired, chant, tickDelay);
      }

      @Override
      public void encode(RegistryFriendlyByteBuf buf, SaveCustomSpellPayload payload) {
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.phrase());
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.particleName());
         ByteBufCodecs.FLOAT.encode(buf, payload.manaCost());
         ByteBufCodecs.FLOAT.encode(buf, payload.damageOrEffect());
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.javaCode());
         ByteBufCodecs.BOOL.encode(buf, payload.chantRequired());
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.chant());
         ByteBufCodecs.VAR_INT.encode(buf, payload.tickDelay());
      }
   };

   public SaveCustomSpellPayload(String phrase, String particleName, float manaCost, float damageOrEffect, String javaCode) {
      this(phrase, particleName, manaCost, damageOrEffect, javaCode, false, "", 0);
   }

   public SaveCustomSpellPayload(String phrase, String particleName, float manaCost, float damageOrEffect) {
      this(phrase, particleName, manaCost, damageOrEffect, "", false, "", 0);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
