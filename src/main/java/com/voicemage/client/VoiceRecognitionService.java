package com.voicemage.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voicemage.Voicemage;
import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.Spell;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

public final class VoiceRecognitionService {
   private static final float SAMPLE_RATE = 16000.0F;
   /** Words must exist in the speech model's vocabulary, otherwise Vosk ignores them (so no "earthwall"/"windblast"). */
   private static final List<String> COMMAND_PHRASES = List.of(
      "fireball", "fire ball", "pitfall", "pit fall", "heal", "lightning", "lightening", "earth wall", "blink", "magic missile",
      "magic missle", "meteor", "wind blast", "freeze", "clone", "polymorph", "poly morph", "shrink",
      "fly", "get doomed"
   );
   private final BiConsumer<Spell, String> onSpell;
   private volatile boolean running;
   private volatile boolean explosionChantRequired = true;
   private volatile int grammarVersion = 0;
   private volatile String status = "Voice: starting...";
   private volatile String partialText = "";
   private volatile MicrophoneCapture microphone;
   private volatile long generation;
   private Thread thread;

   public VoiceRecognitionService(BiConsumer<Spell, String> onSpell) {
      this.onSpell = onSpell;
   }

   private static String grammar(boolean explosionAloneAllowed) {
      List<String> phrases = new ArrayList<>(COMMAND_PHRASES);
      phrases.add(Spell.EXPLOSION.phrase());
      phrases.add(Spell.ALTERNATIVE_CHANT);
      if (explosionAloneAllowed) {
         phrases.add("explosion");
      }

      if (VoicemageConfig.INSTANCE.customSpells != null) {
         for (VoicemageConfig.CustomSpellEntry custom : VoicemageConfig.INSTANCE.customSpells) {
            if (custom.phrase != null && !custom.phrase.isBlank()) {
               String phrase = VoicemageConfig.cleanPhrase(custom.phrase);
               if (!phrase.isBlank() && !phrases.contains(phrase)) {
                  phrases.add(phrase);
               }
            }
            if (custom.chant != null && !custom.chant.isBlank()) {
               String chant = VoicemageConfig.cleanPhrase(custom.chant);
               if (!chant.isBlank() && !phrases.contains(chant)) {
                  phrases.add(chant);
               }
            }
         }
      }

      phrases.add("[unk]");
      JsonArray array = new JsonArray();
      phrases.forEach(array::add);
      return array.toString();
   }

   public void updateGrammar() {
      this.grammarVersion++;
   }

   public void setExplosionChantRequired(boolean required) {
      this.explosionChantRequired = required;
   }

   public boolean explosionChantRequired() {
      return this.explosionChantRequired;
   }

   public String status() {
      return this.status;
   }

   public String displayStatus() {
      String partial = this.partialText;
      return this.isListening() && !partial.isBlank() ? "Hearing: \"" + partial + "\"" : this.status;
   }

   public boolean isRunning() {
      return this.running;
   }

   public boolean isListening() {
      return this.running && "Voice: listening".equals(this.status);
   }

