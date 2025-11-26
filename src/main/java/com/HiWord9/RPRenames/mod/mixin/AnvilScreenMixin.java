package com.HiWord9.RPRenames.mod.mixin;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.gui.RPRInteractableScreen;
import com.HiWord9.RPRenames.mod.gui.widget.GhostCraft;
import com.HiWord9.RPRenames.mod.gui.widget.Offsetable;
import com.HiWord9.RPRenames.mod.gui.widget.OffsetableWidget;
import com.HiWord9.RPRenames.mod.gui.widget.RPRWidget; 
import com.HiWord9.RPRenames.mod.gui.widget.external.FavoriteButton;
import com.HiWord9.RPRenames.mod.gui.widget.external.OpenerButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.HiWord9.RPRenames.mod.util.Util.*;

@Mixin(value = AnvilScreen.class, priority = 1200)
public abstract class AnvilScreenMixin extends Screen implements RPRInteractableScreen, Offsetable {
    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    private TextFieldWidget nameField;

    boolean afterPutInAnvilFirst = false;
    boolean afterPutInAnvilSecond = false;

    private static final int MENU_SHIFT = 77;

    RPRWidget rprWidget = new RPRWidget();

    GhostCraft ghostCraft;
    OpenerButton opener;
    FavoriteButton favoriteButton;

    @Unique boolean rpr_initialized = false; 
    @Unique int rpr_lastWidth = -1;
    @Unique int rpr_lastHeight = -1;

    @Unique
    private void rprInit() {
        if (shouldNotModify()) return;
        
        AnvilScreen screen = (AnvilScreen) (Object) this;
        int x = screen.x;
        int y = screen.y;

        opener = new OpenerButton(rprWidget, x + 3, y + 44);
        favoriteButton = new FavoriteButton(rprWidget, x, y, config().favoriteButtonPosition);

        var slots = screen.getScreenHandler().slots;
        ghostCraft = new GhostCraft(
                new GhostCraft.GhostSlot(x + slots.get(0).x - 1, y + slots.get(0).y - 1),
                new GhostCraft.GhostSlot(x + slots.get(1).x - 1, y + slots.get(1).y - 1),
                new GhostCraft.GhostSlot(x + slots.get(2).x - 1, y + slots.get(2).y - 1)
        );

        RPRInteractableScreen rprInteractableScreen = null;
        if ((Object)this instanceof RPRInteractableScreen interactable) {
            rprInteractableScreen = interactable;
        }

        rprWidget.init(
                x - RPRWidget.WIDGET_WIDTH - 1, y,
                rprInteractableScreen,
                RPRenames.renamesManager,
                RPRenames.favoritesManager,
                nameField,
                favoriteButton,
                ghostCraft
        );

        if (config().openByDefault) opener.execute();
        if (rprWidget.isOpen()) updateMenuShift();
    }

    @Inject(at = @At("HEAD"), method = "drawForeground")
    private void onDrawForegroundHead(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!rpr_initialized || this.width != rpr_lastWidth || this.height != rpr_lastHeight) {
            this.rpr_lastWidth = this.width;
            this.rpr_lastHeight = this.height;
            this.rpr_initialized = true;
            rprInit();
        }
    }

    @Inject(at = @At("RETURN"), method = "onRenamed")
    private void newNameEntered(String name, CallbackInfo ci) {
        if (shouldNotModify() || !rpr_initialized) return;
        rprWidget.updatedName();
    }

    public boolean mouseClicked(Click click, boolean isReleased) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (this.myMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseClicked(click, isReleased);
    }

    public boolean myMouseClicked(double mouseX, double mouseY, int button) {
        afterPutInAnvilFirst = false;
        afterPutInAnvilSecond = false;
        
        if (opener != null && opener.mouseClicked(mouseX, mouseY, button)) return true;
        if (favoriteButton != null && favoriteButton.mouseClicked(mouseX, mouseY, button)) return true;
        
        if (ghostCraft != null && ghostCraft.mouseClicked(mouseX, mouseY, button)) {
            if (rprWidget.getActiveItemStack().isEmpty()) {
                nameField.setText("");
                if (rprWidget.getCurrentTab().forCraftItemOnly) {
                    rprWidget.resetPageContent();
                }
            }
        }
    
        if (rprWidget.mouseClicked(new Click(mouseX, mouseY, new MouseInput(button, 0)), false)) return true;
        
        return false;
    }

    @Inject(at = @At("HEAD"), method = "onSlotUpdate", cancellable = true)
    private void itemUpdateHead(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (shouldNotModify()) return;
        
        if (!rpr_initialized) return; 

        if (slotId != 0) return;

        if (config().fixDelayedPacketsChangingTab) {
            if (afterPutInAnvilFirst) {
                afterPutInAnvilFirst = false;
                afterPutInAnvilSecond = true;
                return;
            }

            if (afterPutInAnvilSecond) {
                afterPutInAnvilSecond = false;
                ci.cancel();
                return;
            }
        }

        if (ItemStack.areEqual(stack, rprWidget.getActiveItemStack())) ci.cancel();
    }

    @Inject(at = @At("RETURN"), method = "onSlotUpdate")
    private void itemUpdateReturn(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (shouldNotModify()) return;
        
        if (!rpr_initialized) return; 
        
        rprWidget.updatedItem(slotId, stack);
    }

    @Override
    public void updateMenuShift() {
        if (!config().offsetMenu) return;
        offset(MENU_SHIFT * (rprWidget.isOpen() ? 1 : -1), 0);
    }

    @Override
    public void offset(int x, int y) {
        var screen = (AnvilScreen) (Object) this;
        screen.x += x;
        screen.y += y;

        OffsetableWidget.offset(nameField, x, y);
        offsetWidgets(x, y);
    }

    private void offsetWidgets(int x, int y) {
        if (opener != null) opener.offset(x, y);
        if (favoriteButton != null) favoriteButton.offset(x, y);
        rprWidget.offset(x, y);
        if (ghostCraft != null) ghostCraft.offset(x, y);
    }

    @Inject(at = @At("HEAD"), method = "drawForeground")
    private void onDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldNotModify()) return;
        if (client == null || client.currentScreen == null) return;
        if (opener == null) return; 

        AnvilScreen screen = (AnvilScreen) client.currentScreen;
        int xScreenOffset = screen.x;
        int yScreenOffset = screen.y;
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float)-xScreenOffset, (float)-yScreenOffset);

        opener.render(context, mouseX, mouseY, 0);
        favoriteButton.render(context, mouseX, mouseY, 0);
        ghostCraft.render(context, mouseX, mouseY, 0);
        rprWidget.render(context, mouseX, mouseY, 0);

        context.getMatrices().popMatrix();
    }

    @Override
    public void moveToCraft(int inventorySlot, int craftSlot) {
        if (client == null) return;

        if (
                config().fixDelayedPacketsChangingTab
                && !client.isInSingleplayer()
                && !rprWidget.getActiveItemStack().isEmpty()
        ) afterPutInAnvilFirst = true;

        RPRInteractableScreen.super.moveToCraft(inventorySlot, craftSlot);
    }

    @Override
    public int getCraftSlotsAmount() {
        return 3;
    }

    private boolean shouldNotModify() {
        return !config().enableAnvilModification;
    }
}
