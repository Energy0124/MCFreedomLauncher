package net.minecraft.launcher.ui.tabs.website;

import java.awt.*;

public abstract interface Browser {
    public abstract void loadUrl(String paramString);

    public abstract Component getComponent();

    public abstract void resize(Dimension paramDimension);
}