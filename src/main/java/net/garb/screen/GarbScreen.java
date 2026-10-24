package net.garb.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.GarbMain;
import net.garb.network.packet.C2SSelectCharacterPacket;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.util.PlayerModelRenderer;
import net.garb.util.SkinLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class GarbScreen extends Screen {

    private record SlotSkin(Identifier textureId, boolean slim) {
    }

    private final List<S2CCharacterDataPacket.SlotInfo> slots;
    private final List<SlotSkin> slotSkins;

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 160;
    private static final int CARD_GAP = 16;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CARD_PADDING = 10;
    private static final int EXIT_BUTTON_SIZE = 20;
    private static final int EXIT_BUTTON_MARGIN = 6;
    private static final int CARD_MODEL_HEIGHT = 40;
    private static final int CARD_MODEL_GAP = 6;

    private static final Identifier CHARACTER_SLOT_TEXTURE = GarbMain.identifierOf("textures/gui/character_slot.png");

    private static final int COLOR_CARD_BORDER = 0xFF555555;
    private static final int COLOR_CARD_HOVER = 0x30FFFFFF;
    private static final int COLOR_EMPTY_TEXT = 0xFF707070;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_CARD_TEXT = 0xFF404040;
    private static final int COLOR_LABEL = 0xFF606060;
    private static final int COLOR_HP = 0xFFAA0000;
    private static final int COLOR_HUNGER = 0xFFAA5500;
    private static final int COLOR_XP = 0xFF00AA00;

    private static final Identifier ICON_HEALTH = GarbMain.identifierOf("textures/attribute/generic.max_health.png");
    private static final Identifier ICON_HUNGER = GarbMain.identifierOf("textures/attribute/generic.hunger.png");
    private static final Identifier ICON_XP = Identifier.ofVanilla("textures/entity/experience_orb.png");
    private static final int ICON_SIZE = 9;

    private final String errorKey;
    private final Consumer<C2SSelectCharacterPacket> selectCallback;
    private final Runnable exitCallback;

    public GarbScreen(List<S2CCharacterDataPacket.SlotInfo> slots, String errorKey, Consumer<C2SSelectCharacterPacket> selectCallback, Runnable exitCallback) {
        super(Text.translatable("screen.garb.title"));
        this.slots = slots;
        this.slotSkins = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            this.slotSkins.add(null);
        }
        this.errorKey = errorKey;
        this.selectCallback = selectCallback;
        this.exitCallback = exitCallback;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int totalWidth = slots.size() * CARD_WIDTH + (slots.size() - 1) * CARD_GAP;
        int startX = (this.width - totalWidth) / 2;
        int cardsY = (this.height - CARD_HEIGHT) / 2;
        int buttonY = cardsY + CARD_HEIGHT + 10;

        for (int i = 0; i < slots.size(); i++) {
            final int index = i;
            S2CCharacterDataPacket.SlotInfo slot = slots.get(i);

            if (this.slotSkins.get(index) == null && slot.exists() && slot.skinUsername() != null && !slot.skinUsername().isEmpty()) {
                SkinLoader.loadForUsername(slot.skinUsername(), result -> {
                    if (result.success()) {
                        this.slotSkins.set(index, new SlotSkin(result.textureId(), result.slim()));
                    }
                });
            }
        }

        for (int i = 0; i < slots.size(); i++) {
            final int slot = i;
            int cardX = startX + i * (CARD_WIDTH + CARD_GAP);

            String label = slots.get(i).exists() ? Text.translatable("screen.garb.select").getString() : Text.translatable("screen.garb.create").getString();

            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                if (slots.get(slot).exists()) {
                    selectCallback.accept(new C2SSelectCharacterPacket(slot, null, null, null));
                    this.client.setScreen(null);
                } else {
                    this.client.setScreen(new CreateCharacterScreen(this, result -> {
                        selectCallback.accept(new C2SSelectCharacterPacket(slot, result.characterName(), result.skinUsername(), result.classId()));
                    }));
                }
            }).position(cardX, buttonY).size(CARD_WIDTH, BUTTON_HEIGHT).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.garb.exit"), btn -> exitCallback.run())
                .tooltip(Tooltip.of(Text.translatable("menu.disconnect")))
                .position(this.width - EXIT_BUTTON_SIZE - EXIT_BUTTON_MARGIN, EXIT_BUTTON_MARGIN)
                .size(EXIT_BUTTON_SIZE, EXIT_BUTTON_SIZE).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int totalWidth = slots.size() * CARD_WIDTH + (slots.size() - 1) * CARD_GAP;
        int startX = (this.width - totalWidth) / 2;
        int cardsY = (this.height - CARD_HEIGHT) / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.title"), this.width / 2, cardsY - 24, COLOR_TITLE);
        if (errorKey != null && !errorKey.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(errorKey), this.width / 2, cardsY - 12, COLOR_ERROR);
        }

        for (int i = 0; i < slots.size(); i++) {
            int cardX = startX + i * (CARD_WIDTH + CARD_GAP);
            boolean hovered = isMouseOverCard(mouseX, mouseY, cardX, cardsY);
            drawCard(context, slots.get(i), i, cardX, cardsY, hovered);
        }
    }

    private void drawCard(DrawContext context, S2CCharacterDataPacket.SlotInfo slot, int index, int x, int y, boolean hovered) {

        context.drawTexture(CHARACTER_SLOT_TEXTURE, x, y, 0, 0, CARD_WIDTH, CARD_HEIGHT);
        if (hovered) {
            context.fill(x + 1, y + 1, x + CARD_WIDTH - 2, y + CARD_HEIGHT - 2, COLOR_CARD_HOVER);
        }

        int textX = x + CARD_PADDING;
        int textY = y + CARD_PADDING;
        int lineHeight = 12;

        context.drawText(this.textRenderer, Text.translatable("screen.garb.slot", index + 1), textX, textY, COLOR_CARD_TEXT, false);
        textY += lineHeight + 4;

        context.fill(x + CARD_PADDING, textY, x + CARD_WIDTH - CARD_PADDING, textY + 1, COLOR_CARD_BORDER);
        textY += 6;

        if (!slot.exists()) {
            int emptyY = y + CARD_HEIGHT / 2 - 10;
            drawCenteredText(context, Text.translatable("screen.garb.empty"), x + CARD_WIDTH / 2, emptyY, COLOR_EMPTY_TEXT);
            return;
        }

        if (slot.name() != null && !slot.name().isEmpty()) {
            drawCenteredText(context, Text.literal(slot.name()), x + CARD_WIDTH / 2, textY, COLOR_CARD_TEXT);
            textY += lineHeight;
        }

        SlotSkin skin = index < this.slotSkins.size() ? this.slotSkins.get(index) : null;
        if (skin != null) {
            PlayerModelRenderer.draw(context, skin.textureId(), skin.slim(), slot.armor(), x + CARD_WIDTH / 2, textY, CARD_MODEL_HEIGHT);
            textY += CARD_MODEL_HEIGHT + CARD_MODEL_GAP;
        } else {
            drawCenteredText(context, Text.literal("..."), x + CARD_WIDTH / 2, textY + 10, COLOR_EMPTY_TEXT);
            textY += 26;
        }

        drawIconStat(context, ICON_HEALTH, Text.literal(String.format("%.1f / %.1f", slot.health(), slot.maxHealth())), textX, textY, COLOR_HP);
        textY += lineHeight + 2;

        drawIconStat(context, ICON_HUNGER, Text.literal(slot.foodLevel() + " / 20"), textX, textY, COLOR_HUNGER);
        textY += lineHeight + 2;

        drawIconStat(context, ICON_XP, Text.translatable("screen.garb.level", slot.xpLevel()), textX, textY, COLOR_XP);
        textY += lineHeight + 2;

        context.drawText(this.textRenderer, Text.translatable("screen.garb.last_played"), textX, textY, COLOR_LABEL, false);
        context.drawText(this.textRenderer, formatLastPlayed(slot.lastPlayed()), textX, textY + lineHeight, COLOR_CARD_TEXT, false);
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(this.textRenderer, text, centerX - this.textRenderer.getWidth(text) / 2, y, color, false);
    }

    private boolean isMouseOverCard(int mouseX, int mouseY, int cardX, int cardY) {
        return mouseX >= cardX && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
    }

    private Text formatLastPlayed(long timestamp) {
        if (timestamp <= 0) {
            return Text.of("-");
        }
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return Text.translatable("screen.garb.last_played.d_ago", days);
        }
        if (hours > 0) {
            return Text.translatable("screen.garb.last_played.h_ago", hours);
        }
        if (minutes > 0) {
            return Text.translatable("screen.garb.last_played.m_ago", minutes);
        }
        return Text.translatable("screen.garb.last_played.just_now");
    }

    private void drawIconStat(DrawContext context, Identifier icon, Text valueText, int x, int y, int color) {
        if (icon.equals(ICON_XP)) {
            context.setShaderColor(0.5f, 1.0f, 0.2f, 1.0f);
            context.drawTexture(icon, x, y, ICON_SIZE, ICON_SIZE, 32, 32, 16, 16, 64, 64);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            context.drawTexture(icon, x, y, ICON_SIZE, ICON_SIZE, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
        int textY = y + (ICON_SIZE - this.textRenderer.fontHeight) / 2;
        context.drawText(this.textRenderer, valueText, x + ICON_SIZE + 4, textY, color, false);
    }

}