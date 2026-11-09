package com.voicemage;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.network.CastCustomSpellPayload;
import com.voicemage.network.CastSpellPayload;
import com.voicemage.network.ChantSettingsPayload;
import com.voicemage.network.CustomSpellsSyncPayload;
import com.voicemage.network.DeleteCustomSpellPayload;
import com.voicemage.network.FallingSpellPayload;
import com.voicemage.network.ManaSyncPayload;
import com.voicemage.network.OpenSettingsPayload;
import com.voicemage.network.OpenSpellsPayload;
import com.voicemage.network.SaveCustomSpellPayload;
import com.voicemage.network.SaveSettingsPayload;
import com.voicemage.network.SpellVisualPayload;
import com.voicemage.network.UpdateSpellCostPayload;
import com.voicemage.spell.Spell;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class VoicemageNetworking {
   private static final double BROADCAST_DISTANCE_SQUARED = 65536.0;

   private VoicemageNetworking() {
   }

   private static boolean isOperator(ServerPlayer player) {
      MinecraftServer server = player.getServer();
      return server != null && server.getPlayerList().isOp(player.getGameProfile());
   }

   public static void openSettingsScreen(ServerPlayer player) {
      if (isOperator(player)) {
         ServerPlayNetworking.send(player, new OpenSettingsPayload(VoicemageConfig.fullConfigJson()));
      }
   }

   public static void openSpellsScreen(ServerPlayer player) {
      if (isOperator(player)) {
         ServerPlayNetworking.send(player, new OpenSpellsPayload(VoicemageConfig.fullConfigJson()));
      }
   }

   public static void syncSettings(ServerPlayer player) {
      ServerPlayNetworking.send(player, new ChantSettingsPayload(VoicemageConfig.INSTANCE.explosionChantRequired));
      ServerPlayNetworking.send(player, new CustomSpellsSyncPayload(VoicemageConfig.customSpellsJson()));
   }

   public static void broadcastSettings(MinecraftServer server) {
      server.getPlayerList().getPlayers().forEach(VoicemageNetworking::syncSettings);
   }

   public static void register() {
      PayloadTypeRegistry.clientboundPlay().register(ChantSettingsPayload.TYPE, ChantSettingsPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(SpellVisualPayload.TYPE, SpellVisualPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(FallingSpellPayload.TYPE, FallingSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(CustomSpellsSyncPayload.TYPE, CustomSpellsSyncPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CastCustomSpellPayload.TYPE, CastCustomSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(OpenSettingsPayload.TYPE, OpenSettingsPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(OpenSpellsPayload.TYPE, OpenSpellsPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(SaveCustomSpellPayload.TYPE, SaveCustomSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(DeleteCustomSpellPayload.TYPE, DeleteCustomSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(SaveSettingsPayload.TYPE, SaveSettingsPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(UpdateSpellCostPayload.TYPE, UpdateSpellCostPayload.STREAM_CODEC);

      ServerPlayNetworking.registerGlobalReceiver(
         CastSpellPayload.TYPE, (payload, context) -> context.server().execute(() -> Voicemage.SPELLS.cast(context.player(), payload.spellId()))
      );
      ServerPlayNetworking.registerGlobalReceiver(
         CastCustomSpellPayload.TYPE, (payload, context) -> context.server().execute(() -> Voicemage.SPELLS.castCustom(context.player(), payload.phrase()))
      );
      ServerPlayNetworking.registerGlobalReceiver(
         SaveCustomSpellPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (!isOperator(context.player())) {
               return;
            }
            String phrase = payload.phrase().trim().toLowerCase(Locale.ROOT);
            if (phrase.isBlank() || Spell.byPhrase(phrase) != null) {
               return;
            }
            ParticleType<?> type = VoicemageConfig.parseParticle(payload.particleName());
            if (type == null) {
               return;
            }
            float mana = Math.max(0.0F, Math.min(100.0F, payload.manaCost()));
            float power = Math.max(0.0F, payload.damageOrEffect());

            VoicemageConfig.CustomSpellEntry existing = VoicemageConfig.INSTANCE.findCustomSpell(phrase);
            if (existing != null) {
               existing.particleName = payload.particleName();
               existing.manaCost = mana;
               existing.damageOrEffect = power;
            } else {
               VoicemageConfig.INSTANCE.customSpells.add(
                  new VoicemageConfig.CustomSpellEntry(phrase, payload.particleName(), mana, power)
               );
            }
            VoicemageConfig.save();
            VoicemageNetworking.broadcastSettings(context.server());
         })
      );
      ServerPlayNetworking.registerGlobalReceiver(
         DeleteCustomSpellPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (!isOperator(context.player())) {
               return;
            }
            String phrase = payload.phrase().trim().toLowerCase(Locale.ROOT);
            boolean removed = VoicemageConfig.INSTANCE.customSpells.removeIf(
               entry -> entry.phrase != null && entry.phrase.equalsIgnoreCase(phrase)
            );
            if (removed) {
               VoicemageConfig.save();
               VoicemageNetworking.broadcastSettings(context.server());
            }
         })
      );
      ServerPlayNetworking.registerGlobalReceiver(
         SaveSettingsPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (!isOperator(context.player())) {
               return;
            }
            VoicemageConfig.applyFullConfigJson(payload.configJson());
            VoicemageConfig.save();
            VoicemageNetworking.broadcastSettings(context.server());
         })
      );
      ServerPlayNetworking.registerGlobalReceiver(
         UpdateSpellCostPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (!isOperator(context.player())) {
               return;
            }
            Spell spell = Spell.byNetworkId(payload.spellId());
            if (spell != null) {
               VoicemageConfig.setManaCost(spell, payload.newCost());
               VoicemageNetworking.broadcastSettings(context.server());
            }
         })
      );
   }

   public static void sync(ServerPlayer player, double mana, int spellId, int result) {
      ServerPlayNetworking.send(player, new ManaSyncPayload((int)Math.round(mana * 10.0), (int)Math.round(Voicemage.SPELLS.maxMana() * 10.0), spellId, result));
   }

   public static void falling(ServerLevel level, FallingSpellPayload payload) {
      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         if (player.level() == level && player.distanceToSqr(payload.x(), payload.y(), payload.z()) <= 65536.0) {
            ServerPlayNetworking.send(player, payload);
         }
      }
   }

   public static void visual(ServerLevel level, int spellId, double x, double y, double z, int phaseTicks, int durationTicks, int entityId, double scale) {
      SpellVisualPayload payload = new SpellVisualPayload(spellId, x, y, z, phaseTicks, durationTicks, entityId, scale);
      Entity attached = entityId >= 0 ? level.getEntity(entityId) : null;
      double broadcastX = attached == null ? x : attached.getX();
      double broadcastY = attached == null ? y : attached.getY();
      double broadcastZ = attached == null ? z : attached.getZ();

      for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
         if (viewer.level() == level && viewer.distanceToSqr(broadcastX, broadcastY, broadcastZ) <= 65536.0) {
            ServerPlayNetworking.send(viewer, payload);
         }
      }
   }
}
