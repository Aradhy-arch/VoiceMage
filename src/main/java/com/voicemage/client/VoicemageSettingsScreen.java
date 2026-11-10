package com.voicemage.client;

import com.voicemage.VoicemageClient;
import com.voicemage.config.VoicemageConfig;
import com.voicemage.network.DeleteCustomSpellPayload;
import com.voicemage.network.SaveCustomSpellPayload;
import com.voicemage.network.SaveSettingsPayload;
import com.voicemage.spell.Spell;
import com.voicemage.spell.custom.CustomSpellCompiler;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;

public final class VoicemageSettingsScreen extends Screen {
   private static final int TAB_MANA = 0;
   private static final int TAB_COMBAT = 1;
   private static final int TAB_CUSTOM = 2;
   private static final int SPELLS_PER_PAGE = 4;

   private int activeTab = TAB_MANA;
   private String statusMessage = "";
   private int statusColor = 0xFFAAAAAA;

   // Tab 0: Mana & Rules
   private EditBox maxManaBox;
   private EditBox manaRegenBox;
   private EditBox castIntervalBox;
   private EditBox flyDurationBox;
   private EditBox flyCooldownBox;
   private EditBox flyManaBox;
   private EditBox getDoomedManaBox;
   private boolean chantRequired;
   private boolean pitfallEnabled;

   // Tab 1: Combat & Explosions
   private EditBox explosionPowerBox;
   private EditBox explosionRangeBox;
   private EditBox explosionDelayBox;
   private EditBox explosionHeightBox;
   private EditBox fireballPowerBox;
   private EditBox meteorPowerBox;
   private EditBox lightningDmgBox;
   private EditBox healAmountBox;
   private EditBox skillRangeBox;

   // Tab 2: Custom Spells
   private EditBox phraseBox;
   private EditBox customManaBox;
   private MultiLineEditBox codeBox;
   private int customPage = 0;
   private VoicemageConfig.CustomSpellEntry selectedCustomSpell;

   public VoicemageSettingsScreen() {
      super(Component.literal("VoiceMage Configuration"));
      this.chantRequired = VoicemageConfig.INSTANCE.explosionChantRequired;
      this.pitfallEnabled = VoicemageConfig.INSTANCE.pitfallEnabled;
   }

   @Override
   protected void init() {
      super.init();

      int boxW = 390;
      int boxH = 226;
      int boxX = (this.width - boxW) / 2;
      int boxY = (this.height - boxH) / 2;

      // Tab Buttons
      int tabY = boxY + 28;
      int tabW = 120;
      this.addRenderableWidget(
         Button.builder(Component.literal(this.activeTab == TAB_MANA ? "§6⚡ Mana & Rules" : "⚡ Mana & Rules"), btn -> {
            this.saveCurrentTabValues();
            this.activeTab = TAB_MANA;
            this.refresh();
         }).bounds(boxX + 10, tabY, tabW, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal(this.activeTab == TAB_COMBAT ? "§6💥 Combat & World" : "💥 Combat & World"), btn -> {
            this.saveCurrentTabValues();
            this.activeTab = TAB_COMBAT;
            this.refresh();
         }).bounds(boxX + 135, tabY, tabW, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal(this.activeTab == TAB_CUSTOM ? "§6✨ Custom Spells" : "✨ Custom Spells"), btn -> {
            this.saveCurrentTabValues();
            this.activeTab = TAB_CUSTOM;
            this.refresh();
         }).bounds(boxX + 260, tabY, tabW, 18).build()
      );

      int contentY = tabY + 24;

      if (this.activeTab == TAB_MANA) {
         this.initManaTab(boxX, contentY);
      } else if (this.activeTab == TAB_COMBAT) {
         this.initCombatTab(boxX, contentY);
      } else {
         this.initCustomSpellsTab(boxX, contentY);
      }

      // Bottom Bar Action Buttons
      int bottomY = boxY + boxH - 24;
      this.addRenderableWidget(
         Button.builder(Component.literal("§a💾 Save & Apply"), btn -> this.saveAllSettings())
            .bounds(boxX + 10, bottomY, 140, 18)
            .build()
      );

      this.addRenderableWidget(
         Button.builder(Component.literal("Close"), btn -> this.onClose())
            .bounds(boxX + boxW - 80, bottomY, 70, 18)
            .build()
      );
   }

