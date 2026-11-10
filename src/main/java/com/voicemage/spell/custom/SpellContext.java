package com.voicemage.spell.custom;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.SpellEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fluent context passed to dynamic Java custom spells executed via Janino.
 * Bound to the variable 'ctx' in user scripts.
 */
public class SpellContext {
   public final ServerPlayer player;
   public final ServerLevel level;
   public final LivingEntity targetEntity;
   public final Vec3 targetPos;
   private final SpellEffects effects;

   public SpellContext(ServerPlayer player, ServerLevel level, LivingEntity targetEntity, Vec3 targetPos, SpellEffects effects) {
      this.player = player;
      this.level = level;
      this.targetEntity = targetEntity;
      this.targetPos = targetPos != null ? targetPos : player.getEyePosition().add(player.getLookAngle().scale(10.0));
      this.effects = effects;
   }

   public SpellContext(ServerPlayer player, ServerLevel level, LivingEntity targetEntity, Vec3 targetPos) {
      this(player, level, targetEntity, targetPos, null);
   }

   public ServerPlayer getPlayer() {
      return this.player;
   }

   public ServerPlayer getCaster() {
      return this.player;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public LivingEntity getTargetEntity() {
      return this.targetEntity;
   }

   public Vec3 getTargetPos() {
      return this.targetPos;
   }

   /**
    * Returns all living entities within a spherical radius around the target position (or caster).
    */
   public List<LivingEntity> getNearbyLiving(double radius) {
      Vec3 center = this.targetPos != null ? this.targetPos : this.player.position();
      double r = Math.max(0.5, Math.min(64.0, radius));
      AABB box = new AABB(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
      List<LivingEntity> entities = this.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator());
      List<LivingEntity> result = new ArrayList<>();
      double rSq = r * r;
      for (LivingEntity e : entities) {
         if (e.distanceToSqr(center) <= rSq) {
            result.add(e);
         }
      }
      return result;
   }

   /**
    * Returns all nearby living entities excluding the caster and non-harmable players.
    */
   public List<LivingEntity> getNearbyFoes(double radius) {
      List<LivingEntity> list = this.getNearbyLiving(radius);
      list.removeIf(e -> e.equals(this.player) || (e instanceof Player p && !this.player.canHarmPlayer(p)));
      return list;
   }

   /**
    * Shoots a multi-particle raycast beam from caster's eyes to the targeted position.
    * E.g. ctx.beam("flame, soul_fire_flame, sonic_boom")
    */
   public void beam(String particles) {
      List<ParticleOptions> options = parseParticleList(particles);
      if (options.isEmpty()) {
         options.add(ParticleTypes.WITCH);
      }
      Vec3 start = this.player.getEyePosition();
      Vec3 ray = this.targetPos.subtract(start);
      double dist = ray.length();
      Vec3 dir = dist > 0.001 ? ray.normalize() : this.player.getLookAngle();
      double step = Math.max(0.5, dist / 25.0);
      for (double d = 0.5; d < dist; d += step) {
         Vec3 p = start.add(dir.scale(d));
         for (ParticleOptions opt : options) {
            this.level.sendParticles(opt, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
   }

   /**
    * Spawns an explosive burst of one or more particle types at the given position.
    */
   public void burst(String particles, Vec3 pos, int count) {
      Vec3 at = pos != null ? pos : this.targetPos;
      List<ParticleOptions> options = parseParticleList(particles);
      if (options.isEmpty()) {
         options.add(ParticleTypes.WITCH);
      }
      int c = Math.max(1, Math.min(200, count));
      for (ParticleOptions opt : options) {
         this.level.sendParticles(opt, at.x, at.y, at.z, c, 0.6, 0.6, 0.6, 0.08);
      }
   }

   /**
    * Spawns a single particle type at specific coordinates.
    */
   public void spawnParticle(String particleId, double x, double y, double z, int count) {
      ParticleType<?> type = VoicemageConfig.parseParticle(particleId);
      ParticleOptions opt = (type instanceof ParticleOptions o) ? o : ParticleTypes.WITCH;
      int c = Math.max(1, Math.min(200, count));
      this.level.sendParticles(opt, x, y, z, c, 0.2, 0.2, 0.2, 0.02);
   }

   /**
    * Causes nearby living entities to jump, rotate, and emit music notes.
    */
   public void makeDance(double radius, int seconds) {
      List<LivingEntity> entities = this.getNearbyLiving(radius);
      for (LivingEntity e : entities) {
         e.setDeltaMovement(e.getDeltaMovement().x, 0.45, e.getDeltaMovement().z);
         float rot = e.getYRot() + 90.0F;
         e.setYRot(rot);
         e.setYHeadRot(rot);
         e.setYBodyRot(rot);
         e.hurtMarked = true;
         this.level.sendParticles(ParticleTypes.NOTE, e.getX(), e.getY() + 1.2, e.getZ(), 6, 0.4, 0.4, 0.4, 0.5);
      }
      playSound("block.note_block.bell", 1.2F, 1.4F);
      message("§d🕺 Dance party underway (" + entities.size() + " dancers)!");
   }

   /**
    * Deals magic damage to target entity.
    */
   public void damage(LivingEntity target, float amount) {
      if (target == null || !target.isAlive()) return;
      if (target instanceof Player p && !this.player.canHarmPlayer(p)) return;
      target.hurtServer(this.level, this.level.damageSources().magic(), Math.max(0.0F, amount));
      target.hurtMarked = true;
   }

   /**
    * Restores health to target entity with healing spark particles.
    */
   public void heal(LivingEntity target, float amount) {
      if (target == null || !target.isAlive()) return;
      target.heal(Math.max(0.0F, amount));
      this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
   }

   /**
    * Launches target entity with explicit velocity vector.
    */
   public void launch(LivingEntity target, double vx, double vy, double vz) {
      if (target == null || !target.isAlive()) return;
      target.setDeltaMovement(vx, vy, vz);
      target.hurtMarked = true;
   }

   /**
    * Pulls target entity towards the crosshair/target position.
    */
   public void pull(LivingEntity target, double strength) {
      if (target == null || !target.isAlive()) return;
      Vec3 center = this.targetPos != null ? this.targetPos : this.player.position();
      Vec3 diff = center.subtract(target.position());
      Vec3 dir = diff.length() > 0.1 ? diff.normalize() : new Vec3(0, 1, 0);
      double s = Math.max(0.1, Math.min(5.0, strength));
      target.setDeltaMovement(dir.x * s, Math.max(0.2, dir.y * s), dir.z * s);
      target.hurtMarked = true;
   }

   /**
    * Strikes a lightning bolt at coordinates.
    */
   public void strikeLightning(Vec3 pos) {
      Vec3 at = pos != null ? pos : this.targetPos;
      LightningBolt bolt = (LightningBolt)EntityTypes.LIGHTNING_BOLT.create(this.level, EntitySpawnReason.TRIGGERED);
      if (bolt != null) {
         bolt.setVisualOnly(false);
         bolt.setCause(this.player);
         bolt.setPos(at.x, at.y, at.z);
         this.level.addFreshEntity(bolt);
      }
   }

   /**
    * Applies a status potion effect to the target entity (e.g. "speed", "levitation", "glowing").
    */
   public void applyPotion(LivingEntity target, String effectName, int seconds, int amplifier) {
      if (target == null || !target.isAlive() || effectName == null) return;
      Holder<MobEffect> holder = resolveMobEffect(effectName);
      if (holder != null) {
         int ticks = Math.max(20, seconds * 20);
         int amp = Math.max(0, amplifier);
         target.addEffect(new MobEffectInstance(holder, ticks, amp, false, true), this.player);
      }
   }

   /**
    * Plays any Minecraft sound at target or caster location.
    */
   public void playSound(String soundName, float volume, float pitch) {
      if (soundName == null || soundName.isBlank()) return;
      String clean = soundName.trim().toLowerCase(Locale.ROOT);
      if (!clean.contains(":")) {
         clean = "minecraft:" + clean;
      }
      Identifier id = Identifier.tryParse(clean);
      Holder<SoundEvent> holder = null;
      if (id != null) {
         holder = BuiltInRegistries.SOUND_EVENT.get(id).orElse(null);
      }
      BlockPos pos = this.targetPos != null ? BlockPos.containing(this.targetPos) : this.player.blockPosition();
      SoundEvent sound = holder != null ? holder.value() : SoundEvents.ILLUSIONER_CAST_SPELL;
      this.level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
   }

   /**
    * Sends a chat/action-bar message to the caster.
    */
   public void message(String text) {
      if (text != null && !text.isBlank()) {
         this.player.sendSystemMessage(Component.literal(text));
      }
   }

   /**
    * Executes a server command as the caster.
    */
   public void runCommand(String command) {
      if (command == null || command.isBlank()) return;
      String clean = command.trim();
      if (clean.startsWith("/")) {
         clean = clean.substring(1);
      }
      try {
         this.player.level().getServer().getCommands().performPrefixedCommand(
            this.player.createCommandSourceStack(),
            clean
         );
      } catch (Exception e) {
         this.message("§cCommand execution failed: " + e.getMessage());
      }
   }

   /**
    * Schedules an action to execute after a tick delay (20 ticks = 1 second).
    * Enables creating multi-stage spells with timed phases and effects!
    * E.g. ctx.schedule(20, () -> { ctx.strikeLightning(ctx.getTargetPos()); });
    */
   public void schedule(int delayTicks, Runnable action) {
      if (action == null) return;
      if (this.effects != null) {
         this.effects.schedule(this.level, this.player, Math.max(0, delayTicks), action, "custom stage");
      } else {
         action.run();
      }
   }

   /**
    * Alias for schedule(delayTicks, action).
    */
   public void delay(int delayTicks, Runnable action) {
      this.schedule(delayTicks, action);
   }

   private static List<ParticleOptions> parseParticleList(String particles) {
      List<ParticleOptions> list = new ArrayList<>();
      if (particles == null || particles.isBlank()) return list;
      String[] parts = particles.split(",");
      for (String p : parts) {
         String name = p.trim();
         if (!name.isEmpty()) {
            ParticleType<?> type = VoicemageConfig.parseParticle(name);
            if (type instanceof ParticleOptions opt) {
               list.add(opt);
            }
         }
      }
      return list;
   }

   private static Holder<MobEffect> resolveMobEffect(String name) {
      if (name == null || name.isBlank()) return null;
      String clean = name.trim().toLowerCase(Locale.ROOT);
      if (!clean.contains(":")) {
         clean = "minecraft:" + clean;
      }
      Identifier id = Identifier.tryParse(clean);
      if (id != null) {
         return BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
      }
      return null;
   }
}
