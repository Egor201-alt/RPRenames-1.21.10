package com.HiWord9.RPRenames.mod.gui;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.gui.widget.external.FavoriteButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.HiWord9.RPRenames.mod.util.Util.*;

public class Graphics {
    static public final int DEFAULT_PREVIEW_WIDTH = 42;
    static public final int DEFAULT_PREVIEW_HEIGHT = 42;
    static public final int DEFAULT_PREVIEW_SIZE_ENTITY = 32;
    static public final int DEFAULT_PREVIEW_SIZE_ITEM = 16;

    static public final int SLOT_SIZE = 18;
    static public final int STACK_IN_SLOT_SIZE = 16;

    public static final int HIGHLIGHT_COLOR_WRONG = 822018048;
    public static final int HIGHLIGHT_COLOR_SECOND = 822018303;

    static public final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;

    static public boolean renderTooltipAsFavorite = false;

    public static final Identifier FAVORITE_TOOLTIP_FRAME_TEXTURE = Identifier.of(RPRenames.MOD_ID, "favorite_tooltip");

    public static void renderText(DrawContext context, Text text, int x, int y, boolean shadow, boolean centered) {
        renderText(context, text, DEFAULT_TEXT_COLOR, x, y, shadow, centered);
    }

    public static void renderText(DrawContext context, Text text, int color, int x, int y, boolean shadow, boolean centered) {
        var renderer = textRenderer();
        int xOffset = centered ? renderer.getWidth(text) / 2 : 0;
        context.drawText(renderer, text, x - xOffset, y, color, shadow);
    }

    public static void renderStack(DrawContext context, ItemStack itemStack, int x, int y) {
        renderStack(context, itemStack, x, y, 0, STACK_IN_SLOT_SIZE);
    }

    public static void renderStack(DrawContext context, ItemStack itemStack, int x, int y, int z, int size) {
        float scale = size != STACK_IN_SLOT_SIZE ? ((float) size / STACK_IN_SLOT_SIZE) : 1f;

        MatrixStack matrices = context.getMatrices();
        matrices.push(); // pushMatrix -> push
        matrices.translate(x, y, z);
        matrices.scale(scale, scale, 1.0f);
        
        context.drawItem(itemStack, 0, 0); 
        matrices.pop(); // popMatrix -> pop
    }

    public static void renderEntityInBox(DrawContext context, ScreenRect rect, int size, Entity entity, boolean spin) {
        renderEntityInBox(context, rect, size, entity, spin, 500);
    }

    public static void renderEntityInBox(DrawContext context, ScreenRect rect, double size, Entity entity, boolean spin, int z) {
        context.enableScissor(
                rect.getLeft(), rect.getTop(),
                rect.getRight(), rect.getBottom()
        );
        renderEntity(context, rect.getLeft(), rect.getTop(),
                rect.getRight(), rect.getBottom(),
                size, entity, spin);
        context.disableScissor();
    }

    public static void renderEntity(DrawContext context, int x1, int y1, int x2, int y2, double size, Entity entity, boolean spin) {
        if (entity instanceof SquidEntity) size /= 1.5;
        else if (entity instanceof ItemEntity) size *= 2;

        if (entity instanceof LivingEntity l && l.isBaby()) size /= 1.7;

        var camera = client().getCameraEntity();
        if (camera != null) {
            entity.setPos(camera.getX(), camera.getY(), camera.getZ());
        }

        if (!(entity instanceof PlayerEntity)) {
            assert player() != null;
            entity.age = player().age;
        }

        float yaw = spin ? (float) (((System.currentTimeMillis() / 10)) % 360) : 225.0F;
        float mouseX = (float) Math.sin(Math.toRadians(yaw)) * 100f;
        float mouseY = 0f;

        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;

        int entityY = centerY + (int)(size * 0.4); 

        if (entity instanceof LivingEntity living) {
            InventoryScreen.drawEntity(context, centerX, entityY, (int)size, -mouseX, mouseY, living);
        } 
        else if (entity instanceof ItemEntity itemEntity) {
            renderStack(context, itemEntity.getStack(), centerX - 8, centerY - 8, 0, (int)size);
        }
    }