   private void initManaTab(int boxX, int contentY) {
      VoicemageConfig cfg = VoicemageConfig.INSTANCE;

      // Column 1
      int col1X = boxX + 18;
      this.maxManaBox = new EditBox(this.font, col1X, contentY + 12, 160, 16, Component.literal("Max Mana"));
      this.maxManaBox.setValue(String.valueOf(cfg.maximumMana));
      this.addRenderableWidget(this.maxManaBox);

      this.manaRegenBox = new EditBox(this.font, col1X, contentY + 42, 160, 16, Component.literal("Mana Regen"));
      this.manaRegenBox.setValue(String.valueOf(cfg.manaRegenerationPerSecond));
      this.addRenderableWidget(this.manaRegenBox);

      this.castIntervalBox = new EditBox(this.font, col1X, contentY + 72, 160, 16, Component.literal("Cast Interval"));
      this.castIntervalBox.setValue(String.valueOf(cfg.minimumCastIntervalTicks));
      this.addRenderableWidget(this.castIntervalBox);

      this.flyDurationBox = new EditBox(this.font, col1X, contentY + 102, 78, 16, Component.literal("Fly Duration"));
      this.flyDurationBox.setValue(String.valueOf(cfg.flyDurationTicks));
      this.addRenderableWidget(this.flyDurationBox);

      this.flyCooldownBox = new EditBox(this.font, col1X + 82, contentY + 102, 78, 16, Component.literal("Fly Cooldown"));
      this.flyCooldownBox.setValue(String.valueOf(cfg.flyCooldownTicks));
      this.addRenderableWidget(this.flyCooldownBox);

      // Column 2
      int col2X = boxX + 210;
      this.flyManaBox = new EditBox(this.font, col2X, contentY + 12, 160, 16, Component.literal("Fly Mana Cost"));
      this.flyManaBox.setValue(String.valueOf(cfg.flyManaCost));
      this.addRenderableWidget(this.flyManaBox);

      this.getDoomedManaBox = new EditBox(this.font, col2X, contentY + 42, 160, 16, Component.literal("Get Doomed Mana Cost"));
      this.getDoomedManaBox.setValue(String.valueOf(cfg.getDoomedManaCost));
      this.addRenderableWidget(this.getDoomedManaBox);

      // Toggle Buttons
      this.addRenderableWidget(
         Button.builder(Component.literal("Chant: " + (this.chantRequired ? "§aREQUIRED" : "§cOPTIONAL")), btn -> {
            this.chantRequired = !this.chantRequired;
            btn.setMessage(Component.literal("Chant: " + (this.chantRequired ? "§aREQUIRED" : "§cOPTIONAL")));
         }).bounds(col2X, contentY + 72, 160, 18).build()
      );

      this.addRenderableWidget(
         Button.builder(Component.literal("Pitfall: " + (this.pitfallEnabled ? "§aENABLED" : "§cDISABLED")), btn -> {
            this.pitfallEnabled = !this.pitfallEnabled;
            btn.setMessage(Component.literal("Pitfall: " + (this.pitfallEnabled ? "§aENABLED" : "§cDISABLED")));
         }).bounds(col2X, contentY + 102, 160, 18).build()
      );
   }

