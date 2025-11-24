package com.HiWord9.RPRenames.mod.gui.widget.external;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.gui.widget.OffsetableWidget;
import com.HiWord9.RPRenames.mod.gui.widget.RPRWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OpenerButton extends ClickableWidget implements OffsetableWidget {
    private static final Identifier TEXTURE = Identifier.of(RPRenames.MOD_ID, "textures/gui/opener.png");

    RPRWidget rprWidget;

    static final int BUTTON_WIDTH = 22;
    static final int BUTTON_HEIGHT = 22;

    static final int TEXTURE_WIDTH = 22;
    static final int TEXTURE_HEIGHT = 88;
    static final int FOCUSED_OFFSET_V = 22;
    static final int OPENED_OFFSET_V = 44;

    public OpenerButton(RPRWidget instance, int x, int y) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Text.empty());
        rprWidget = instance;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int u = 0;
        int v = 0;
        v += rprWidget.isOpen() ? OPENED_OFFSET_V : 0;
        v += isHovered() ? FOCUSED_OFFSET_V : 0;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                getX(), getY(),
                (float) u, (float) v,
                getWidth(), getHeight(),
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        if (this.isMouseOver(mouseX, mouseY) && button == 0) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            execute();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public void execute() {
        rprWidget.toggleOpen();
    }
    
    @Override
    public void offset(int x, int y) {
        setX(getX() + x);
        setY(getY() + y);
    }
}
