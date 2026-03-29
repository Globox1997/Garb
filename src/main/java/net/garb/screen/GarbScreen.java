package net.garb.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.util.SkinLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
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

    private static final int COLOR_CARD_BG = 0xCC1A1A1A;
    private static final int COLOR_CARD_BORDER = 0xFF555555;
    private static final int COLOR_CARD_HOVER = 0xCC2A2A2A;
    private static final int COLOR_EMPTY_TEXT = 0xFF888888;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_HP = 0xFFFF5555;
    private static final int COLOR_HUNGER = 0xFFFFAA55;
    private static final int COLOR_XP = 0xFF55FF55;

    private int hoveredSlot = -1;

    private final Consumer<Integer> selectCallback;

    private final ClientConfigurationNetworkHandler clientConfigurationNetworkHandler;

    public GarbScreen(List<S2CCharacterDataPacket.SlotInfo> slots, Consumer<Integer> selectCallback) {
        super(Text.translatable("screen.garb.title"));
        this.slots = slots;
        this.slotSkins = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            this.slotSkins.add(null);
        }
        this.selectCallback = selectCallback;

        this.clientConfigurationNetworkHandler = clientConfigurationNetworkHandler;
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

            if (slot.exists() && slot.skinUsername() != null && !slot.skinUsername().isEmpty()) {
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


            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> selectSlot(slot)).position(cardX, buttonY).size(CARD_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int totalWidth = slots.size() * CARD_WIDTH + (slots.size() - 1) * CARD_GAP;
        int startX = (this.width - totalWidth) / 2;
        int cardsY = (this.height - CARD_HEIGHT) / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.title"), this.width / 2, cardsY - 24, COLOR_TITLE);

        hoveredSlot = -1;
        for (int i = 0; i < slots.size(); i++) {
            int cardX = startX + i * (CARD_WIDTH + CARD_GAP);
            boolean hovered = isMouseOverCard(mouseX, mouseY, cardX, cardsY);
            if (hovered) hoveredSlot = i;
            drawCard(context, slots.get(i), i, cardX, cardsY, hovered);
        }
    }

    private void drawCard(DrawContext context, S2CCharacterDataPacket.SlotInfo slot, int index, int x, int y, boolean hovered) {

        int bgColor = hovered ? COLOR_CARD_HOVER : COLOR_CARD_BG;

        context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

        context.drawBorder(x, y, CARD_WIDTH, CARD_HEIGHT, COLOR_CARD_BORDER);

        int textX = x + CARD_PADDING;
        int textY = y + CARD_PADDING;
        int lineHeight = 12;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.slot", index + 1), textX, textY, COLOR_TITLE);
        textY += lineHeight + 4;

        context.fill(x + CARD_PADDING, textY, x + CARD_WIDTH - CARD_PADDING, textY + 1, COLOR_CARD_BORDER);
        textY += 6;

        if (!slot.exists()) {
            int emptyY = y + CARD_HEIGHT / 2 - 10;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.garb.empty"), x + CARD_WIDTH / 2, emptyY, COLOR_EMPTY_TEXT);
            return;
        }

        SlotSkin skin = index < this.slotSkins.size() ? this.slotSkins.get(index) : null;
        if (skin != null) {
            int scale = 5;
            int playerX = x + (CARD_WIDTH / 2) - (scale * 2);

            if (slot.name() != null && !slot.name().isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(slot.name()), x + CARD_WIDTH / 2, textY - 10, COLOR_TITLE);
            }

            drawPlayer(context, skin.textureId(), skin.slim(), playerX, textY, scale);
            textY += scale * 8 + 6;
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("..."), x + CARD_WIDTH / 2, textY + 10, COLOR_EMPTY_TEXT);
            textY += 26;
        }

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.hp"), textX, textY, COLOR_LABEL);
        context.drawTextWithShadow(this.textRenderer, Text.literal(String.format("%.1f / 20.0", slot.health())), textX, textY + lineHeight, COLOR_HP);
        textY += lineHeight * 2 + 4;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.hunger"), textX, textY, COLOR_LABEL);
        context.drawTextWithShadow(this.textRenderer, Text.literal(slot.foodLevel() + " / 20"), textX, textY + lineHeight, COLOR_HUNGER);
        textY += lineHeight * 2 + 4;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.xp"), textX, textY, COLOR_LABEL);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Level " + slot.xpLevel()), textX, textY + lineHeight, COLOR_XP);
        textY += lineHeight * 2 + 4;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.garb.last_played"), textX, textY, COLOR_LABEL);
        context.drawTextWithShadow(this.textRenderer, Text.literal(formatLastPlayed(slot.lastPlayed())), textX, textY + lineHeight, COLOR_VALUE);

    }

    private boolean isMouseOverCard(int mouseX, int mouseY, int cardX, int cardY) {
        return mouseX >= cardX && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
    }

    private String formatLastPlayed(long timestamp) {
        if (timestamp <= 0) return "-";
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "Just now";
    }

    private void selectSlot(int slot) {
//        ClientPlayNetworking.send(new C2SSelectCharacterPacket(slot));
        this.selectCallback.accept(slot);
        this.client.setScreen(null);
    }

    private static void drawPlayer(DrawContext context, Identifier skin, boolean slim, int x, int y, int scale) {
        drawSkinPart(context, skin, x + scale, y, scale * 2, scale * 2, 8, 8, 8, 8);
        drawSkinPart(context, skin, x + scale, y + scale * 2, scale * 2, scale * 3, 20, 20, 8, 12);

        int armW = slim ? 3 : 4;
        drawSkinPart(context, skin, x, y + scale * 2, scale, scale * 3, 44, 20, armW, 12);
        drawSkinPart(context, skin, x + scale * 3, y + scale * 2, scale, scale * 3, 36, 52, armW, 12);

        drawSkinPart(context, skin, x + scale, y + scale * 5, scale, scale * 3, 4, 20, 4, 12);
        drawSkinPart(context, skin, x + scale * 2, y + scale * 5, scale, scale * 3, 20, 52, 4, 12);

        drawSkinPart(context, skin, x + scale - 1, y - 1, scale * 2 + 2, scale * 2 + 2, 40, 8, 8, 8);
    }

    private static void drawSkinPart(DrawContext context, Identifier texture, int x, int y, int width, int height, int u, int v, int regionW, int regionH) {
        context.drawTexture(texture, x, y, width, height, u, v, regionW, regionH, 64, 64);
    }

}