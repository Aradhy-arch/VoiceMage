package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record UpdateSpellCostPayload(int spellId, int newCost) implements CustomPacketPayload {
   public static final Type<UpdateSpellCostPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "update_spell_cost"));
   public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSpellCostPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, UpdateSpellCostPayload::spellId,
      ByteBufCodecs.VAR_INT, UpdateSpellCostPayload::newCost,
      UpdateSpellCostPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