    public static void drawTooltip(
            DrawContext context, TextRenderer textRenderer,
            List<TooltipComponent> components,
            int x, int y,
            TooltipPositioner positioner
    ) {
        drawTooltip(context, textRenderer, components, x, y, positioner, false);
    }

    public static void drawTooltip(
            DrawContext context, TextRenderer textRenderer,
            TooltipComponent component,
            int x, int y,
            TooltipPositioner positioner,
            boolean favorite
    ) {
        drawTooltip(context, textRenderer, List.of(component), x, y, positioner, favorite);
    }

    public static void drawTooltipWithFixedBorders(
            DrawContext context, TextRenderer textRenderer,
            TooltipComponent component,
            int x, int y,
            TooltipPositioner positioner,
            boolean favorite
    ) {
        drawTooltip(
                context, textRenderer,
                List.of(component,
                        new TooltipComponent() { //dump tooltip component to increase list size
                            public int getHeight(TextRenderer textRenderer) {return 0;}
                            public int getWidth(TextRenderer textRenderer) {return 0;}
                        }
                ),
                x, y, positioner, favorite
        );
    }

    public static void drawTooltip(
            DrawContext context, TextRenderer textRenderer,
            List<TooltipComponent> components,
            int x, int y,
            TooltipPositioner positioner,
            boolean favorite
    ) {
        renderTooltipAsFavorite = favorite;
        context.drawTooltip(textRenderer, components, x, y, positioner, null);
        renderTooltipAsFavorite = false;
    }

    public static void renderStarInFavoriteTooltip(DrawContext context, int x, int y, int width) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0,0,0);
        
        context.drawTexture(
                RenderLayer::getGuiTextured,
                FavoriteButton.TEXTURE,
                x + width - (FavoriteButton.BUTTON_WIDTH), y,
                0, 0,
                FavoriteButton.BUTTON_WIDTH, FavoriteButton.BUTTON_HEIGHT,
                FavoriteButton.TEXTURE_WIDTH, FavoriteButton.TEXTURE_HEIGHT
        );
        matrices.pop();
    }

    public static <H extends ScreenHandler, S extends HandledScreen<H> & RPRInteractableScreen> void highlightAvailableSlots(
            List<Item> items, DrawContext context, S screen, int color
    ) {
        var allSlots = screen.getScreenHandler().slots;

        var slotsToHighlight = new ArrayList<Slot>();
        if (!allSlots.isEmpty()) {
            slotsToHighlight.add(allSlots.getFirst());
            if (screen.getCraftSlotsAmount() < allSlots.size()) {
                slotsToHighlight.addAll(
                        allSlots.subList(screen.getCraftSlotsAmount(), allSlots.size())
                );
            }
        }

        highlightSlots(items, slotsToHighlight, context, screen.x, screen.y, color);
    }

    public static void highlightSlots(
            List<Item> items, List<Slot> slots,
            DrawContext context,
            int xOffset, int yOffset,
            int color
    ) {
        for (Slot slot : slots)
            if (items.contains(slot.getStack().getItem()))
                highlightSlot(context, xOffset, yOffset, slot, color);
    }

    public static void highlightSlot(DrawContext context, int xOffset, int yOffset, Slot slot, int color) {
        int x = xOffset + slot.x - 1;
        int y = yOffset + slot.y - 1;
        context.fillGradient(x, y, x + SLOT_SIZE, y + SLOT_SIZE, color, color);
    }

    public static TooltipComponent tooltipOf(String string) {
        return tooltipOf(Text.of(string));
    }

    public static TooltipComponent tooltipOf(Text mutableText) {
        return TooltipComponent.of(mutableText.asOrderedText());
    }
}
