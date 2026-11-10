package com.voicemage;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.Spell;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class VoicemageCommands {
   private VoicemageCommands() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, access, environment) -> {
         dispatcher.register(spellGuideCommand("spells"));
         dispatcher.register(spellGuideCommand("grimoire"));
         dispatcher.register(spellGuideCommand("grimwore"));
         dispatcher.register(spellGuideCommand("g"));
         dispatcher.register(voicemageCommand());
      });
   }

   private static LiteralArgumentBuilder<CommandSourceStack> spellGuideCommand(String alias) {
      return Commands.literal(alias)
         .executes(context -> {
            CommandSourceStack source = (CommandSourceStack)context.getSource();
            ServerPlayer player = source.getPlayer();
            if (player != null) {
               VoicemageNetworking.openSpellsScreen(player);
               return 1;
            }
            return showSpellsList(source);
         });
   }

   private static int showSpellsList(CommandSourceStack source) {
      StringBuilder message = new StringBuilder("§6=== VoiceMage Spells ===§r");
      for (Spell spell : Spell.values()) {
         message.append("\n§e").append(spell.displayName()).append("§r — §b").append(VoicemageConfig.INSTANCE.manaCost(spell)).append(" mana§r (Say: §f\"").append(spell.phrase()).append("\"§r)");
      }

      if (!VoicemageConfig.INSTANCE.customSpells.isEmpty()) {
         message.append("\n\n§6Custom Spells:§r");
         for (VoicemageConfig.CustomSpellEntry custom : VoicemageConfig.INSTANCE.customSpells) {
            message.append("\n§a").append(custom.phrase).append("§r (particle: §b").append(custom.particleName).append("§r) — §d").append((int)Math.ceil(custom.manaCost)).append(" mana§r, power: §c").append(custom.damageOrEffect).append("§r");
         }
      }

      source.sendSuccess(() -> Component.literal(message.toString()), false);
      return Spell.values().length + VoicemageConfig.INSTANCE.customSpells.size();
   }

   private static LiteralArgumentBuilder<CommandSourceStack> voicemageCommand() {
      LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("voicemage")
         .executes(
            context -> {
               CommandSourceStack source = (CommandSourceStack)context.getSource();
               ServerPlayer player = source.getPlayer();
               if (player != null) {
                  VoicemageNetworking.openSpellsScreen(player);
                  return 1;
               }
               VoicemageConfig config = VoicemageConfig.INSTANCE;
               String chantRule = config.explosionChantRequired ? "Explosion requires a full chant." : "Say Explosion to cast it.";
               source.sendSuccess(
                  () -> Component.literal(
                     "voicemage — Fireball, Pitfall, Heal, Lightning, Earth Wall, Blink, Magic Missile, Meteor, Wind Blast, Freeze, Clone, Polymorph, Shrink, Explosion, Fly, and Get Doomed. Maximum mana "
                        + config.maximumMana
                        + ", regeneration "
                        + config.manaRegenerationPerSecond
                        + "/second. "
                        + chantRule
                  ),
                  false
               );
               return 1;
            }
         );
      root.then(((LiteralArgumentBuilder)Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(context -> {
         boolean loaded = VoicemageConfig.load();
         VoicemageNetworking.broadcastSettings(((CommandSourceStack)context.getSource()).getServer());
         if (!loaded) {
            ((CommandSourceStack)context.getSource())
               .sendFailure(Component.literal("config/voicemage.json could not be read. The previous settings stay active; a backup copy of the file was saved next to it."));
            return 0;
         }

         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Reloaded config/voicemage.json."), false);
         return 1;
      }));
      root.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("regen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(context -> {
                  double rate = VoicemageConfig.INSTANCE.manaRegenerationPerSecond;
                  ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Mana regeneration is " + rate + " mana/second."), false);
                  return 1;
               }))
            .then(Commands.argument("manaPerSecond", DoubleArgumentType.doubleArg(0.0, 100.0)).executes(context -> {
               double rate = DoubleArgumentType.getDouble(context, "manaPerSecond");
               VoicemageConfig.setManaRegenerationPerSecond(rate);
               ((CommandSourceStack)context.getSource())
                  .sendSuccess(() -> Component.literal("Mana regeneration set to " + rate + " mana/second and saved."), true);
               return 1;
            }))
      );

      for (Spell spell : Spell.values()) {
         root.then(spellCostCommand(spell));
      }

      root.then(chantCommand());
      root.then(settingsCommand());
      root.then(spellGuideCommand("spells"));
      root.then(spellGuideCommand("grimoire"));
      root.then(spellGuideCommand("grimwore"));
      root.then(spellGuideCommand("g"));
      return root;
   }

   private static LiteralArgumentBuilder<CommandSourceStack> spellCostCommand(Spell spell) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                  spell.name().toLowerCase(Locale.ROOT)
               )
               .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .executes(
               context -> {
                  ((CommandSourceStack)context.getSource())
                     .sendSuccess(() -> Component.literal(spell.displayName() + ": " + VoicemageConfig.INSTANCE.manaCost(spell) + " mana"), false);
                  return 1;
               }
            ))
         .then(Commands.argument("cost", IntegerArgumentType.integer(0, 100)).executes(context -> {
            int cost = IntegerArgumentType.getInteger(context, "cost");
            VoicemageConfig.setManaCost(spell, cost);
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(spell.displayName() + " now costs " + cost + " mana."), true);
            return 1;
         }));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> chantCommand() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("chant")
               .executes(
                  context -> {
                     ((CommandSourceStack)context.getSource())
                        .sendSuccess(() -> Component.literal("Explosion full chant required: " + VoicemageConfig.INSTANCE.explosionChantRequired), false);
                     return 1;
                  }
               ))
            .then(
               Commands.literal("show")
                  .executes(
                     context -> {
                        ((CommandSourceStack)context.getSource())
                           .sendSuccess(
                              () -> Component.literal(
                                 "Explosion chants\n\nO ancient flame, awaken from your endless slumber. Let the skies burn and turn my enemies to ash, leaving nothing behind. EXPLOSION!\n\nDarkness blacker than black and darker than dark,\nI beseech thee, combine with my deep crimson.\nReturn all creation to cinders,\nand come from the abyss!\nEXPLOSION!"
                              ),
                              false
                           );
                        return 2;
                     }
                  )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("required", BoolArgumentType.bool()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  context -> {
                     boolean required = BoolArgumentType.getBool(context, "required");
                     VoicemageConfig.INSTANCE.explosionChantRequired = required;
                     VoicemageConfig.save();
                     VoicemageNetworking.broadcastSettings(((CommandSourceStack)context.getSource()).getServer());
                     ((CommandSourceStack)context.getSource())
                        .sendSuccess(
                           () -> Component.literal(required ? "Explosion now requires either full chant." : "Explosion can now be cast by saying Explosion."),
                           true
                        );
                     return 1;
                  }
               )
         );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> settingsCommand() {
      LiteralArgumentBuilder<CommandSourceStack> settings = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("settings")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
         .executes(context -> {
            CommandSourceStack source = (CommandSourceStack)context.getSource();
            ServerPlayer player = source.getPlayer();
            if (player != null) {
               VoicemageNetworking.openSettingsScreen(player);
               return 1;
            }
            return showSettingsMenu(source);
         });

      settings.then(Commands.literal("list").executes(context -> listCustomSpells((CommandSourceStack)context.getSource())));

      settings.then(
         ((LiteralArgumentBuilder)Commands.literal("clear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .executes(context -> {
               CommandSourceStack source = (CommandSourceStack)context.getSource();
               int count = VoicemageConfig.INSTANCE.customSpells.size();
               VoicemageConfig.INSTANCE.customSpells.clear();
               VoicemageConfig.save();
               VoicemageNetworking.broadcastSettings(source.getServer());
               source.sendSuccess(() -> Component.literal("Cleared " + count + " custom spell(s)."), true);
               return count;
            })
      );

      settings.then(
         ((LiteralArgumentBuilder)Commands.literal("remove").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("phrase", StringArgumentType.string())
                  .executes(context -> removeCustomSpell((CommandSourceStack)context.getSource(), StringArgumentType.getString(context, "phrase")))
            )
      );

      settings.then(
         ((LiteralArgumentBuilder)Commands.literal("add").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("phrase", StringArgumentType.string())
                  .then(
                     Commands.argument("particleName", StringArgumentType.string())
                        .executes(context -> addCustomSpell(
                           (CommandSourceStack)context.getSource(),
                           StringArgumentType.getString(context, "phrase"),
                           StringArgumentType.getString(context, "particleName"),
                           20.0F,
                           5.0F
                        ))
                        .then(
                           Commands.argument("manaCost", FloatArgumentType.floatArg(0.0F, 100.0F))
                              .executes(context -> addCustomSpell(
                                 (CommandSourceStack)context.getSource(),
                                 StringArgumentType.getString(context, "phrase"),
                                 StringArgumentType.getString(context, "particleName"),
                                 FloatArgumentType.getFloat(context, "manaCost"),
                                 5.0F
                              ))
                              .then(
                                 Commands.argument("damageOrEffect", FloatArgumentType.floatArg(0.0F, 1000.0F))
                                    .executes(context -> addCustomSpell(
                                       (CommandSourceStack)context.getSource(),
                                       StringArgumentType.getString(context, "phrase"),
                                       StringArgumentType.getString(context, "particleName"),
                                       FloatArgumentType.getFloat(context, "manaCost"),
                                       FloatArgumentType.getFloat(context, "damageOrEffect")
                                    ))
                              )
                        )
                  )
            )
      );

      return settings;
   }

   private static int showSettingsMenu(CommandSourceStack source) {
      VoicemageConfig config = VoicemageConfig.INSTANCE;
      StringBuilder builder = new StringBuilder();
      builder.append("§6=== VoiceMage Settings ===§r\n");
      builder.append("§eMax Mana:§r ").append(config.maximumMana).append("\n");
      builder.append("§eMana Regeneration:§r ").append(config.manaRegenerationPerSecond).append(" /sec\n");
      builder.append("§eExplosion Chant Required:§r ").append(config.explosionChantRequired).append("\n");
      builder.append("§eCustom Spells (").append(config.customSpells.size()).append("):§r\n");

      if (config.customSpells.isEmpty()) {
         builder.append("  §7(No custom spells configured yet)§r\n");
      } else {
         for (VoicemageConfig.CustomSpellEntry spell : config.customSpells) {
            builder.append("  §a\"").append(spell.phrase).append("\"§r -> §b")
                   .append(spell.particleName).append("§r (cost: §d")
                   .append(spell.manaCost).append("§r, effect: §c")
                   .append(spell.damageOrEffect).append("§r)\n");
         }
      }

      builder.append("\n§eCommands:§r\n");
      builder.append("  §f/voicemage settings add <phrase> <particleName> [manaCost] [damageOrEffect]§r\n");
      builder.append("  §f/voicemage settings remove <phrase>§r\n");
      builder.append("  §f/voicemage settings list§r\n");
      builder.append("  §f/voicemage settings clear§r");

      source.sendSuccess(() -> Component.literal(builder.toString()), false);
      return 1;
   }

   private static int listCustomSpells(CommandSourceStack source) {
      VoicemageConfig config = VoicemageConfig.INSTANCE;
      if (config.customSpells.isEmpty()) {
         source.sendSuccess(() -> Component.literal("No custom spells registered. Use '/voicemage settings add <phrase> <particleName>' to add one."), false);
         return 0;
      }

      StringBuilder builder = new StringBuilder("§6Registered Custom Spells (" + config.customSpells.size() + "):§r\n");
      for (int i = 0; i < config.customSpells.size(); i++) {
         VoicemageConfig.CustomSpellEntry spell = config.customSpells.get(i);
         builder.append(i + 1).append(". §a\"").append(spell.phrase)
                .append("\"§r — Particle: §b").append(spell.particleName)
                .append("§r, Mana: §d").append(spell.manaCost)
                .append("§r, Power: §c").append(spell.damageOrEffect).append("§r\n");
      }
      source.sendSuccess(() -> Component.literal(builder.toString().trim()), false);
      return config.customSpells.size();
   }

   private static int addCustomSpell(CommandSourceStack source, String phrase, String particleName, float manaCost, float damageOrEffect) {
      if (phrase == null || phrase.isBlank()) {
         source.sendFailure(Component.literal("Spell phrase cannot be blank."));
         return 0;
      }

      String normalizedPhrase = phrase.trim().toLowerCase(Locale.ROOT);
      if (Spell.byPhrase(normalizedPhrase) != null) {
         source.sendFailure(Component.literal("Phrase \"" + phrase + "\" is already used by a built-in spell."));
         return 0;
      }

      ParticleType<?> particleType = VoicemageConfig.parseParticle(particleName);
      if (particleType == null) {
         source.sendFailure(Component.literal("Invalid particle name: \"" + particleName + "\". Valid examples: minecraft:flame, minecraft:witch, minecraft:soul_fire_flame, minecraft:sonic_boom"));
         return 0;
      }

      float clampedMana = Math.max(0.0F, Math.min(100.0F, manaCost));
      float clampedDamage = Math.max(0.0F, damageOrEffect);

      VoicemageConfig.CustomSpellEntry existing = VoicemageConfig.INSTANCE.findCustomSpell(normalizedPhrase);
      if (existing != null) {
         existing.particleName = particleName.trim().toLowerCase(Locale.ROOT);
         existing.manaCost = clampedMana;
         existing.damageOrEffect = clampedDamage;
      } else {
         VoicemageConfig.INSTANCE.customSpells.add(
            new VoicemageConfig.CustomSpellEntry(normalizedPhrase, particleName.trim().toLowerCase(Locale.ROOT), clampedMana, clampedDamage)
         );
      }

      VoicemageConfig.save();
      VoicemageNetworking.broadcastSettings(source.getServer());

      source.sendSuccess(
         () -> Component.literal("Custom spell \"" + normalizedPhrase + "\" saved with particle " + particleName + " (cost: " + clampedMana + ", effect: " + clampedDamage + ")."),
         true
      );
      return 1;
   }

   private static int removeCustomSpell(CommandSourceStack source, String phrase) {
      if (phrase == null || phrase.isBlank()) {
         source.sendFailure(Component.literal("Phrase cannot be blank."));
         return 0;
      }

      String normalized = phrase.trim().toLowerCase(Locale.ROOT);
      boolean removed = VoicemageConfig.INSTANCE.customSpells.removeIf(
         entry -> entry.phrase != null && entry.phrase.trim().toLowerCase(Locale.ROOT).equals(normalized)
      );

      if (removed) {
         VoicemageConfig.save();
         VoicemageNetworking.broadcastSettings(source.getServer());
         source.sendSuccess(() -> Component.literal("Removed custom spell \"" + normalized + "\"."), true);
         return 1;
      } else {
         source.sendFailure(Component.literal("No custom spell found with phrase \"" + phrase + "\"."));
         return 0;
      }
   }
}
