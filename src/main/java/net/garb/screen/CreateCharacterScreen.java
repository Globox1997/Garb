package net.garb.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.garb.GarbMain;
import net.garb.character.CharacterClass;
import net.garb.character.CharacterClassRegistry;
import net.garb.network.packet.C2SCheckNamePacket;
import net.garb.util.SkinLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class CreateCharacterScreen extends Screen {

    public record CreationResult(String characterName, String skinUsername, String classId) {
    }

    private record AttributeIconInfo(Identifier icon, Text label, double value, double defaultValue) {
    }

    private final Screen parent;
    private final Consumer<CreationResult> onCreate;

    private TextFieldWidget characterNameField;
    private TextFieldWidget skinUsernameField;

    private enum SkinLoadState {IDLE, LOADING, SUCCESS, ERROR}

    private enum NameCheckState {IDLE, CHECKING, AVAILABLE, TAKEN}

    private SkinLoadState skinLoadState = SkinLoadState.IDLE;
    private String skinErrorMessage = null;
    private Identifier loadedSkinTexture = null;
    private boolean loadedSkinSlim = false;
    private String lastSearchedSkin = "";
    private long lastSkinTypedTime = 0;
    private static final long DEBOUNCE_MS = 800;

    private NameCheckState nameCheckState = NameCheckState.IDLE;
    private String lastCheckedName = "";
    private long lastNameTypedTime = 0;

    private List<CharacterClass> classes = new ArrayList<>();
    private int selectedClassIndex = 0;

    private static final int PANEL_PADDING = 16;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFFFFFFF;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_MUTED = 0xFF666666;
    private static final int COLOR_BORDER = 0xFF555555;

    private static final int ICON_SIZE = 9;
    private static final int ICON_RENDER_SIZE = 9;
    private static final int ICON_GAP = 4;

    private static final Identifier CLASS_BACKGROUND_TEXTURE = GarbMain.identifierOf("textures/gui/class_background.png");
    private static final int CLASS_BG_WIDTH = 148;
    private static final int CLASS_BG_HEIGHT = 195;
    private static final int CLASS_ICON_SIZE = 32;
    private static final int CLASS_ICON_FRAME_SIZE = 42;
    private static final int PANEL_EDGE = 4;
    private static final int CLASS_ARROW_SIZE = 20;
    private static final int CLASS_ARROW_GAP = 2;
    private static final int PANEL_GAP = 6;
    private static final int PANEL_INNER_PADDING = 8;
    private static final int TITLE_AREA = 32;
    private static final int BOTTOM_AREA = PANEL_PADDING + 20 + 8;

    private int panelY;
    private int panelY2;
    private int classBgX;
    private int leftPanelX1;
    private int leftPanelX2;
    private int rightPanelX1;
    private int rightPanelX2;
    private static final int COLOR_CLASS_TEXT = 0xFF404040;
    private static final int COLOR_CLASS_MUTED = 0xFF606060;

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
        updateLayout();

        int fieldX = leftPanelX1 + PANEL_INNER_PADDING;
        int fieldW = leftPanelX2 - leftPanelX1 - PANEL_INNER_PADDING * 2;

        characterNameField = new TextFieldWidget(this.textRenderer, fieldX, panelY + 34, fieldW, 20, Text.translatable("screen.garb.name"));
        characterNameField.setMaxLength(32);
        characterNameField.setPlaceholder(Text.translatable("screen.garb.character_name_placeholder"));
        characterNameField.setChangedListener(this::onCharacterNameChanged);
        characterNameField.setFocused(true);
        this.addSelectableChild(characterNameField);

        skinUsernameField = new TextFieldWidget(this.textRenderer, fieldX, panelY + 70, fieldW, 20, Text.translatable("screen.garb.skin_username"));
        skinUsernameField.setMaxLength(32);
        skinUsernameField.setPlaceholder(Text.translatable("screen.garb.skin_username_placeholder"));
        skinUsernameField.setChangedListener(this::onSkinUsernameChanged);
        this.addSelectableChild(skinUsernameField);

        if (this.client != null && this.client.getSession() != null) {
            skinUsernameField.setText(this.client.getSession().getUsername());
        }

        int arrowY = panelY + (CLASS_BG_HEIGHT - CLASS_ARROW_SIZE) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> cycleClass(-1)).position(classBgX - CLASS_ARROW_GAP - CLASS_ARROW_SIZE, arrowY).size(CLASS_ARROW_SIZE, CLASS_ARROW_SIZE).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> cycleClass(+1)).position(classBgX + CLASS_BG_WIDTH + CLASS_ARROW_GAP, arrowY).size(CLASS_ARROW_SIZE, CLASS_ARROW_SIZE).build());

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

        String currentName = characterNameField.getText().trim();
        if (!currentName.isEmpty() && !currentName.equals(lastCheckedName) && nameCheckState != NameCheckState.CHECKING && System.currentTimeMillis() - lastNameTypedTime > DEBOUNCE_MS) {
            triggerNameCheck(currentName);
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
            if (this.client == null) {
                return;
            }
            this.client.execute(() -> {
                if (!username.equals(lastSearchedSkin)) {
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
        if (nameCheckState == NameCheckState.TAKEN && charName.equals(lastCheckedName)) {
            return;
        }

        String skinName = skinUsernameField.getText().trim();
        String classId = (!classes.isEmpty()) ? classes.get(selectedClassIndex).getId().toString() : "";

        onCreate.accept(new CreationResult(charName, skinName, classId));
        this.client.setScreen(null);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
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

        renderLeftPanel(context);

        renderMiddlePanel(context);

        renderRightPanel(context, mouseX, mouseY);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.create_character"), this.width / 2, PANEL_PADDING, COLOR_TITLE);
    }

    private void updateLayout() {
        int available = this.height - TITLE_AREA - BOTTOM_AREA;
        int panelHeight = Math.min(CLASS_BG_HEIGHT, available);
        panelY = TITLE_AREA + Math.max(0, (available - panelHeight) / 2);
        panelY2 = panelY + panelHeight;

        classBgX = this.width / 2 - CLASS_BG_WIDTH / 2;

        leftPanelX2 = classBgX - CLASS_ARROW_GAP - CLASS_ARROW_SIZE - PANEL_GAP;
        leftPanelX1 = Math.max(PANEL_INNER_PADDING, leftPanelX2 - CLASS_BG_WIDTH);

        rightPanelX1 = classBgX + CLASS_BG_WIDTH + CLASS_ARROW_GAP + CLASS_ARROW_SIZE + PANEL_GAP;
        rightPanelX2 = Math.min(this.width - PANEL_INNER_PADDING, rightPanelX1 + CLASS_BG_WIDTH);
    }

    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        int w = Math.min(width, CLASS_BG_WIDTH);
        int h = Math.min(height, CLASS_BG_HEIGHT);
        int edge = PANEL_EDGE;
        context.drawTexture(CLASS_BACKGROUND_TEXTURE, x, y, 0, 0, w - edge, h - edge);
        context.drawTexture(CLASS_BACKGROUND_TEXTURE, x + w - edge, y, CLASS_BG_WIDTH - edge, 0, edge, h - edge);
        context.drawTexture(CLASS_BACKGROUND_TEXTURE, x, y + h - edge, 0, CLASS_BG_HEIGHT - edge, w - edge, edge);
        context.drawTexture(CLASS_BACKGROUND_TEXTURE, x + w - edge, y + h - edge, CLASS_BG_WIDTH - edge, CLASS_BG_HEIGHT - edge, edge, edge);
    }

    private void renderLeftPanel(DrawContext context) {
        int panelW = leftPanelX2 - leftPanelX1;
        int panelCx = leftPanelX1 + panelW / 2;
        int x = leftPanelX1 + PANEL_INNER_PADDING;

        drawPanel(context, leftPanelX1, panelY, panelW, panelY2 - panelY);

        context.drawTexture(CLASS_BACKGROUND_TEXTURE, panelCx-25, panelY+111, 148, 55, 50, 62);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.character"), panelCx, panelY + 8, COLOR_TITLE);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.name"), x, panelY + 24, COLOR_LABEL);
        characterNameField.render(context, 0, 0, 0);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skin_username"), x, panelY + 60, COLOR_LABEL);
        skinUsernameField.render(context, 0, 0, 0);

        renderNameCheckStatus(context, panelCx, panelY + 24);

        renderSkinPreview(context, panelCx, panelY + 118);
    }

    private void renderSkinPreview(DrawContext context, int cx, int y) {
        switch (skinLoadState) {
            case IDLE -> context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skin_preview_hint"), cx, y-22, COLOR_MUTED);

            case LOADING -> context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skin_loading"), cx, y-22, COLOR_LABEL);

            case ERROR -> context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(skinErrorMessage), cx, y-22, COLOR_ERROR);

            case SUCCESS -> {
                if (loadedSkinTexture != null) {
                    int scale = 6;
                    int playerX = cx - scale * 2;
                    drawPlayer(context, loadedSkinTexture, loadedSkinSlim, playerX, y, scale);
                }
            }
        }
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(this.textRenderer, text, centerX - this.textRenderer.getWidth(text) / 2, y, color, false);
    }

    private void renderMiddlePanel(DrawContext context) {
        int panelCx = classBgX + CLASS_BG_WIDTH / 2;
        int bgY = panelY;

        drawPanel(context, classBgX, bgY, CLASS_BG_WIDTH, panelY2 - panelY);

        context.drawTexture(CLASS_BACKGROUND_TEXTURE, panelCx - CLASS_ICON_FRAME_SIZE / 2, bgY + 19, CLASS_BG_WIDTH, 0, CLASS_ICON_FRAME_SIZE, CLASS_ICON_FRAME_SIZE);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.choose_class"), panelCx, bgY + 8, COLOR_TITLE);

        if (classes.isEmpty()) {
            drawCenteredText(context, Text.translatable("screen.garb.no_classes_loaded"), panelCx, (bgY + panelY2) / 2, COLOR_CLASS_MUTED);
            return;
        }

        CharacterClass selected = classes.get(selectedClassIndex);

        int iconY = bgY + 19 + (CLASS_ICON_FRAME_SIZE - CLASS_ICON_SIZE) / 2;
        if (selected.getIcon() != null) {
            context.drawTexture(selected.getIcon(), panelCx - CLASS_ICON_SIZE / 2, iconY, CLASS_ICON_SIZE, CLASS_ICON_SIZE, 0, 0, 16, 16, 16, 16);
        } else {
            drawCenteredText(context, Text.literal("?"), panelCx, iconY + 12, COLOR_CLASS_MUTED);
        }

        int textY = bgY + 19 + CLASS_ICON_FRAME_SIZE + 6;
        drawCenteredText(context, Text.literal(selected.getDisplayName()), panelCx, textY, COLOR_CLASS_TEXT);
        textY += 12;

        drawCenteredText(context, Text.literal((selectedClassIndex + 1) + " / " + classes.size()), panelCx, textY, COLOR_CLASS_MUTED);
        textY += 12;

        context.fill(classBgX + 8, textY, classBgX + CLASS_BG_WIDTH - 8, textY + 1, COLOR_BORDER);
        textY += 6;

        if (selected.getDescription() != null && !selected.getDescription().isEmpty()) {
            int maxY = panelY2 - 8 - this.textRenderer.fontHeight;
            for (String line : wrapText(selected.getDescription(), CLASS_BG_WIDTH - 16)) {
                if (textY > maxY) {
                    break;
                }
                drawCenteredText(context, Text.literal(line), panelCx, textY, COLOR_CLASS_MUTED);
                textY += 10;
            }
        }
    }

    private void renderRightPanel(DrawContext context, int mouseX, int mouseY) {
        int panelW = rightPanelX2 - rightPanelX1;
        int panelCx = rightPanelX1 + panelW / 2;
        int textX = rightPanelX1 + PANEL_INNER_PADDING;
        int maxY = panelY2 - 8 - this.textRenderer.fontHeight;

        drawPanel(context, rightPanelX1, panelY, panelW, panelY2 - panelY);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.class_stats"), panelCx, panelY + 8, COLOR_TITLE);

        if (classes.isEmpty()) {
            return;
        }
        CharacterClass selected = classes.get(selectedClassIndex);
        int textY = panelY + 24;

        List<AttributeIconInfo> attributeIcons = resolveChangedAttributeIcons(selected);
        textY = renderAttributeIcons(context, textX, textY, panelW - PANEL_INNER_PADDING * 2, attributeIcons, mouseX, mouseY);
        textY += 6;

        context.fill(rightPanelX1 + PANEL_INNER_PADDING, textY, rightPanelX2 - PANEL_INNER_PADDING, textY + 1, COLOR_BORDER);
        textY += 6;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.starter_items"), textX, textY, COLOR_LABEL);
        textY += 12;

        if (selected.getStartingItems().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("—"), textX, textY, COLOR_MUTED);
            textY += 10;
        } else {
            for (CharacterClass.StartItem item : selected.getStartingItems()) {
                if (textY > maxY) {
                    break;
                }
                String itemName = item.item().contains(":") ? item.item().split(":")[1].replace("_", " ") : item.item();
                itemName = Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1);
                context.drawTextWithShadow(this.textRenderer, Text.literal("• " + itemName + " x" + item.count()), textX, textY, COLOR_VALUE);
                textY += 10;
            }
        }
        textY += 4;
        renderSkillPresets(context, textX, textY, maxY, selected);
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

    private void onCharacterNameChanged(String value) {
        lastNameTypedTime = System.currentTimeMillis();
        if (!value.trim().equals(lastCheckedName)) {
            nameCheckState = NameCheckState.IDLE;
        }
    }

    private void triggerNameCheck(String name) {
        lastCheckedName = name;
        nameCheckState = NameCheckState.CHECKING;
        ClientConfigurationNetworking.send(new C2SCheckNamePacket(name));
    }

    public void onNameCheckResult(String name, boolean taken) {
        if (!name.equals(lastCheckedName)) {
            return;
        }
        nameCheckState = taken ? NameCheckState.TAKEN : NameCheckState.AVAILABLE;
    }

    private void renderNameCheckStatus(DrawContext context, int cx, int y) {
        switch (nameCheckState) {
            case CHECKING -> context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.checking_name"), cx, y, COLOR_MUTED);
            case TAKEN -> context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.name_taken"), cx, y, COLOR_ERROR);
            default -> {
            }
        }
    }

    private List<AttributeIconInfo> resolveChangedAttributeIcons(CharacterClass characterClass) {
        List<AttributeIconInfo> icons = new ArrayList<>();

        for (Map.Entry<String, Double> entry : characterClass.getAttributes().entrySet()) {
            Identifier attrId = Identifier.of(entry.getKey());
            Optional<RegistryEntry.Reference<EntityAttribute>> registryEntry = Registries.ATTRIBUTE.getEntry(attrId);
            if (registryEntry.isEmpty()) {
                continue;
            }

            EntityAttribute attribute = registryEntry.get().value();
            double defaultValue = attribute.getDefaultValue();
            double value = entry.getValue();
            if (Math.abs(value - defaultValue) < 0.0001) {
                continue;
            }

            Identifier icon = GarbMain.identifierOf("textures/attribute/" + attrId.getPath() + ".png");
            Text label = Text.translatable(attribute.getTranslationKey());
            icons.add(new AttributeIconInfo(icon, label, value, defaultValue));
        }

        return icons;
    }

    private void renderSkillPresets(DrawContext context, int textX, int textY, int maxY, CharacterClass selected) {
        if (selected.getSkillPresets().isEmpty()) {
            return;
        }

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.skill_presets"), textX, textY, COLOR_LABEL);
        textY += 12;

        for (Map.Entry<String, Integer> entry : selected.getSkillPresets().entrySet()) {
            if (textY > maxY) {
                break;
            }
            Text skillName = Text.translatable("skill.levelz." + entry.getKey());
            context.drawTextWithShadow(this.textRenderer, skillName.copy().append(" +" + entry.getValue()), textX, textY, COLOR_VALUE);
            textY += 10;
        }
    }

    private int renderAttributeIcons(DrawContext context, int startX, int startY, int maxWidth, List<AttributeIconInfo> icons, int mouseX, int mouseY) {
        if (icons.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("—"), startX, startY, COLOR_MUTED);
            return startY + 12;
        }

        int rowHeight = ICON_RENDER_SIZE;
        int x = startX;
        int y = startY;

        for (AttributeIconInfo info : icons) {
            String valueStr = String.format("%.1f", info.value());
            int textWidth = this.textRenderer.getWidth(valueStr);
            int itemWidth = ICON_RENDER_SIZE + 4 + textWidth;

            if (x != startX && x + itemWidth > startX + maxWidth) {
                x = startX;
                y += rowHeight + ICON_GAP;
            }

            boolean increase = info.value() > info.defaultValue();
            int tintColor = increase ? 0x5A55FF55 : 0x5AFF5555;
            context.drawTexture(CLASS_BACKGROUND_TEXTURE, x - 1, y - 1, 148, 42, 13, 13);

//            context.fill(x - 1, y - 1, x + ICON_RENDER_SIZE + 1, y + ICON_RENDER_SIZE + 1, tintColor);
            context.drawTexture(info.icon(), x + 1, y + 1, ICON_RENDER_SIZE, ICON_RENDER_SIZE, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            int textY = y + 1 + (ICON_RENDER_SIZE - this.textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(this.textRenderer, Text.literal(valueStr), x + ICON_RENDER_SIZE + 4, textY, COLOR_VALUE);

            if (mouseX >= x && mouseX < x + itemWidth && mouseY >= y && mouseY < y + ICON_RENDER_SIZE) {
                context.drawTooltip(this.textRenderer, info.label(), mouseX, mouseY);
            }

            x += itemWidth + ICON_GAP;
        }

        return y + rowHeight;
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