   private void initCombatTab(int boxX, int contentY) {
      VoicemageConfig cfg = VoicemageConfig.INSTANCE;

      int col1X = boxX + 18;
      this.explosionPowerBox = new EditBox(this.font, col1X, contentY + 12, 160, 16, Component.literal("Explosion Power"));
      this.explosionPowerBox.setValue(String.valueOf(cfg.explosionPower));
      this.addRenderableWidget(this.explosionPowerBox);

      this.explosionRangeBox = new EditBox(this.font, col1X, contentY + 42, 160, 16, Component.literal("Explosion Range"));
      this.explosionRangeBox.setValue(String.valueOf(cfg.explosionRange));
      this.addRenderableWidget(this.explosionRangeBox);

      this.explosionDelayBox = new EditBox(this.font, col1X, contentY + 72, 160, 16, Component.literal("Explosion Fall Ticks"));
      this.explosionDelayBox.setValue(String.valueOf(cfg.explosionFallTicks));
      this.addRenderableWidget(this.explosionDelayBox);

      this.explosionHeightBox = new EditBox(this.font, col1X, contentY + 102, 160, 16, Component.literal("Explosion Fall Height"));
      this.explosionHeightBox.setValue(String.valueOf(cfg.explosionFallHeight));
      this.addRenderableWidget(this.explosionHeightBox);

      int col2X = boxX + 210;
      this.fireballPowerBox = new EditBox(this.font, col2X, contentY + 12, 160, 16, Component.literal("Fireball Power"));
      this.fireballPowerBox.setValue(String.valueOf(cfg.fireballExplosionPower));
      this.addRenderableWidget(this.fireballPowerBox);

      this.meteorPowerBox = new EditBox(this.font, col2X, contentY + 42, 160, 16, Component.literal("Meteor Power"));
      this.meteorPowerBox.setValue(String.valueOf(cfg.meteorExplosionPower));
      this.addRenderableWidget(this.meteorPowerBox);

      this.lightningDmgBox = new EditBox(this.font, col2X, contentY + 72, 160, 16, Component.literal("Lightning Damage"));
      this.lightningDmgBox.setValue(String.valueOf(cfg.lightningDamage));
      this.addRenderableWidget(this.lightningDmgBox);

      this.healAmountBox = new EditBox(this.font, col2X, contentY + 102, 78, 16, Component.literal("Heal Amount"));
      this.healAmountBox.setValue(String.valueOf(cfg.healAmount));
      this.addRenderableWidget(this.healAmountBox);

      this.skillRangeBox = new EditBox(this.font, col2X + 82, contentY + 102, 78, 16, Component.literal("Skill Range"));
      this.skillRangeBox.setValue(String.valueOf(cfg.skillShotRange));
      this.addRenderableWidget(this.skillRangeBox);
   }

