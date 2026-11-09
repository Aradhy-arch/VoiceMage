package com.voicemage.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Unpacks the speech model that is built into the mod jar (see build.gradle). Nothing is ever downloaded: the model
 * comes with the mod, so voice recognition works offline from the first start.
 */
final class SpeechModelInstaller {
   private static final String MODEL_NAME = "vosk-model-small-en-us-0.15";
   private static final String BUNDLED_MODEL = "/assets/voicemage/model/" + MODEL_NAME + ".zip";
   private static final long MAX_EXTRACTED_BYTES = 600L * 1024L * 1024L;

   private SpeechModelInstaller() {
   }

   static synchronized Path ensureInstalled(SpeechModelInstaller.StatusSink status) throws IOException {
      return ensureInstalled(FabricLoader.getInstance().getGameDir().resolve("voicemage"), status);
   }

   static synchronized Path ensureInstalled(Path parent, SpeechModelInstaller.StatusSink status) throws IOException {
      Path model = parent.resolve(MODEL_NAME);
      if (valid(model)) {
         return model;
      }

      status.set("Unpacking speech model (first start only)...");
      Files.createDirectories(parent);
      deleteTree(model);
      Path installing = parent.resolve(MODEL_NAME + ".installing");
      deleteTree(installing);
      Files.createDirectories(installing);

      try (InputStream bundled = SpeechModelInstaller.class.getResourceAsStream(BUNDLED_MODEL)) {
         if (bundled == null) {
            throw new IOException("This build of VoiceMage does not contain the speech model");
         }

         extractModel(bundled, installing);
         if (!valid(installing)) {
            throw new IOException("The built-in speech model is incomplete");
         }

         try {
            Files.move(installing, model, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(installing, model);
         }

         return model;
      } finally {
         deleteTree(installing);
      }
   }

   private static void extractModel(InputStream archive, Path destination) throws IOException {
      Path root = destination.toAbsolutePath().normalize();
      String prefix = MODEL_NAME + "/";
      long extracted = 0L;

      try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(archive))) {
         ZipEntry entry;
         while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName().replace('\\', '/');
            if (!name.startsWith(prefix)) {
               continue;
            }

            String relative = name.substring(prefix.length());
            if (relative.isEmpty()) {
               continue;
            }

            Path output = root.resolve(relative).normalize();
            if (!output.startsWith(root)) {
               throw new IOException("Unsafe model archive path");
            }

            if (entry.isDirectory()) {
               Files.createDirectories(output);
            } else {
               Files.createDirectories(output.getParent());

               try (OutputStream out = Files.newOutputStream(output)) {
                  byte[] buffer = new byte[65536];

                  int read;
                  while ((read = zip.read(buffer)) != -1) {
                     extracted += read;
                     if (extracted > MAX_EXTRACTED_BYTES) {
                        throw new IOException("Model archive is unexpectedly large");
                     }

                     out.write(buffer, 0, read);
                  }
               }
            }
         }
      }
   }

   private static boolean valid(Path model) {
      return Files.isRegularFile(model.resolve("am/final.mdl")) && Files.isRegularFile(model.resolve("conf/model.conf"));
   }

   private static void deleteTree(Path root) throws IOException {
      if (Files.exists(root)) {
         try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
               Files.deleteIfExists(path);
            }
         }
      }
   }

   @FunctionalInterface
   interface StatusSink {
      void set(String var1);
   }
}
