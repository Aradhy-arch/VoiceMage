package com.voicemage.mixin;

import com.voicemage.Voicemage;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
   protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
   private void voicemage$causeFallDamage(double fallDistance, float multiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      if (!player.isCreative() && !player.isSpectator() && Voicemage.SPELLS.isFlightActive(player.getUUID())) {
         if (fallDistance >= 2.0) {
            player.awardStat(Stats.FALL_ONE_CM, (int)Math.round(fallDistance * 100.0));
         }
         cir.setReturnValue(super.causeFallDamage(fallDistance, multiplier, damageSource));
      }
   }
}
