package net.garb.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.character.CharacterClass;
import net.garb.character.CharacterClassRegistry;
import net.garb.util.SkinLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class CreateCharacterScreen extends Screen {

    public record CreationResult(String characterName, String skinUsername, String classId) {
    }

    private final Screen parent;
    private final Consumer<CreationResult> onCreate;

    private TextFieldWidget characterNameField;
    private TextFieldWidget skinUsernameField;

    private enum SkinLoadState {IDLE, LOADING, SUCCESS, ERROR}

    private SkinLoadState skinLoadState = SkinLoadState.IDLE;
    private String skinErrorMessage = null;
    private Identifier loadedSkinTexture = null;
    private boolean loadedSkinSlim = false;
    private String lastSearchedSkin = "";
    private long lastSkinTypedTime = 0;
    private static final long DEBOUNCE_MS = 800;

    private List<CharacterClass> classes = new ArrayList<>();
    private int selectedClassIndex = 0;

    private static final int PANEL_PADDING = 16;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_MUTED = 0xFF666666;
    private static final int COLOR_PANEL_BG = 0xCC1A1A1A;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_HP = 0xFFFF5555;
    private static final int COLOR_SPEED = 0xFF55FFFF;
    private static final int COLOR_DMG = 0xFFFFAA55;
    private static final int COLOR_ARMOR = 0xFFAAAAFF;

    public CreateCharacterScreen(Screen parent, Consumer<CreationResult> onCreate) {
        super(Text.translatable("screen.garb.create_character"));
        this.parent = parent;
        this.onCreate = onCreate;
    }

    @Override
    protected void init() {
        super.init();
        this.classes = new ArrayList<>(CharacterClassRegistry.getAll());

        int cx = this.width / 2;
        int cy = this.height / 2;

        int fieldX = PANEL_PADDING;
        int fieldW = (this.width / 3) - PANEL_PADDING * 2;

        characterNameField = new TextFieldWidget(this.textRenderer, fieldX, cy - 60, fieldW, 20, Text.translatable("screen.garb.name"));
        characterNameField.setMaxLength(32);
        characterNameField.setPlaceholder(Text.translatable("screen.garb.character_name_placeholder"));
        characterNameField.setFocused(true);
        this.addSelectableChild(characterNameField);

        skinUsernameField = new TextFieldWidget(this.textRenderer, fieldX, cy - 20, fieldW, 20, Text.translatable("screen.garb.skin_username"));
        skinUsernameField.setMaxLength(32);
        skinUsernameField.setPlaceholder(Text.translatable("screen.garb.skin_username_placeholder"));
        skinUsernameField.setChangedListener(this::onSkinUsernameChanged);
        this.addSelectableChild(skinUsernameField);

        if (this.client != null && this.client.getSession() != null) {
            skinUsernameField.setText(this.client.getSession().getUsername());
        }

        int classPanelCx = cx;
        int arrowY = cy + 60;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> cycleClass(-1)).position(classPanelCx - 90, arrowY).size(20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> cycleClass(+1)).position(classPanelCx + 70, arrowY).size(20, 20).build());

        int btnY = this.height - PANEL_PADDING - 20;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.garb.confirm"), btn -> confirm()).position(cx - 102, btnY).size(100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> this.client.setScreen(parent)).position(cx + 2, btnY).size(100, 20).build());
    }

    @Override
    public void tick() {
        super.tick();

        String current = skinUsernameField.getText().trim();
        if (!current.isEmpty() && !current.equals(lastSearchedSkin) && skinLoadState != SkinLoadState.LOADING && System.currentTimeMillis() - lastSkinTypedTime > DEBOUNCE_MS) {
            triggerSkinLoad(current);
        }
    }

    private void onSkinUsernameChanged(String value) {
        lastSkinTypedTime = System.currentTimeMillis();
        if (!value.equals(lastSearchedSkin)) {
            skinLoadState = SkinLoadState.IDLE;
            loadedSkinTexture = null;
        }
    }

    private void triggerSkinLoad(String username) {
        lastSearchedSkin = username;
        skinLoadState = SkinLoadState.LOADING;
        loadedSkinTexture = null;
        skinErrorMessage = null;

        SkinLoader.loadForUsername(username, result -> {
            if (this.client == null){
                return;
            }
            this.client.execute(() -> {
                if (!username.equals(lastSearchedSkin)){
                    return;
                }
                if (result.success()) {
                    loadedSkinTexture = result.textureId();
                    loadedSkinSlim = result.slim();
                    skinLoadState = SkinLoadState.SUCCESS;
                } else {
                    skinErrorMessage = result.errorMessage();
                    skinLoadState = SkinLoadState.ERROR;
                }
            });
        });
    }

    private void cycleClass(int direction) {
        if (classes.isEmpty()) {
            return;
        }
        selectedClassIndex = Math.floorMod(selectedClassIndex + direction, classes.size());
    }

    private void confirm() {
        String charName = characterNameField.getText().trim();
        if (charName.isEmpty()) {
            characterNameField.setPlaceholder(Text.translatable("screen.garb.name_required"));
            return;
        }

        String skinName = skinUsernameField.getText().trim();
        String classId = (!classes.isEmpty()) ? classes.get(selectedClassIndex).getId().toString() : "";

        onCreate.accept(new CreationResult(charName, skinName, classId));
        this.client.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int panelW = this.width / 3;
        int cy = this.height / 2;

        renderLeftPanel(context, panelW, cy);

        renderMiddlePanel(context, panelW, cy);

        renderRightPanel(context, panelW, cy);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.create_character"), this.width / 2, PANEL_PADDING, COLOR_TITLE);
    }

    private void renderLeftPanel(DrawContext context, int panelW, int cy) {
        int x = PANEL_PADDING;
        int panelX = panelW + PANEL_PADDING;
        int panelX2 = panelW - PANEL_PADDING;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.character"), (panelW / 2), PANEL_PADDING + 24, COLOR_TITLE);

        context.fill(x - 4, PANEL_PADDING + 16, panelX2 + 4, this.height - PANEL_PADDING - 28, COLOR_PANEL_BG);
        context.drawBorder(x - 4, PANEL_PADDING + 16, panelX2 - x + 8, this.height - PANEL_PADDING * 2 - 44, COLOR_BORDER);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.name"), x, cy - 74, COLOR_LABEL);
        characterNameField.render(context, 0, 0, 0);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skin_username"), x, cy - 34, COLOR_LABEL);
        skinUsernameField.render(context, 0, 0, 0);

        int previewCx = x + (panelW - PANEL_PADDING * 2) / 2;
        int previewY = cy + 20;
        renderSkinPreview(context, previewCx, previewY);
    }

    private void renderSkinPreview(DrawContext context, int cx, int y) {
        switch (skinLoadState) {
            case IDLE -> context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skin_preview_hint"), cx, y, COLOR_MUTED);

            case LOADING -> {
                int dots = (int) ((System.currentTimeMillis() / 400) % 4);
                String label = "Loading" + ".".repeat(dots);
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), cx, y, COLOR_LABEL);
            }

            case ERROR -> context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(skinErrorMessage), cx, y, COLOR_ERROR);

            case SUCCESS -> {
                if (loadedSkinTexture != null) {
                    int scale = 6;
                    int playerX = cx - scale * 2;
                    drawPlayer(context, loadedSkinTexture, loadedSkinSlim, playerX, y, scale);
                }
            }
        }
    }

    private void renderMiddlePanel(DrawContext context, int panelW, int cy) {
        int panelX = panelW + PANEL_PADDING;
        int panelX2 = panelW * 2 - PANEL_PADDING;
        int panelCx = (panelX + panelX2) / 2;

        context.fill(panelX - 4, PANEL_PADDING + 16, panelX2 + 4, this.height - PANEL_PADDING - 28, COLOR_PANEL_BG);
        context.drawBorder(panelX - 4, PANEL_PADDING + 16, panelX2 - panelX + 8, this.height - PANEL_PADDING * 2 - 44, COLOR_BORDER);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.choose_class"), panelCx, PANEL_PADDING + 24, COLOR_TITLE);

        if (classes.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.no_classes_loaded"), panelCx, cy, COLOR_MUTED);
            return;
        }

        CharacterClass selected = classes.get(selectedClassIndex);

        int iconY = PANEL_PADDING + 40;
        int iconSize = 32;
        if (selected.getIcon() != null) {
            context.drawTexture(selected.getIcon(), panelCx - iconSize / 2, iconY, iconSize, iconSize, 0, 0, 16, 16, 16, 16);
        } else {
            context.drawBorder(panelCx - iconSize / 2, iconY, iconSize, iconSize, COLOR_BORDER);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("?"), panelCx, iconY + 12, COLOR_MUTED);
        }

        int textY = iconY + iconSize + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selected.getDisplayName()), panelCx, textY, COLOR_TITLE);
        textY += 14;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal((selectedClassIndex + 1) + " / " + classes.size()), panelCx, textY, COLOR_MUTED);
        textY += 14;

        context.fill(panelX + 8, textY, panelX2 - 8, textY + 1, COLOR_BORDER);
        textY += 6;

        if (selected.getDescription() != null && !selected.getDescription().isEmpty()) {
            for (String line : wrapText(selected.getDescription(), panelX2 - panelX - 16)) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), panelCx, textY, COLOR_LABEL);
                textY += 10;
            }
        }
    }

    private void renderRightPanel(DrawContext context, int panelW, int cy) {
        int panelX = panelW * 2 + PANEL_PADDING;
        int panelX2 = this.width - PANEL_PADDING;
        int panelCx = (panelX + panelX2) / 2;
        int textX = panelX + 8;

        context.fill(panelX - 4, PANEL_PADDING + 16, panelX2 + 4, this.height - PANEL_PADDING - 28, COLOR_PANEL_BG);
        context.drawBorder(panelX - 4, PANEL_PADDING + 16, panelX2 - panelX + 8, this.height - PANEL_PADDING * 2 - 44, COLOR_BORDER);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.class_stats"), panelCx, PANEL_PADDING + 24, COLOR_TITLE);

        if (classes.isEmpty()) {
            return;
        }
        CharacterClass selected = classes.get(selectedClassIndex);
        int textY = PANEL_PADDING + 40;
        int lineH = 14;

        renderStat(context, textX, textY, "Max Health", selected.getAttributes().getOrDefault("minecraft:max_health", 20.0), 20.0, COLOR_HP);
        textY += lineH;

        renderStat(context, textX, textY, "Attack Damage", selected.getAttributes().getOrDefault("minecraft:attack_damage", 2.0), 2.0, COLOR_DMG);
        textY += lineH;

        renderStat(context, textX, textY, "Armor", selected.getAttributes().getOrDefault("minecraft:armor", 0.0), 0.0, COLOR_ARMOR);
        textY += lineH;

        renderStat(context, textX, textY, "Speed", selected.getAttributes().getOrDefault("minecraft:movement_speed", 0.1), 0.1, COLOR_SPEED);
        textY += lineH + 4;

        context.fill(panelX + 8, textY, panelX2 - 8, textY + 1, COLOR_BORDER);
        textY += 8;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.starter_items"), textX, textY, COLOR_LABEL);
        textY += lineH;

        if (selected.getStartingItems().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("—"), textX, textY, COLOR_MUTED);
        } else {
            for (CharacterClass.StartItem item : selected.getStartingItems()) {
                String itemName = item.item().contains(":") ? item.item().split(":")[1].replace("_", " ") : item.item();

                itemName = Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1);
                context.drawTextWithShadow(this.textRenderer, Text.literal("• " + itemName + " x" + item.count()), textX, textY, COLOR_VALUE);
                textY += 10;
                if (textY > this.height - PANEL_PADDING - 40) {
                    break;
                }
            }
        }
    }

    private void renderStat(DrawContext context, int x, int y, String label, double value, double defaultValue, int color) {
        String valueStr = String.format("%.1f", value);

        String indicator = "";
        if (value > defaultValue) {
            indicator = " ▲";
        } else if (value < defaultValue) {
            indicator = " ▼";
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(label + ": ").withColor(COLOR_LABEL).append(Text.literal(valueStr + indicator).withColor(color)), x, y, COLOR_LABEL);
    }

    private void drawPlayer(DrawContext context, Identifier skin, boolean slim, int x, int y, int scale) {
        drawSkinPart(context, skin, x + scale, y, scale * 2, scale * 2, 8, 8, 8, 8);  // Kopf
        drawSkinPart(context, skin, x + scale, y + scale * 2, scale * 2, scale * 3, 20, 20, 8, 12); // Körper
        int armW = slim ? 3 : 4;
        drawSkinPart(context, skin, x, y + scale * 2, scale, scale * 3, 44, 20, armW, 12); // R-Arm
        drawSkinPart(context, skin, x + scale * 3, y + scale * 2, scale, scale * 3, 36, 52, armW, 12); // L-Arm
        drawSkinPart(context, skin, x + scale, y + scale * 5, scale, scale * 3, 4, 20, 4, 12); // R-Bein
        drawSkinPart(context, skin, x + scale * 2, y + scale * 5, scale, scale * 3, 20, 52, 4, 12); // L-Bein

        drawSkinPart(context, skin, x + scale - 1, y - 1, scale * 2 + 2, scale * 2 + 2, 40, 8, 8, 8);
    }

    private void drawSkinPart(DrawContext context, Identifier texture, int x, int y, int w, int h, int u, int v, int rW, int rH) {
        context.drawTexture(texture, x, y, w, h, u, v, rW, rH, 64, 64);
    }

    private List<String> wrapText(String text, int maxPixelWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (this.textRenderer.getWidth(test) > maxPixelWidth) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}