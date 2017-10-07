package net.minecraft.launcher.ui.tabs.website;

import java.awt.*;

public interface Browser {
    void loadUrl(final String p0);

    Component getComponent();

    void resize(final Dimension p0);
}
