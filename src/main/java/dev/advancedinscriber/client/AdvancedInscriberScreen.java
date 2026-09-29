/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.advancedinscriber.client;

import dev.advancedinscriber.container.AdvancedInscriberContainer;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;

public final class AdvancedInscriberScreen extends UpgradeableScreen<AdvancedInscriberContainer> {

    private final ProgressBar progressBar;

    public AdvancedInscriberScreen(AdvancedInscriberContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);
        this.progressBar = new ProgressBar(this.container, style.getImage("progressBar"),
                ProgressBar.Direction.VERTICAL);
        this.widgets.add("progressBar", this.progressBar);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int max = this.container.getMaxProgress();
        int progress = max <= 0 ? 0 : this.container.getCurrentProgress() * 100 / max;
        this.progressBar.setFullMsg(new StringTextComponent(progress + "%"));
    }
}
