package net.minecraft.launcher.ui.tabs.website;

import com.mojang.launcher.OperatingSystem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.awt.*;
import java.net.URI;

public class JFXBrowser implements Browser {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Object lock;
    private final JFXPanel fxPanel;
    private String urlToBrowseTo;
    private Dimension size;
    private WebView browser;
    private WebEngine webEngine;

    public JFXBrowser() {
        this.lock = new Object();
        this.fxPanel = new JFXPanel();
        Platform.runLater(() -> {
            final Group root = new Group();
            final Scene scene = new Scene(root);
            JFXBrowser.this.fxPanel.setScene(scene);
            synchronized (JFXBrowser.this.lock) {
                JFXBrowser.this.browser = new WebView();
                JFXBrowser.this.browser.setContextMenuEnabled(false);
                if (JFXBrowser.this.size != null) {
                    JFXBrowser.this.resize(JFXBrowser.this.size);
                }
                JFXBrowser.this.webEngine = JFXBrowser.this.browser.getEngine();
                JFXBrowser.this.webEngine.setJavaScriptEnabled(true);
                JFXBrowser.this.webEngine.getLoadWorker().stateProperty().addListener((ChangeListener) (observableValue, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        final EventListener listener = event -> {
                            if (event.getTarget() instanceof Element) {
                                Element element = (Element) event.getTarget();
                                String href = element.getAttribute("href");
                                while (StringUtils.isEmpty(href) && element.getParentNode() instanceof Element) {
                                    element = (Element) element.getParentNode();
                                    href = element.getAttribute("href");
                                }
                                if (href != null && href.length() > 0) {
                                    try {
                                        OperatingSystem.openLink(new URI(href));
                                    } catch (Exception e) {
                                        JFXBrowser.LOGGER.error("Unexpected exception opening link " + href, e);
                                    }
                                    event.preventDefault();
                                    event.stopPropagation();
                                }
                            }
                        };
                        final Document doc = JFXBrowser.this.webEngine.getDocument();
                        if (doc != null) {
                            final NodeList elements = doc.getElementsByTagName("a");
                            for (int i = 0; i < elements.getLength(); ++i) {
                                final Node item = elements.item(i);
                                if (item instanceof EventTarget) {
                                    ((EventTarget) item).addEventListener("click", listener, false);
                                }
                            }
                        }
                    }
                });
                if (JFXBrowser.this.urlToBrowseTo != null) {
                    JFXBrowser.this.loadUrl(JFXBrowser.this.urlToBrowseTo);
                }
            }
            root.getChildren().add(JFXBrowser.this.browser);
        });
    }

    @Override
    public void loadUrl(final String url) {
        synchronized (this.lock) {
            this.urlToBrowseTo = url;
            if (this.webEngine != null) {
                Platform.runLater(() -> JFXBrowser.this.webEngine.load(url));
            }
        }
    }

    @Override
    public Component getComponent() {
        return this.fxPanel;
    }

    @Override
    public void resize(final Dimension size) {
        synchronized (this.lock) {
            this.size = size;
            if (this.browser != null) {
                this.browser.setMinSize(size.getWidth(), size.getHeight());
                this.browser.setMaxSize(size.getWidth(), size.getHeight());
                this.browser.setPrefSize(size.getWidth(), size.getHeight());
            }
        }
    }
}
