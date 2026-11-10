package com.voicemage.spell.custom;

import com.voicemage.Voicemage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.janino.ScriptEvaluator;

public final class CustomSpellCompiler {
   private static final Map<String, ScriptEvaluator> CACHE = new ConcurrentHashMap<>();

   private static final String[] DEFAULT_IMPORTS = new String[] {
      "net.minecraft.world.entity.*",
      "net.minecraft.world.entity.player.*",
      "net.minecraft.server.level.*",
      "net.minecraft.world.phys.*",
      "net.minecraft.core.particles.*",
      "net.minecraft.sounds.*",
      "net.minecraft.world.effect.*",
      "net.minecraft.core.*",
      "com.voicemage.spell.custom.*",
      "java.util.*",
      "java.lang.Math.*"
   };

   private CustomSpellCompiler() {
   }

   private static ScriptEvaluator createEvaluator() throws Exception {
      ScriptEvaluator se = new ScriptEvaluator();
      se.setParentClassLoader(SpellContext.class.getClassLoader());
      se.setDefaultImports(DEFAULT_IMPORTS);
      se.setParameters(new String[] { "ctx" }, new Class<?>[] { SpellContext.class });
      se.setReturnType(void.class);
      return se;
   }

   /**
    * Validates the given Java code snippet using Janino.
    * @return null if code is valid, or the error message string if compilation failed.
    */
   public static String validate(String javaCode) {
      if (javaCode == null || javaCode.trim().isEmpty()) {
         return null;
      }
      try {
         ScriptEvaluator se = createEvaluator();
         se.cook(javaCode);
         return null;
      } catch (Throwable t) {
         Throwable cause = t;
         while (cause.getCause() != null) {
            cause = cause.getCause();
         }
         String msg = cause.getMessage();
         if (msg == null || msg.isBlank()) {
            msg = cause.getClass().getSimpleName();
         }
         return msg;
      }
   }

   /**
    * Executes a custom spell Java snippet with Janino in memory.
    * Catches runtime and compilation errors safely and notifies the caster.
    */
   public static boolean execute(String javaCode, SpellContext ctx) {
      if (javaCode == null || javaCode.trim().isEmpty()) {
         return false;
      }
      try {
         ScriptEvaluator se = CACHE.computeIfAbsent(javaCode, code -> {
            try {
               ScriptEvaluator evaluator = createEvaluator();
               evaluator.cook(code);
               return evaluator;
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         });
         se.evaluate(new Object[] { ctx });
         return true;
      } catch (Throwable t) {
         Throwable cause = t;
         while (cause.getCause() != null) {
            cause = cause.getCause();
         }
         String errorMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
         ctx.message("§c[Spell Error] " + errorMsg);
         Voicemage.LOGGER.error("Custom spell execution failed: " + errorMsg, t);
         return false;
      }
   }

   public static void clearCache() {
      CACHE.clear();
   }
}
