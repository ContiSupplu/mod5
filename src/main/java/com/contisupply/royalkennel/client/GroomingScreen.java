package com.contisupply.royalkennel.client;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.contisupply.royalkennel.DogStyle;
import com.contisupply.royalkennel.KennelAttachments;
import com.contisupply.royalkennel.KennelNet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariant;

/**
 * "The Royal Kennel" - a parchment-and-timber atelier drawn entirely with
 * fills and gradients (no textures, so it survives GUI pipeline changes).
 * The world stays visible behind it: the dog sits center-right while the
 * cinematic camera slowly orbits it.
 */
public class GroomingScreen extends Screen {

    // ---- Palette -----------------------------------------------------------
    private static final int TIMBER_DARK = 0xFF2A1C10;
    private static final int TIMBER = 0xFF46311E;
    private static final int TIMBER_LIGHT = 0xFF5A4028;
    private static final int GOLD = 0xFFC9A227;
    private static final int GOLD_BRIGHT = 0xFFE8C765;
    private static final int PARCHMENT_TOP = 0xF7EADCB4;
    private static final int PARCHMENT_BOTTOM = 0xF7D2B183;
    private static final int INK = 0xFF4A3319;
    private static final int INK_FADED = 0xFF7A5A33;
    private static final int STRIP_BG = 0xC029190E;
    private static final int CREAM = 0xFFF2E3BF;

    private static final int PANEL_X = 12;
    private static final int PANEL_W = 180;
    private static final int PANEL_H = 252;

    // ---- Catalogue ---------------------------------------------------------
    private static final String[] HAT_NAMES = {"Bare", "Crown of the Realm", "Mage's Cap", "Knight's Helm"};
    private static final String[] BACK_NAMES = {"Unburdened", "Adventurer's Pack", "Rescue Keg", "Royal Cape"};
    private static final String[] DYE_NAMES = {
            "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
            "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"};
    // Vanilla dye colors, indexed by DyeColor id.
    private static final int[] DYE_SWATCHES = {
            0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF474F52,
            0xFF9D9D97, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA, 0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21};

    private static final Map<String, String[]> BREED_LORE = Map.ofEntries(
            Map.entry("pale", new String[]{"Pale", "Hound of the Vale"}),
            Map.entry("ashen", new String[]{"Ashen", "Ghost of the Moor"}),
            Map.entry("black", new String[]{"Black", "Warden of Night"}),
            Map.entry("chestnut", new String[]{"Chestnut", "Tavern Companion"}),
            Map.entry("rusty", new String[]{"Rusty", "Errant of the Jungle"}),
            Map.entry("snowy", new String[]{"Snowy", "Winter Sentinel"}),
            Map.entry("spotted", new String[]{"Spotted", "Scout of the Savanna"}),
            Map.entry("striped", new String[]{"Striped", "Rogue of the Badlands"}),
            Map.entry("woods", new String[]{"Woods", "Warden of the Forest"}));

    private static final String[] NOBLE_NAMES = {
            "Sir Barksalot", "Duke Fluffington", "Lady Snoutwick", "Baron von Woof",
            "Squire Biscuit", "Dame Wagatha", "Lord Scruffington", "Ser Fetchalot",
            "Countess Nibbles", "Friar Chewbert", "Knight Nosewise", "Earl Greymuzzle",
            "Princess Pawline", "Bishop Barkely", "Wolfgang the Bold", "Maximus Muddypaw",
            "Reginald Rufflebottom", "Gwendolyn Goodgirl", "Percival the Loyal",
            "Magnus Bonechewer", "Isolde Itchyear", "Tristan Tailchaser"};

    private static final Random RANDOM = new Random();

    // ---- State -------------------------------------------------------------
    private final Wolf wolf;
    private List<Holder.Reference<WolfVariant>> variants;
    private int variantIndex;
    private int hat;
    private int back;
    private int collar;
    private String pendingName = "";
    private boolean stateLoaded;
    private boolean closeSent;

    private EditBox nameBox;

    public GroomingScreen(Wolf wolf) {
        super(Component.literal("The Royal Kennel"));
        this.wolf = wolf;
    }