   public synchronized void start() {
      if (!this.running) {
         this.running = true;
         this.partialText = "";
         this.status = "Voice: starting...";
         long runId = ++this.generation;
         this.thread = new Thread(() -> this.run(runId), "voicemage speech recognition");
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   public synchronized void stop() {
      this.generation++;
      this.running = false;
      MicrophoneCapture active = this.microphone;
      if (active != null) {
         active.close();
      }

      if (this.thread != null) {
         this.thread.interrupt();
      }

      this.microphone = null;
      this.partialText = "";
      this.status = "Voice: muted (press V to resume)";
   }

   private void run(long runId) {
      Model model = null;

      try {
         Path modelPath = SpeechModelInstaller.ensureInstalled(value -> this.setStatus(runId, value));
         if (!this.isCurrent(runId)) {
            return;
         }

         this.setStatus(runId, "Voice: loading speech model...");
         LibVosk.setLogLevel(LogLevel.WARNINGS);
         model = SharedSpeechModel.acquire(modelPath);
         if (!this.isCurrent(runId)) {
            return;
         }

         boolean chantRequired = this.explosionChantRequired;

         try (MicrophoneCapture capture = MicrophoneCapture.open(ClientSettings.microphone())) {
            if (!this.isCurrent(runId)) {
               return;
            }

            this.microphone = capture;
            this.status = "Voice: listening";
            Voicemage.LOGGER.info("Voice recognition is listening on: {}", capture.describe());
            this.recognizeLoop(runId, capture, model, chantRequired);
         }
      } catch (Exception error) {
         if (this.isCurrent(runId)) {
            this.status = "Voice error: " + concise(error.getMessage());
            Voicemage.LOGGER.error("Voice recognition stopped", error);
         }
      } finally {
         if (model != null) {
            SharedSpeechModel.release();
         }

         if (this.generation == runId) {
            MicrophoneCapture active = this.microphone;
            if (active != null) {
               active.close();
            }

            this.microphone = null;
            this.running = false;
         }
      }
   }

   private void recognizeLoop(long runId, MicrophoneCapture capture, Model model, boolean initialChantRequired) {
      byte[] buffer = new byte[MicrophoneCapture.OUTPUT_BUFFER_BYTES];
      String stablePartial = "";
      int stableCount = 0;
      Spell lastSpell = null;
      String lastCustomPhrase = null;
      long lastCastNanos = 0L;
      boolean activeChantRequired = initialChantRequired;
      int activeGrammarVersion = this.grammarVersion;
      Recognizer recognizer = null;

      try {
         recognizer = new Recognizer(model, SAMPLE_RATE, grammar(activeChantRequired));

         while (this.isCurrent(runId) && !Thread.currentThread().isInterrupted()) {
            boolean required = this.explosionChantRequired;
            int currentGrammarVersion = this.grammarVersion;
            if (activeChantRequired != required || activeGrammarVersion != currentGrammarVersion) {
               try {
                  recognizer.close();
               } catch (Exception ignored) {
               }
               recognizer = new Recognizer(model, SAMPLE_RATE, grammar(required));
               activeChantRequired = required;
               activeGrammarVersion = currentGrammarVersion;
               stablePartial = "";
               stableCount = 0;
               this.partialText = "";
               Voicemage.LOGGER.info("Voice recognition grammar refreshed (version {})", currentGrammarVersion);
            }

            int read = capture.read(buffer);
            if (read < 0) {
               break;
            }

            if (read > 0) {
               if (recognizer.acceptWaveForm(buffer, read)) {
                  String text = cleanJsonText(recognizer.getResult(), "text");
                  this.partialText = text;
                  if (!text.isBlank()) {
                     Spell spell = Spell.byPhrase(text, this.explosionChantRequired);
                     if (spell != null && allowed(spell, lastSpell, lastCastNanos)) {
                        this.onSpell.accept(spell, text);
                        lastSpell = spell;
                        lastCustomPhrase = null;
                        lastCastNanos = System.nanoTime();
                     } else if (spell == null) {
                        VoicemageConfig.CustomSpellEntry custom = VoicemageConfig.INSTANCE.findCustomSpell(text);
                        if (custom != null && allowedCustom(custom.phrase, lastCustomPhrase, lastCastNanos)) {
                           Voicemage.LOGGER.info("Recognized custom spell: {}", custom.phrase);
                           this.onSpell.accept(null, custom.phrase);
                           lastSpell = null;
                           lastCustomPhrase = custom.phrase;
                           lastCastNanos = System.nanoTime();
                        }
                     }
                  }

                  stablePartial = "";
                  stableCount = 0;
               } else {
                  String partial = cleanJsonText(recognizer.getPartialResult(), "partial");
                  this.partialText = partial;
                  if (partial.equals(stablePartial)) {
                     stableCount++;
                  } else {
                     stablePartial = partial;
                     stableCount = 1;
                  }

                  if (!partial.isBlank() && stableCount >= 2) {
                     Spell spell = Spell.byPhrase(partial, this.explosionChantRequired);
                     if (spell != null && allowed(spell, lastSpell, lastCastNanos)) {
                        this.onSpell.accept(spell, partial);
                        lastSpell = spell;
                        lastCustomPhrase = null;
                        lastCastNanos = System.nanoTime();
                        recognizer.reset();
                        this.partialText = "";
                        stablePartial = "";
                        stableCount = 0;
                     } else if (spell == null) {
                        VoicemageConfig.CustomSpellEntry custom = VoicemageConfig.INSTANCE.findCustomSpell(partial);
                        if (custom != null && allowedCustom(custom.phrase, lastCustomPhrase, lastCastNanos)) {
                           Voicemage.LOGGER.info("Recognized custom spell (partial): {}", custom.phrase);
                           this.onSpell.accept(null, custom.phrase);
                           lastSpell = null;
                           lastCustomPhrase = custom.phrase;
                           lastCastNanos = System.nanoTime();
                           recognizer.reset();
                           this.partialText = "";
                           stablePartial = "";
                           stableCount = 0;
                        }
                     }
                  }
               }
            }
         }
      } finally {
         if (recognizer != null) {
            try {
               recognizer.close();
            } catch (Exception ignored) {
            }
         }
      }
   }

   private boolean isCurrent(long runId) {
      return this.running && this.generation == runId;
   }

   private void setStatus(long runId, String value) {
      if (this.isCurrent(runId)) {
         this.status = value;
      }
   }

   private static boolean allowed(Spell current, Spell previous, long previousNanos) {
      long elapsed = System.nanoTime() - previousNanos;
      long debounce = current == previous ? 1500000000L : 350000000L;
      return elapsed >= debounce;
   }

   private static boolean allowedCustom(String current, String previous, long previousNanos) {
      long elapsed = System.nanoTime() - previousNanos;
      long debounce = (previous != null && previous.equalsIgnoreCase(current)) ? 1500000000L : 350000000L;
      return elapsed >= debounce;
   }

   private static String cleanJsonText(String json, String key) {
      String raw = jsonText(json, key);
      if (raw == null) {
         return "";
      }
      return raw.replace("[unk]", "")
                .replaceAll("(?i)\\bunk\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
   }

   private static String jsonText(String json, String key) {
      try {
         JsonObject object = JsonParser.parseString(json).getAsJsonObject();
         return object.has(key) ? object.get(key).getAsString() : "";
      } catch (RuntimeException ignored) {
         return "";
      }
   }

   private static String concise(String message) {
      if (message != null && !message.isBlank()) {
         return message.length() <= 80 ? message : message.substring(0, 77) + "...";
      } else {
         return "unknown problem";
      }
   }
}
