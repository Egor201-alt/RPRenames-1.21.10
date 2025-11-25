package com.HiWord9.RPRenames.mod.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow
    @Nullable
    private Runnable tooltipDrawer;

    @Shadow
    public abstract void drawTooltipImmediately(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, @Nullable Identifier texture);

    @Unique
    private final List<Runnable> extraTooltipDrawers = new ArrayList<>();

    @Inject(
            method = "drawTooltipImmediately(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"), 
            cancellable = true
    )
    private void onDrawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, @Nullable Identifier texture, CallbackInfo ci) {
        if (!components.isEmpty()) {
            if (tooltipDrawer == null) {
                tooltipDrawer = () -> this.drawTooltipImmediately(textRenderer, components, x, y, positioner, texture);
            } else {
                extraTooltipDrawers.add(() -> this.drawTooltipImmediately(textRenderer, components, x, y, positioner, texture));
            }
            ci.cancel();
        }
    }

    @Inject(method = "draw(Z)V", at = @At("TAIL"))
    private void afterDraw(boolean bl, CallbackInfo ci) {
        if (!extraTooltipDrawers.isEmpty()) {
            for (Runnable drawer : extraTooltipDrawers) {
                drawer.run();
            }
            extraTooltipDrawers.clear();
        }
    }
}