    @Override
    protected void init() {
        if (!stateLoaded) {
            loadStateFromWolf();
            stateLoaded = true;
        }

        int px = PANEL_X;
        int py = panelY();

        addCycler(px, py + 42, () -> cycleVariant(-1), () -> cycleVariant(1));
        addCycler(px, py + 76, () -> cycleHat(-1), () -> cycleHat(1));
        addCycler(px, py + 110, () -> cycleBack(-1), () -> cycleBack(1));
        addCycler(px, py + 144, () -> cycleCollar(-1), () -> cycleCollar(1));

        nameBox = new EditBox(this.font, px + 14, py + 193, PANEL_W - 28, 12, Component.literal("Name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(24);
        nameBox.setTextColor(CREAM);
        nameBox.setValue(pendingName);
        nameBox.setResponder(text -> {
            pendingName = text;
            sendUpdate();
        });
        addRenderableWidget(nameBox);

        addRenderableWidget(new MedievalButton(px + 10, py + 209, PANEL_W - 20, 16,
                "✦ Bestow a Noble Name", () -> {
            String name = NOBLE_NAMES[RANDOM.nextInt(NOBLE_NAMES.length)];
            nameBox.setValue(name); // responder sends the update
        }));

        addRenderableWidget(new MedievalButton(px + 10, py + PANEL_H - 24, PANEL_W - 20, 18,
                "♦ Seal the Decree ♦", () -> {
            sendCloseOnce(true);
            onClose();
        }));
    }

    private int panelY() {
        return Math.max(9, (this.height - PANEL_H) / 2);
    }

    private void addCycler(int px, int rowY, Runnable left, Runnable right) {
        addRenderableWidget(new ArrowButton(px + 8, rowY + 11, "<", left));
        addRenderableWidget(new ArrowButton(px + PANEL_W - 22, rowY + 11, ">", right));
    }

    private void loadStateFromWolf() {
        var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT);
        variants = registry.listElements().toList();
        variantIndex = 0;
        var currentKey = wolf.getVariant().unwrapKey().orElse(null);
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).key() == currentKey) {
                variantIndex = i;
                break;
            }
        }
        DogStyle style = wolf.getAttached(KennelAttachments.DOG_STYLE);
        if (style == null) {
            style = DogStyle.PLAIN;
        }
        hat = style.hat();
        back = style.back();
        collar = wolf.getCollarColor().getId();
        pendingName = wolf.getCustomName() == null ? "" : wolf.getCustomName().getString();
    }

    // ---- Cycling + networking ----------------------------------------------

    private void cycleVariant(int dir) {
        if (!variants.isEmpty()) {
            variantIndex = Math.floorMod(variantIndex + dir, variants.size());
            sendUpdate();
        }
    }

    private void cycleHat(int dir) {
        hat = Math.floorMod(hat + dir, DogStyle.HAT_COUNT);
        sendUpdate();
    }

    private void cycleBack(int dir) {
        back = Math.floorMod(back + dir, DogStyle.BACK_COUNT);
        sendUpdate();
    }

    private void cycleCollar(int dir) {
        collar = Math.floorMod(collar + dir, 16);
        sendUpdate();
    }

    private String variantId() {
        if (variants == null || variants.isEmpty()) {
            return "minecraft:pale";
        }
        return variants.get(variantIndex).key().location().toString();
    }

    private void sendUpdate() {
        ClientPlayNetworking.send(new KennelNet.UpdateDogPayload(
                wolf.getId(), variantId(), hat, back, collar, pendingName));
    }

    private void sendCloseOnce(boolean sealed) {
        if (!closeSent) {
            closeSent = true;
            ClientPlayNetworking.send(new KennelNet.CloseGroomingPayload(sealed));
        }
    }

    // ---- Screen behaviour ---------------------------------------------------

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No blur, no darkening: the star of the show is the dog behind the UI.
    }

    @Override
    public void tick() {
        super.tick();
        if (wolf.isRemoved() || this.minecraft.player == null || this.minecraft.player.distanceTo(wolf) > 14.0f) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        sendCloseOnce(false);
        super.onClose();
    }

    @Override
    public void removed() {
        ClientGroomingSession.end();
        super.removed();
    }

    // ---- Painting -----------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        paintScene(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void paintScene(GuiGraphics g) {
        // Cinematic letterbox.
        g.fillGradient(0, 0, this.width, 26, 0xA8000000, 0x00000000);
        g.fillGradient(0, this.height - 26, this.width, this.height, 0x00000000, 0xA8000000);

        int px = PANEL_X;
        int py = panelY();
        paintPanel(g, px, py, PANEL_W, PANEL_H);

        // Title.
        centerText(g, "✦ The Royal Kennel ✦", px + PANEL_W / 2, py + 9, GOLD_BRIGHT, true);
        centerText(g, "~ Charter of thy Noble Hound ~", px + PANEL_W / 2, py + 21, INK_FADED, false);
        paintDivider(g, px + 12, py + 33, PANEL_W - 24);

        String[] lore = breedLore();
        paintCyclerRow(g, px, py + 42, "Bloodline", lore[0], lore[1]);
        paintCyclerRow(g, px, py + 76, "Headwear", HAT_NAMES[hat], null);
        paintCyclerRow(g, px, py + 110, "Back Gear", BACK_NAMES[back], null);
        paintCollarRow(g, px, py + 144);

        // Name section.
        g.drawString(this.font, "Title & Name", px + 10, py + 180, INK, false);
        paintStrip(g, px + 10, py + 189, PANEL_W - 20, 16);
    }

    private String[] breedLore() {
        if (variants == null || variants.isEmpty()) {
            return new String[]{"Unknown", "Hound of Mystery"};
        }
        String path = variants.get(variantIndex).key().location().getPath();
        String[] lore = BREED_LORE.get(path);
        if (lore != null) {
            return lore;
        }
        String pretty = path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1).replace('_', ' ');
        return new String[]{pretty, "Hound of Mystery"};
    }

    private void paintCyclerRow(GuiGraphics g, int px, int y, String label, String value, String epithet) {
        g.drawString(this.font, label, px + 10, y, INK, false);
        if (epithet != null) {
            int w = this.font.width(epithet);
            g.drawString(this.font, epithet, px + PANEL_W - 10 - w, y, INK_FADED, false);
        }
        paintStrip(g, px + 26, y + 11, PANEL_W - 52, 16);
        centerText(g, value, px + PANEL_W / 2, y + 15, CREAM, true);
    }

    private void paintCollarRow(GuiGraphics g, int px, int y) {
        g.drawString(this.font, "Collar Hue", px + 10, y, INK, false);
        paintStrip(g, px + 26, y + 11, PANEL_W - 52, 16);
        int swatch = DYE_SWATCHES[collar];
        String name = DYE_NAMES[collar];
        int textW = this.font.width(name);
        int total = textW + 14;
        int startX = px + PANEL_W / 2 - total / 2;
        int sy = y + 14;
        g.fill(startX - 1, sy - 1, startX + 11, sy + 11, 0xFF1A120A);
        g.fill(startX, sy, startX + 10, sy + 10, swatch);
        g.drawString(this.font, name, startX + 14, y + 15, CREAM, true);
    }

    private void paintPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x55000000);               // drop shadow
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, TIMBER_DARK);              // timber frame
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF8A6A24);               // gold pinstripe
        g.fillGradient(x, y, x + w, y + h, PARCHMENT_TOP, PARCHMENT_BOTTOM);  // parchment
        outline(g, x + 4, y + 4, w - 8, h - 8, 0x40593F23);                   // faded ink rule

        // Gold corner accents.
        for (int[] c : new int[][]{{x + 2, y + 2}, {x + w - 5, y + 2}, {x + 2, y + h - 5}, {x + w - 5, y + h - 5}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, GOLD);
        }
    }

    private void paintStrip(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, STRIP_BG);
        outline(g, x, y, w, h, 0x66C9A227);
    }

    private void paintDivider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, 0x66593F23);
        g.fill(x + w / 2 - 2, y - 1, x + w / 2 + 2, y + 2, GOLD);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void centerText(GuiGraphics g, String text, int cx, int y, int color, boolean shadow) {
        g.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, shadow);
    }

    // ---- Widgets ------------------------------------------------------------

    private class ArrowButton extends AbstractButton {
        private final Runnable action;

        ArrowButton(int x, int y, String glyph, Runnable action) {
            super(x, y, 14, 16, Component.literal(glyph));
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x, y, x + w, y + h, TIMBER_DARK);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, isHoveredOrFocused() ? TIMBER_LIGHT : TIMBER);
            centerText(g, getMessage().getString(), x + w / 2, y + (h - 8) / 2,
                    isHoveredOrFocused() ? GOLD_BRIGHT : GOLD, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class MedievalButton extends AbstractButton {
        private final Runnable action;

        MedievalButton(int x, int y, int w, int h, String label, Runnable action) {
            super(x, y, w, h, Component.literal(label));
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x, y, x + w, y + h, isHoveredOrFocused() ? GOLD : TIMBER_DARK);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF6B4A2B, 0xFF4A3018);
            centerText(g, getMessage().getString(), x + w / 2, y + (h - 8) / 2,
                    isHoveredOrFocused() ? 0xFFFFEFC2 : GOLD_BRIGHT, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
