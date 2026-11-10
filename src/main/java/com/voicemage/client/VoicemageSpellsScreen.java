package com.voicemage.client;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.Spell;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoicemageSpellsScreen extends Screen {
   private static final int SPELLS_PER_PAGE = 3;

   public enum Category {
      ALL("All"),
      OFFENSE("Offense"),
      DEFENSE("Defense"),
      UTILITY("Utility"),
      CUSTOM("Custom");

      final String label;

      Category(String label) {
         this.label = label;
      }
   }

   public static class SpellDisplayEntry {
      public final Spell spell;
      public final VoicemageConfig.CustomSpellEntry custom;
      public final String name;
      public final String phrase;
      public final String description;
      public final Category category;

      public SpellDisplayEntry(Spell spell, String description, Category category) {
         this.spell = spell;
         this.custom = null;
         this.name = spell.displayName();
         this.phrase = spell.phrase();
         this.description = description;
         this.category = category;
      }

      public SpellDisplayEntry(VoicemageConfig.CustomSpellEntry custom) {
         this.spell = null;
         this.custom = custom;
         this.name = custom.phrase;
         this.phrase = custom.phrase;
         if (custom.javaCode != null && !custom.javaCode.isBlank()) {
            this.description = "Custom Java spell | " + custom.particleName;
         } else {
            this.description = "Custom particle: " + custom.particleName + " | Power: " + custom.damageOrEffect;
         }
         this.category = Category.CUSTOM;
      }

      public boolean isCustom() {
         return this.custom != null;
      }

      public int manaCost() {
         if (this.custom != null) {
            return (int) Math.ceil(this.custom.manaCost);
         }
         return VoicemageConfig.INSTANCE.manaCost(this.spell);
      }
   }

   private Category selectedCategory = Category.ALL;
   private String searchQuery = "";
   private int page = 0;
   private EditBox searchBox;

   public VoicemageSpellsScreen() {
      super(Component.literal("VoiceMage Grimoire"));
   }

   private List<SpellDisplayEntry> buildAllEntries() {
      List<SpellDisplayEntry> list = new ArrayList<>();
      list.add(new SpellDisplayEntry(Spell.FIREBALL, "Launches an explosive fire charge detonating upon impact.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.PITFALL, "Opens a sudden collapsed pit under your targeted area.", Category.UTILITY));
      list.add(new SpellDisplayEntry(Spell.HEAL, "Channels radiant restorative divine magic to heal your wounds.", Category.DEFENSE));
      list.add(new SpellDisplayEntry(Spell.LIGHTNING, "Strikes targeted crosshair location with a lethal lightning bolt.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.EARTH_WALL, "Raises a temporary defensive stone barrier against enemy attacks.", Category.DEFENSE));
      list.add(new SpellDisplayEntry(Spell.BLINK, "Instantly warps you forward short-range in gaze direction.", Category.UTILITY));
      list.add(new SpellDisplayEntry(Spell.MAGIC_MISSILE, "Fires homing missiles of concentrated arcane force.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.METEOR, "Calls down a blazing celestial meteorite from the stratosphere.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.WIND_BLAST, "Unleashes a ferocious gale knocking back enemies with force.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.FREEZE, "Encases targeted mobs or foes into solid glacial ice blocks.", Category.DEFENSE));
      list.add(new SpellDisplayEntry(Spell.CLONE, "Creates decoy duplicates that mimic you and distract monsters.", Category.DEFENSE));
      list.add(new SpellDisplayEntry(Spell.POLYMORPH, "Transfigures the targeted creature into a harmless sheep.", Category.UTILITY));
      list.add(new SpellDisplayEntry(Spell.SHRINK, "Shrinks targeted entity to miniature size, reducing reach & hitbox.", Category.UTILITY));
      list.add(new SpellDisplayEntry(Spell.EXPLOSION, "Cataclysmic Megumin-style blast of sheer obliteration.", Category.OFFENSE));
      list.add(new SpellDisplayEntry(Spell.FLY, "Grants creative flight for 10s with wind swirls, followed by a 5s cooldown.", Category.UTILITY));
      list.add(new SpellDisplayEntry(Spell.GET_DOOMED, "Halves target HP (stops at 0.5 hearts) using dark warden sonic magic.", Category.OFFENSE));

      for (VoicemageConfig.CustomSpellEntry custom : VoicemageConfig.INSTANCE.customSpells) {
         list.add(new SpellDisplayEntry(custom));
      }
      return list;
   }

   private List<SpellDisplayEntry> filteredEntries() {
      List<SpellDisplayEntry> all = this.buildAllEntries();
      List<SpellDisplayEntry> filtered = new ArrayList<>();
      String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);

      for (SpellDisplayEntry entry : all) {
         if (this.selectedCategory != Category.ALL && entry.category != this.selectedCategory) {
            continue;
         }
         if (!query.isEmpty()) {
            boolean matchName = entry.name.toLowerCase(Locale.ROOT).contains(query);
            boolean matchPhrase = entry.phrase.toLowerCase(Locale.ROOT).contains(query);
            boolean matchDesc = entry.description.toLowerCase(Locale.ROOT).contains(query);
            if (!matchName && !matchPhrase && !matchDesc) {
               continue;
            }
         }
         filtered.add(entry);
      }
      return filtered;
   }

   @Override
   protected void init() {
      super.init();

      int boxW = 390;
      int boxH = 226;
      int boxX = (this.width - boxW) / 2;
      int boxY = (this.height - boxH) / 2;

      // Filter category buttons
      int filterY = boxY + 28;
      Category[] categories = Category.values();
      int catW = 46;
      for (int i = 0; i < categories.length; i++) {
         final Category cat = categories[i];
         boolean active = (this.selectedCategory == cat);
         this.addRenderableWidget(
            Button.builder(Component.literal(active ? "§6" + cat.label : cat.label), btn -> {
               this.selectedCategory = cat;
               this.page = 0;
               this.refresh();
            }).bounds(boxX + 10 + i * (catW + 2), filterY, catW, 16).build()
         );
      }

      // Search Box
      this.searchBox = new EditBox(this.font, boxX + boxW - 130, filterY, 120, 16, Component.literal("Search"));
      this.searchBox.setValue(this.searchQuery);
      this.searchBox.setResponder(text -> {
         this.searchQuery = text;
         this.page = 0;
         this.refresh();
      });
      this.addRenderableWidget(this.searchBox);

      // Bottom Bar Navigation
      int bottomY = boxY + boxH - 24;
      this.addRenderableWidget(
         Button.builder(Component.literal("◀ Prev"), btn -> {
            if (this.page > 0) {
               this.page--;
               this.refresh();
            }
         }).bounds(boxX + 10, bottomY, 56, 18).build()
      );

      this.addRenderableWidget(
         Button.builder(Component.literal("Next ▶"), btn -> {
            int maxP = Math.max(0, (this.filteredEntries().size() - 1) / SPELLS_PER_PAGE);
            if (this.page < maxP) {
               this.page++;
               this.refresh();
            }
         }).bounds(boxX + 70, bottomY, 56, 18).build()
      );

      this.addRenderableWidget(
         Button.builder(Component.literal("Close"), btn -> this.onClose())
            .bounds(boxX + boxW - 75, bottomY, 65, 18)
            .build()
      );
   }

   private void refresh() {
      this.clearWidgets();
      this.init();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, 0xD008090E);

      int boxW = 390;
      int boxH = 226;
      int boxX = (this.width - boxW) / 2;
      int boxY = (this.height - boxH) / 2;

      // Outer frame & shadow
      graphics.fill(boxX + 3, boxY + 3, boxX + boxW + 3, boxY + boxH + 3, 0x80000000);
      graphics.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF5C3D88);
      graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF191A24);
      graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFA12131A);

      // Header Banner
      graphics.fill(boxX, boxY, boxX + boxW, boxY + 26, 0xFF1B1D28);
      graphics.fill(boxX, boxY + 26, boxX + boxW, boxY + 27, 0xFFD4AF37);
      graphics.centeredText(this.font, "§6§l✦ VOICEMAGE GRIMOIRE ✦§r §7— Spell Guidebook§r", this.width / 2, boxY + 8, 0xFFFFAA00);

      // Spell Cards Rendering
      List<SpellDisplayEntry> entries = this.filteredEntries();
      int totalEntries = entries.size();
      int maxPage = Math.max(0, (totalEntries - 1) / SPELLS_PER_PAGE);
      int startIndex = this.page * SPELLS_PER_PAGE;
      int cardStartY = boxY + 48;
      int cardH = 43;
      int cardW = boxW - 20;

      if (entries.isEmpty()) {
         graphics.centeredText(this.font, "§7No spells found matching your criteria.§r", this.width / 2, boxY + 100, 0xFFAAAAAA);
      } else {
         for (int i = 0; i < SPELLS_PER_PAGE; i++) {
            int idx = startIndex + i;
            if (idx < totalEntries) {
               SpellDisplayEntry entry = entries.get(idx);
               int cardY = cardStartY + i * (cardH + 2);
               int cardX = boxX + 10;

               // Card backdrop
               graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x60000000);

               // Left accent border strip
               int stripColor = switch (entry.category) {
                  case OFFENSE -> 0xFFFF4444;
                  case DEFENSE -> 0xFF44AAFF;
                  case UTILITY -> 0xFFFFAA00;
                  case CUSTOM -> 0xFFAA44FF;
                  default -> 0xFFDDDDDD;
               };
               graphics.fill(cardX, cardY, cardX + 3, cardY + cardH, stripColor);

               // Top Line: Spell Name & Category Badge
               String title = "§6§l" + entry.name + "§r";
               graphics.text(this.font, title, cardX + 8, cardY + 4, 0xFFFFAA00, true);

               String badge = "§8[§7" + entry.category.label.toUpperCase(Locale.ROOT) + "§8]§r";
               graphics.text(this.font, badge, cardX + 115, cardY + 4, 0xFFAAAAAA, false);

               // Top Right: Mana Cost (configured by OP)
               String manaStr = "§bMana Cost: §d§l" + entry.manaCost() + "§r";
               int manaW = this.font.width(manaStr);
               graphics.text(this.font, manaStr, cardX + cardW - manaW - 8, cardY + 4, 0xFF88CCFF, true);

               // Middle Line: Voice Incantation
               String phraseStr = "§eIncantation: §f\"" + entry.phrase + "\"§r";
               if (phraseStr.length() > 68) {
                  phraseStr = phraseStr.substring(0, 65) + "...\"";
               }
               graphics.text(this.font, phraseStr, cardX + 8, cardY + 17, 0xFFCCCCCC, false);

               // Bottom Line: Description
               String desc = "§7" + entry.description + "§r";
               if (desc.length() > 72) {
                  desc = desc.substring(0, 69) + "...";
               }
               graphics.text(this.font, desc, cardX + 8, cardY + 29, 0xFF888888, false);
            }
         }
      }

      // Bottom bar separator and feedback
      int bottomSepY = boxY + boxH - 28;
      graphics.fill(boxX, bottomSepY, boxX + boxW, bottomSepY + 1, 0xFF2A2D3D);

      // Page counter
      String pageCounter = "§7Page §f" + (this.page + 1) + " §7of §f" + (maxPage + 1) + " §7(" + totalEntries + " Spells)§r";
      graphics.centeredText(this.font, pageCounter, this.width / 2, bottomSepY + 6, 0xFFCCCCCC);

      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
   }
}