   private void initCustomSpellsTab(int boxX, int contentY) {
      int leftX = boxX + 14;
      int rightX = boxX + 236;

      // Phrase & Mana input row
      this.phraseBox = new EditBox(this.font, leftX, contentY + 10, 160, 16, Component.literal("Spell Phrase"));
      this.phraseBox.setMaxLength(64);
      if (this.selectedCustomSpell != null) {
         this.phraseBox.setValue(this.selectedCustomSpell.phrase);
      }
      this.addRenderableWidget(this.phraseBox);

      this.customManaBox = new EditBox(this.font, leftX + 166, contentY + 10, 44, 16, Component.literal("Mana"));
      this.customManaBox.setMaxLength(5);
      this.customManaBox.setValue(this.selectedCustomSpell != null ? String.valueOf((int)this.selectedCustomSpell.manaCost) : "20");
      this.addRenderableWidget(this.customManaBox);

      // Preset buttons row
      int presetY = contentY + 30;
      this.addRenderableWidget(
         Button.builder(Component.literal("🕺 Dance"), btn -> {
            this.phraseBox.setValue("dance party");
            this.customManaBox.setValue("25");
            this.codeBox.setValue("for (LivingEntity e : ctx.getNearbyLiving(10.0)) {\n   e.setDeltaMovement(0, 0.45, 0);\n   e.setYRot(e.getYRot() + 90.0F);\n   ctx.spawnParticle(\"note\", e.getX(), e.getY() + 1.2, e.getZ(), 5);\n}\nctx.playSound(\"block.note_block.bell\", 1.0F, 1.5F);\nctx.message(\"§d🕺 Dance party!\");");
            this.statusMessage = "Loaded preset: 🕺 Dance Party";
            this.statusColor = 0xFFAAAAAA;
         }).bounds(leftX, presetY, 48, 14).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("✨ Beam"), btn -> {
            this.phraseBox.setValue("triple beam");
            this.customManaBox.setValue("30");
            this.codeBox.setValue("ctx.beam(\"flame, soul_fire_flame, sonic_boom\");\nctx.burst(\"flame, sonic_boom\", ctx.getTargetPos(), 30);\nif (ctx.getTargetEntity() != null) {\n   ctx.damage(ctx.getTargetEntity(), 12.0F);\n}\nctx.playSound(\"entity.warden.sonic_boom\", 1.5F, 1.2F);");
            this.statusMessage = "Loaded preset: ✨ Triple Beam";
            this.statusColor = 0xFFAAAAAA;
         }).bounds(leftX + 50, presetY, 46, 14).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("🌪 Vortex"), btn -> {
            this.phraseBox.setValue("vortex");
            this.customManaBox.setValue("35");
            this.codeBox.setValue("Vec3 center = ctx.getTargetPos();\nctx.burst(\"cloud, portal, reverse_portal\", center, 50);\nfor (LivingEntity e : ctx.getNearbyLiving(8.0)) {\n   ctx.pull(e, 0.8);\n   ctx.damage(e, 4.0F);\n}\nctx.playSound(\"entity.wind_charge.wind_burst\", 1.5F, 0.7F);");
            this.statusMessage = "Loaded preset: 🌪 Vortex";
            this.statusColor = 0xFFAAAAAA;
         }).bounds(leftX + 98, presetY, 46, 14).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("⚡ Zap"), btn -> {
            this.phraseBox.setValue("chain zap");
            this.customManaBox.setValue("40");
            this.codeBox.setValue("ctx.strikeLightning(ctx.getTargetPos());\nfor (LivingEntity e : ctx.getNearbyLiving(6.0)) {\n   ctx.damage(e, 10.0F);\n}\nctx.playSound(\"entity.lightning_bolt.thunder\", 1.2F, 1.0F);");
            this.statusMessage = "Loaded preset: ⚡ Chain Zap";
            this.statusColor = 0xFFAAAAAA;
         }).bounds(leftX + 146, presetY, 32, 14).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("📋 AI"), btn -> this.copyAiPrompt())
            .bounds(leftX + 180, presetY, 30, 14).build()
      );

      // Java Code Editor
      int codeY = contentY + 48;
      this.codeBox = new MultiLineEditBox(
         this.font,
         leftX,
         codeY + 10,
         210,
         58,
         Component.literal("// Type Java code or click a preset..."),
         Component.literal("Java Spell Script"),
         0xFFE0E0E0,
         true,
         0xFFFFFFFF,
         true,
         true
      );
      this.codeBox.setCharacterLimit(8192);
      if (this.selectedCustomSpell != null) {
         this.codeBox.setValue(this.selectedCustomSpell.javaCode != null ? this.selectedCustomSpell.javaCode : "");
      }
      this.addRenderableWidget(this.codeBox);

      // Action buttons row
      int actY = codeY + 72;
      this.addRenderableWidget(
         Button.builder(Component.literal("Test Compile"), btn -> this.testCompileCode())
            .bounds(leftX, actY, 68, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§a+ Save Spell"), btn -> this.saveCustomSpell())
            .bounds(leftX + 72, actY, 86, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§cDelete"), btn -> this.deleteCustomSpell())
            .bounds(leftX + 162, actY, 48, 18)
            .build()
      );

      // Right List Pagination
      int listTopY = contentY;
      this.addRenderableWidget(
         Button.builder(Component.literal("◀"), btn -> {
            if (this.customPage > 0) {
               this.customPage--;
               this.refresh();
            }
         }).bounds(rightX + 110, listTopY - 2, 22, 14).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("▶"), btn -> {
            int maxP = Math.max(0, (VoicemageConfig.INSTANCE.customSpells.size() - 1) / SPELLS_PER_PAGE);
            if (this.customPage < maxP) {
               this.customPage++;
               this.refresh();
            }
         }).bounds(rightX + 134, listTopY - 2, 22, 14).build()
      );

      // Custom Spells list items
      int itemStartY = listTopY + 16;
      List<VoicemageConfig.CustomSpellEntry> spells = VoicemageConfig.INSTANCE.customSpells;
      int startIndex = this.customPage * SPELLS_PER_PAGE;
      for (int i = 0; i < SPELLS_PER_PAGE; i++) {
         int idx = startIndex + i;
         if (idx < spells.size()) {
            VoicemageConfig.CustomSpellEntry entry = spells.get(idx);
            int itemY = itemStartY + i * 27;
            this.addRenderableWidget(
               Button.builder(Component.literal("Pick"), btn -> this.selectCustomSpell(entry))
                  .bounds(rightX + 118, itemY + 2, 38, 16)
                  .build()
            );
         }
      }
   }

   private void selectCustomSpell(VoicemageConfig.CustomSpellEntry entry) {
      this.selectedCustomSpell = entry;
      this.phraseBox.setValue(entry.phrase);
      this.customManaBox.setValue(String.valueOf((int)entry.manaCost));
      if (this.codeBox != null) {
         this.codeBox.setValue(entry.javaCode != null ? entry.javaCode : "");
      }
      this.statusMessage = "Editing \"" + entry.phrase + "\"";
      this.statusColor = 0xFFAAAAAA;
   }

   private void testCompileCode() {
      if (this.codeBox == null) return;
      String code = this.codeBox.getValue().trim();
      if (code.isEmpty()) {
         this.statusMessage = "ℹ Code is empty (will use default beam)";
         this.statusColor = 0xFFFFAA00;
         return;
      }
      String err = CustomSpellCompiler.validate(code);
      if (err == null) {
         this.statusMessage = "✔ Java Syntax OK! Ready to save.";
         this.statusColor = 0xFF55FF55;
      } else {
         if (err.length() > 60) {
            err = err.substring(0, 58) + "..";
         }
         this.statusMessage = "❌ " + err;
         this.statusColor = 0xFFFF5555;
      }
   }

   private void copyAiPrompt() {
      String prompt = """
         You are an expert Minecraft Fabric 26.2 Java programmer. Generate Java code for a VoiceMage custom spell.

         RULES & CONSTRAINTS:
         1. Write ONLY the code inside the spell block (no class or method declaration).
         2. Use explicit types (e.g. 'LivingEntity e', 'Vec3 p') - do NOT use 'var'.
         3. You have access to 'SpellContext ctx' which provides:
            - ctx.getCaster() / ctx.getPlayer() -> ServerPlayer
            - ctx.getLevel() -> ServerLevel
            - ctx.getTargetEntity() -> LivingEntity (or null)
            - ctx.getTargetPos() -> Vec3
            - ctx.getNearbyLiving(double radius) -> List<LivingEntity>
            - ctx.getNearbyFoes(double radius) -> List<LivingEntity>
            - ctx.beam(String particleIds) -> emits beam (e.g. "flame, soul_fire_flame, sonic_boom")
            - ctx.burst(String particleIds, Vec3 pos, int count) -> burst particles
            - ctx.spawnParticle(String id, double x, double y, double z, int count)
            - ctx.makeDance(double radius, int seconds) -> mobs/players jump, spin, play notes
            - ctx.damage(LivingEntity target, float amount) -> magic damage
            - ctx.heal(LivingEntity target, float amount) -> restores health & particles
            - ctx.launch(LivingEntity target, double vx, double vy, double vz) -> fling entity
            - ctx.pull(LivingEntity target, double strength) -> pull towards target
            - ctx.strikeLightning(Vec3 pos) -> summons lightning bolt
            - ctx.applyPotion(LivingEntity target, String effect, int seconds, int amplifier)
            - ctx.playSound(String soundId, float volume, float pitch)
            - ctx.message(String text) -> message caster
            - ctx.runCommand(String command) -> run server command
         4. Standard Minecraft packages are pre-imported: net.minecraft.world.entity.*, net.minecraft.server.level.*, net.minecraft.world.phys.*, net.minecraft.core.particles.*, net.minecraft.sounds.*, net.minecraft.world.effect.*, java.util.*, java.lang.Math.*.

         Create a spell that does: [DESCRIBE YOUR SPELL HERE]
         """.stripIndent();

      if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
         this.minecraft.keyboardHandler.setClipboard(prompt);
         this.statusMessage = "✔ Copied AI Prompt to clipboard! Paste to Gemini/Claude.";
         this.statusColor = 0xFF55FF55;
      }
   }

   private void saveCustomSpell() {
      String phrase = this.phraseBox.getValue().trim();
      if (phrase.isBlank()) {
         this.statusMessage = "Spell phrase cannot be blank!";
         this.statusColor = 0xFFFF5555;
         return;
      }

      String normalized = VoicemageConfig.cleanPhrase(phrase);
      if (normalized.isBlank()) {
         this.statusMessage = "Spell phrase cannot be blank!";
         this.statusColor = 0xFFFF5555;
         return;
      }

      if (Spell.byPhrase(normalized) != null) {
         this.statusMessage = "Phrase is already a built-in spell!";
         this.statusColor = 0xFFFF5555;
         return;
      }

      String javaCode = this.codeBox != null ? this.codeBox.getValue() : "";
      if (!javaCode.trim().isEmpty()) {
         String err = CustomSpellCompiler.validate(javaCode);
         if (err != null) {
            if (err.length() > 60) {
               err = err.substring(0, 58) + "..";
            }
            this.statusMessage = "❌ Compile Error: " + err;
            this.statusColor = 0xFFFF5555;
            return;
         }
      }

      float mana = 20.0F;
      try {
         mana = Float.parseFloat(this.customManaBox.getValue().trim());
      } catch (NumberFormatException ignored) {
      }
      mana = Math.max(0.0F, Math.min(100.0F, mana));

      String particleName = this.selectedCustomSpell != null ? this.selectedCustomSpell.particleName : "minecraft:witch";
      float power = this.selectedCustomSpell != null ? this.selectedCustomSpell.damageOrEffect : 5.0F;

      VoicemageConfig.CustomSpellEntry existing = VoicemageConfig.INSTANCE.findCustomSpell(normalized);
      if (existing != null) {
         existing.particleName = particleName;
         existing.manaCost = mana;
         existing.damageOrEffect = power;
         existing.javaCode = javaCode;
      } else {
         VoicemageConfig.INSTANCE.customSpells.add(
            new VoicemageConfig.CustomSpellEntry(normalized, particleName, mana, power, javaCode)
         );
      }

      VoicemageConfig.save();
      ClientPlayNetworking.send(new SaveCustomSpellPayload(normalized, particleName, mana, power, javaCode));
      VoicemageClient.updateVoiceGrammar();

      this.statusMessage = "Saved custom spell \"" + normalized + "\"!";
      this.statusColor = 0xFF55FF55;
      this.selectedCustomSpell = null;
      this.refresh();
   }

   private void deleteCustomSpell() {
      String phrase = this.phraseBox.getValue().trim();
      if (phrase.isBlank()) {
         this.statusMessage = "Select or enter a phrase to delete!";
         this.statusColor = 0xFFFF5555;
         return;
      }

      String normalized = VoicemageConfig.cleanPhrase(phrase);
      boolean removed = VoicemageConfig.INSTANCE.customSpells.removeIf(
         entry -> entry.phrase != null && VoicemageConfig.cleanPhrase(entry.phrase).equals(normalized)
      );

      if (removed) {
         VoicemageConfig.save();
         ClientPlayNetworking.send(new DeleteCustomSpellPayload(normalized));
         VoicemageClient.updateVoiceGrammar();
         this.statusMessage = "Deleted spell \"" + normalized + "\"!";
         this.statusColor = 0xFFFFAA00;
         this.selectedCustomSpell = null;
         this.refresh();
      } else {
         this.statusMessage = "Spell \"" + normalized + "\" not found!";
         this.statusColor = 0xFFFF5555;
      }
   }

   private void saveCurrentTabValues() {
      VoicemageConfig cfg = VoicemageConfig.INSTANCE;
      if (this.activeTab == TAB_MANA && this.maxManaBox != null) {
         try {
            cfg.maximumMana = Double.parseDouble(this.maxManaBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.manaRegenerationPerSecond = Double.parseDouble(this.manaRegenBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.minimumCastIntervalTicks = Integer.parseInt(this.castIntervalBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.flyDurationTicks = Integer.parseInt(this.flyDurationBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.flyCooldownTicks = Integer.parseInt(this.flyCooldownBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.flyManaCost = Integer.parseInt(this.flyManaBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.getDoomedManaCost = Integer.parseInt(this.getDoomedManaBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         cfg.explosionChantRequired = this.chantRequired;
         cfg.pitfallEnabled = this.pitfallEnabled;
      } else if (this.activeTab == TAB_COMBAT && this.explosionPowerBox != null) {
         try {
            cfg.explosionPower = Float.parseFloat(this.explosionPowerBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.explosionRange = Double.parseDouble(this.explosionRangeBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.explosionFallTicks = Integer.parseInt(this.explosionDelayBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.explosionFallHeight = Double.parseDouble(this.explosionHeightBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.fireballExplosionPower = Integer.parseInt(this.fireballPowerBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.meteorExplosionPower = Float.parseFloat(this.meteorPowerBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.lightningDamage = Double.parseDouble(this.lightningDmgBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.healAmount = Double.parseDouble(this.healAmountBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
         try {
            cfg.skillShotRange = Double.parseDouble(this.skillRangeBox.getValue().trim());
         } catch (NumberFormatException ignored) {
         }
      } else if (this.activeTab == TAB_CUSTOM && this.phraseBox != null) {
         String phrase = this.phraseBox.getValue().trim();
         if (!phrase.isBlank()) {
            this.saveCustomSpell();
         }
      }
   }

   private void saveAllSettings() {
      this.saveCurrentTabValues();
      VoicemageConfig.save();
      String json = VoicemageConfig.fullConfigJson();
      ClientPlayNetworking.send(new SaveSettingsPayload(json));
      VoicemageClient.updateVoiceGrammar();

      this.statusMessage = "✔ All settings saved & applied to server!";
      this.statusColor = 0xFF55FF55;
   }

   private void refresh() {
      this.clearWidgets();
      this.init();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      // Dark slate background overlay
      graphics.fill(0, 0, this.width, this.height, 0xD008090E);

      int boxW = 390;
      int boxH = 226;
      int boxX = (this.width - boxW) / 2;
      int boxY = (this.height - boxH) / 2;

      // Container outer bevel and shadow
      graphics.fill(boxX + 3, boxY + 3, boxX + boxW + 3, boxY + boxH + 3, 0x80000000);
      graphics.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF352B4D);
      graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF191A24);
      graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFA12131A);

      // Header Banner
      graphics.fill(boxX, boxY, boxX + boxW, boxY + 26, 0xFF1B1D28);
      graphics.fill(boxX, boxY + 26, boxX + boxW, boxY + 27, 0xFFD4AF37);
      graphics.centeredText(this.font, "§6§l✦ VOICEMAGE CONFIGURATION ✦§r", this.width / 2, boxY + 8, 0xFFFFAA00);
      graphics.text(this.font, "§d[OP CONFIG]§r", boxX + 8, boxY + 9, 0xFFFF77FF, false);

      // Content Header labels
      int tabY = boxY + 28;
      int contentY = tabY + 24;

      if (this.activeTab == TAB_MANA) {
         int col1X = boxX + 18;
         graphics.text(this.font, "Max Mana Pool:", col1X, contentY + 2, 0xFFDDDDDD, true);
         graphics.text(this.font, "Mana Regen (/sec):", col1X, contentY + 32, 0xFFDDDDDD, true);
         graphics.text(this.font, "Cast Interval (ticks):", col1X, contentY + 62, 0xFFDDDDDD, true);
         graphics.text(this.font, "Fly Dur / CD (ticks):", col1X, contentY + 92, 0xFFDDDDDD, true);

         int col2X = boxX + 210;
         graphics.text(this.font, "Fly Mana Cost:", col2X, contentY + 2, 0xFFDDDDDD, true);
         graphics.text(this.font, "Get Doomed Mana Cost:", col2X, contentY + 32, 0xFFDDDDDD, true);
         graphics.text(this.font, "Explosion Chant Mode:", col2X, contentY + 62, 0xFFDDDDDD, true);
         graphics.text(this.font, "Terrain Pitfall Trap:", col2X, contentY + 92, 0xFFDDDDDD, true);
      } else if (this.activeTab == TAB_COMBAT) {
         int col1X = boxX + 18;
         graphics.text(this.font, "Explosion Power:", col1X, contentY + 2, 0xFFFF8888, true);
         graphics.text(this.font, "Explosion Range:", col1X, contentY + 32, 0xFFFF8888, true);
         graphics.text(this.font, "Explosion Delay (ticks):", col1X, contentY + 62, 0xFFFF8888, true);
         graphics.text(this.font, "Explosion Fall Height:", col1X, contentY + 92, 0xFFFF8888, true);

         int col2X = boxX + 210;
         graphics.text(this.font, "Fireball Power:", col2X, contentY + 2, 0xFFFFBB66, true);
         graphics.text(this.font, "Meteor Power:", col2X, contentY + 32, 0xFFFFBB66, true);
         graphics.text(this.font, "Lightning Damage (HP):", col2X, contentY + 62, 0xFF88CCFF, true);
         graphics.text(this.font, "Heal HP:", col2X, contentY + 92, 0xFF88FF88, true);
         graphics.text(this.font, "Skill Range:", col2X + 82, contentY + 92, 0xFF88CCFF, true);
      } else {
         int leftX = boxX + 14;
         int rightX = boxX + 236;
         graphics.text(this.font, "Spell Phrase:", leftX, contentY, 0xFFDDDDDD, true);
         graphics.text(this.font, "Mana:", leftX + 166, contentY, 0xFFDDDDDD, true);
         graphics.text(this.font, "Presets / Helpers:", leftX, contentY + 20, 0xFFFFAA00, false);
         graphics.text(this.font, "Java Code (Janino / ctx):", leftX, contentY + 46, 0xFFDDDDDD, true);

         List<VoicemageConfig.CustomSpellEntry> spells = VoicemageConfig.INSTANCE.customSpells;
         int totalSpells = spells.size();
         graphics.text(this.font, "Registered (" + totalSpells + "):", rightX, contentY, 0xFFFFAA00, true);

         int itemStartY = contentY + 16;
         if (spells.isEmpty()) {
            graphics.text(this.font, "No custom spells yet.", rightX, itemStartY + 10, 0xFF888888, false);
            graphics.text(this.font, "Pick a preset on left!", rightX, itemStartY + 24, 0xFF888888, false);
         } else {
            int startIndex = this.customPage * SPELLS_PER_PAGE;
            for (int i = 0; i < SPELLS_PER_PAGE; i++) {
               int idx = startIndex + i;
               if (idx < spells.size()) {
                  VoicemageConfig.CustomSpellEntry entry = spells.get(idx);
                  int itemY = itemStartY + i * 27;
                  graphics.fill(rightX, itemY, rightX + 158, itemY + 23, 0x50000000);
                  graphics.fill(rightX, itemY, rightX + 2, itemY + 23, 0xFF55FF55);

                  String phraseText = "\"" + entry.phrase + "\"";
                  if (phraseText.length() > 14) {
                     phraseText = phraseText.substring(0, 12) + "..";
                  }
                  graphics.text(this.font, phraseText, rightX + 5, itemY + 3, 0xFF55FF55, true);

                  String details;
                  if (entry.javaCode != null && !entry.javaCode.isBlank()) {
                     details = "§dJava Spell§r (M:" + (int)entry.manaCost + ")";
                  } else {
                     details = "Legacy (M:" + (int)entry.manaCost + " P:" + entry.damageOrEffect + ")";
                  }
                  if (details.length() > 18) {
                     details = details.substring(0, 16) + "..";
                  }
                  graphics.text(this.font, details, rightX + 5, itemY + 13, 0xFFAAAAAA, false);
               }
            }
         }
      }

      // Separator and Status message above bottom bar
      int bottomSepY = boxY + boxH - 28;
      graphics.fill(boxX, bottomSepY, boxX + boxW, bottomSepY + 1, 0xFF2A2D3D);

      if (!this.statusMessage.isEmpty()) {
         graphics.centeredText(this.font, this.statusMessage, this.width / 2, bottomSepY - 11, this.statusColor);
      }

      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
   }
}
