package net.garb.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.character.CharacterData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.OnAStickItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PlayerModelRenderer {

    private static final float MODEL_HEIGHT = 2.0f;
    private static final float PLAYER_SCALE = 0.9375f;
    private static final float MODEL_Y_OFFSET = 1.501f;
    private static final float BODY_YAW = 25.0f;
    private static final float ARM_ROLL = 0.06f;
    private static final float Z_OFFSET = 50.0f;
    private static final int FULL_BRIGHT = 15728880;

    private static final float BACKSLOT_TRIDENT_YAW = 52.0f;
    private static final float BACKSLOT_TRIDENT_PITCH = 40.0f;
    private static final float BACKSLOT_TRIDENT_ROLL = -25.0f;
    private static final float BACKSLOT_TRIDENT_X = -0.26f;
    private static final float BACKSLOT_TRIDENT_NO_CHEST_X = 0.05f;
    private static final float BACKSLOT_BACK_Z = 0.16f;
    private static final float BACKSLOT_ROD_Y = -0.3f;
    private static final float BACKSLOT_CHEST_Z = 0.06f;
    private static final float BACKSLOT_BELT_X = 0.29f;
    private static final float BACKSLOT_BELT_Y = 0.5f;
    private static final float BACKSLOT_BELT_Z = 0.05f;
    private static final float BACKSLOT_FLINT_X = 0.01f;
    private static final float BACKSLOT_FLINT_Z = -0.1f;
    private static final float BACKSLOT_SMALL_ITEM_SCALE = 0.65f;
    private static final float BACKSLOT_SMALL_ITEM_NO_CHEST_Z = 0.015f;

    private static Models wideModels;
    private static Models slimModels;

    private record Models(PlayerEntityModel<LivingEntity> player, BipedEntityModel<LivingEntity> innerArmor, BipedEntityModel<LivingEntity> outerArmor) {
    }

    public static void draw(DrawContext context, Identifier skin, boolean slim, List<CharacterData.WornArmor> armor, int centerX, int topY, int height) {
        draw(context, skin, slim, armor, null, null, centerX, topY, height);
    }

    public static void draw(DrawContext context, Identifier skin, boolean slim, List<CharacterData.WornArmor> armor, CharacterData.CarriedItem backItem, CharacterData.CarriedItem beltItem, int centerX, int topY, int height) {
        Models models = getModels(slim);
        float size = height / (MODEL_HEIGHT * PLAYER_SCALE);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, topY + height, Z_OFFSET);
        matrices.scale(size, size, -size);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.PI));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(BODY_YAW));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        matrices.translate(0.0f, -MODEL_Y_OFFSET, 0.0f);

        DiffuseLighting.method_34742();
        VertexConsumerProvider vertexConsumers = context.getVertexConsumers();

        PlayerEntityModel<LivingEntity> player = models.player();
        player.setVisible(true);
        player.rightArm.roll = ARM_ROLL;
        player.leftArm.roll = -ARM_ROLL;
        player.rightSleeve.copyTransform(player.rightArm);
        player.leftSleeve.copyTransform(player.leftArm);
        player.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(skin)), FULL_BRIGHT, OverlayTexture.DEFAULT_UV);

        for (CharacterData.WornArmor piece : armor) {
            renderArmorPiece(matrices, vertexConsumers, models, piece);
        }

        boolean chestEquipped = armor.stream().anyMatch(piece -> toItem(piece.itemId()) instanceof ArmorItem armorItem && armorItem.getSlotType() == EquipmentSlot.CHEST);
        ItemStack backStack = toStack(backItem);
        if (!backStack.isEmpty()) {
            renderBackItem(matrices, vertexConsumers, player, backStack, chestEquipped);
        }
        ItemStack beltStack = toStack(beltItem);
        if (!beltStack.isEmpty()) {
            renderBeltItem(matrices, vertexConsumers, player, beltStack, chestEquipped);
        }

        context.draw();
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private static void renderBackItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, PlayerEntityModel<LivingEntity> player, ItemStack stack, boolean chestEquipped) {
        matrices.push();
        player.body.rotate(matrices);
        Item item = stack.getItem();
        if (item instanceof TridentItem) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(BACKSLOT_TRIDENT_YAW));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(BACKSLOT_TRIDENT_PITCH));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(BACKSLOT_TRIDENT_ROLL));
            matrices.translate(BACKSLOT_TRIDENT_X, 0.0f, 0.0f);
            if (!chestEquipped) {
                matrices.translate(BACKSLOT_TRIDENT_NO_CHEST_X, 0.0f, 0.0f);
            }
            matrices.scale(1.0f, -1.0f, -1.0f);
            renderItem(matrices, vertexConsumers, stack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        } else {
            matrices.translate(0.0f, 0.0f, BACKSLOT_BACK_Z);
            if (item instanceof FishingRodItem || item instanceof OnAStickItem) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
                matrices.translate(0.0f, BACKSLOT_ROD_Y, 0.0f);
            }
            if (chestEquipped) {
                matrices.translate(0.0f, 0.0f, BACKSLOT_CHEST_Z);
            }
            renderItem(matrices, vertexConsumers, stack, ModelTransformationMode.HEAD);
        }
        matrices.pop();
    }

    private static void renderBeltItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, PlayerEntityModel<LivingEntity> player, ItemStack stack, boolean chestEquipped) {
        matrices.push();
        player.body.rotate(matrices);
        Item item = stack.getItem();
        matrices.translate(BACKSLOT_BELT_X, BACKSLOT_BELT_Y, BACKSLOT_BELT_Z);
        if (item instanceof FlintAndSteelItem) {
            matrices.translate(BACKSLOT_FLINT_X, 0.0f, BACKSLOT_FLINT_Z);
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0f));
        if (item instanceof ShearsItem || item instanceof FlintAndSteelItem) {
            matrices.scale(BACKSLOT_SMALL_ITEM_SCALE, BACKSLOT_SMALL_ITEM_SCALE, BACKSLOT_SMALL_ITEM_SCALE);
            if (!chestEquipped) {
                matrices.translate(0.0f, 0.0f, BACKSLOT_SMALL_ITEM_NO_CHEST_Z);
            }
        }
        renderItem(matrices, vertexConsumers, stack, ModelTransformationMode.HEAD);
        matrices.pop();
    }

    private static void renderItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, ModelTransformationMode mode) {
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, mode, FULL_BRIGHT, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, null, 0);
    }

    private static Item toItem(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        return id != null && Registries.ITEM.containsId(id) ? Registries.ITEM.get(id) : null;
    }

    private static ItemStack toStack(CharacterData.CarriedItem carriedItem) {
        if (carriedItem == null) {
            return ItemStack.EMPTY;
        }
        Item item = toItem(carriedItem.itemId());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (carriedItem.glint()) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private static void renderArmorPiece(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Models models, CharacterData.WornArmor piece) {
        Identifier itemId = Identifier.tryParse(piece.itemId());
        if (itemId == null) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        if (!(item instanceof ArmorItem armorItem)) {
            return;
        }

        EquipmentSlot slot = armorItem.getSlotType();
        boolean inner = slot == EquipmentSlot.LEGS;
        BipedEntityModel<LivingEntity> model = inner ? models.innerArmor() : models.outerArmor();
        models.player().copyBipedStateTo(model);
        setArmorVisible(model, slot);

        int color = piece.color() != -1 ? ColorHelper.Argb.fullAlpha(piece.color()) : -1;
        ArmorMaterial material = armorItem.getMaterial().value();
        for (ArmorMaterial.Layer layer : material.layers()) {
            int layerColor = layer.isDyeable() ? color : -1;
            model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(layer.getTexture(inner))), FULL_BRIGHT, OverlayTexture.DEFAULT_UV, layerColor);
        }

        if (piece.glint()) {
            model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getArmorEntityGlint()), FULL_BRIGHT, OverlayTexture.DEFAULT_UV);
        }
    }

    private static void setArmorVisible(BipedEntityModel<LivingEntity> model, EquipmentSlot slot) {
        model.setVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    private static Models getModels(boolean slim) {
        if (slim) {
            if (slimModels == null) {
                slimModels = createModels(true);
            }
            return slimModels;
        }
        if (wideModels == null) {
            wideModels = createModels(false);
        }
        return wideModels;
    }

    private static Models createModels(boolean slim) {
        EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
        Models models = new Models(
                new PlayerEntityModel<>(loader.getModelPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim),
                new ArmorEntityModel<>(loader.getModelPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)),
                new ArmorEntityModel<>(loader.getModelPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR))
        );
        models.player().child = false;
        models.innerArmor().child = false;
        models.outerArmor().child = false;
        return models;
    }
}
