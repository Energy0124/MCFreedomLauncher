package net.minecraft.launcher.ui.popups.login;

import net.minecraft.launcher.Http;
import java.net.URL;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.exception.ExceptionUtils;
import javax.swing.SwingUtilities;
import java.awt.Component;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import com.google.gson.TypeAdapterFactory;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AuthErrorForm extends JPanel
{
    private final LogInPopup popup;
    private final JLabel errorLabel;
    private final Gson gson;
    
    public AuthErrorForm(final LogInPopup popup) {
        super();
        this.errorLabel = new JLabel();
        this.gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();
        this.popup = popup;
        this.createInterface();
        this.clear();
    }
    
    protected void createInterface() {
        this.setBorder(new EmptyBorder(0, 0, 15, 0));
        this.errorLabel.setFont(this.errorLabel.getFont().deriveFont(1));
        this.add(this.errorLabel);
    }
    
    public void clear() {
        this.setVisible(false);
    }
    
    @Override
    public void setVisible(final boolean value) {
        super.setVisible(value);
        this.popup.repack();
    }
    
    public void displayError(final Throwable throwable, final String... lines) {
        if (SwingUtilities.isEventDispatchThread()) {
            String error = "";
            for (final String line : lines) {
                error = error + "<p>" + line + "</p>";
            }
            if (throwable != null) {
                error = error + "<p style='font-size: 0.9em; font-style: italic;'>(" + ExceptionUtils.getRootCauseMessage(throwable) + ")</p>";
            }
            this.errorLabel.setText("<html><div style='text-align: center;'>" + error + " </div></html>");
            if (!this.isVisible()) {
                this.refreshStatuses();
            }
            this.setVisible(true);
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    AuthErrorForm.this.displayError(throwable, lines);
                }
            });
        }
    }
    
    public void refreshStatuses() {
        this.popup.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final TypeToken<Map<String, ServerStatus>> token = new TypeToken<Map<String, ServerStatus>>() {};
                    final Map<String, ServerStatus> statuses = AuthErrorForm.this.gson.fromJson(Http.performGet(new URL("http://status.mojang.com/check?service=authserver.mojang.com"), AuthErrorForm.this.popup.getLauncher().getProxy()), token.getType());
                    if (statuses.get("authserver.mojang.com") == ServerStatus.RED) {
                        AuthErrorForm.this.displayError(null, "It looks like our servers are down right now. Sorry!", "We're already working on the problem and will have it fixed soon.", "Please try again later!");
                    }
                }
                catch (Exception ex) {}
            }
        });
    }
    
    public enum ServerStatus
    {
        GREEN("Online, no problems detected."), 
        YELLOW("May be experiencing issues."), 
        RED("Offline, experiencing problems.");
        
        private final String title;
        
        private ServerStatus(final String title) {
            this.title = title;
        }
    }
}
