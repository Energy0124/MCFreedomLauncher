package org.hopto.energy;

import net.minecraft.launcher.Launcher;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstallDirSettings {
    public static String fileLocation = "./FreeLauncher.properties";

    public static File loadAtStartup(JFrame frame, File defaultWorkingDir) {
        Properties prop = new Properties();

        File file = new File(fileLocation);
        File workingDirectory;
        System.out.println(file.getAbsoluteFile());

        if (!file.exists()) {
            workingDirectory = changeDirInternal(frame, defaultWorkingDir, prop, file);
        } else {
            try {
                prop.load(new FileInputStream(file));
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (prop.getProperty("installation_dir") != null)
                workingDirectory = new File(prop.getProperty("installation_dir"));
            else
                workingDirectory = changeDirInternal(frame, defaultWorkingDir, prop, file);
        }

        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        return workingDirectory;
    }

    public static File changeDir(JFrame frame, File currentWorkingDir) {
        Properties prop = new Properties();
        File file = new File(fileLocation);
        return changeDirInternal(frame, currentWorkingDir, prop, file);
    }

    private static File changeDirInternal(JFrame frame, File currentWorkingDir, Properties prop, File settingFile) {
        File workingDirectory = new File(currentWorkingDir.getAbsolutePath());

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(1);
        fileChooser.setDialogTitle("Choose your Minecraft installation directory : (Cancelling will choose the default one)");
        int ret = fileChooser.showOpenDialog(frame);
        if (ret == 0) {
            File dir = fileChooser.getSelectedFile();
            prop.setProperty("installation_dir", dir.getAbsolutePath());
            workingDirectory = dir;
        } else if (ret == 1) {
            prop.setProperty("installation_dir", workingDirectory.getAbsolutePath());
        }
        try {
            prop.store(new FileOutputStream(settingFile), "MCFreeLauncher");
        } catch (IOException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        JOptionPane.showMessageDialog(frame, "Minecraft will be installed in " + workingDirectory.getAbsolutePath() + ".\n You can change it by pressing the \"Install folder\" button next to the \"play\" button.\n Note that when you change the installation folder, the content of the previous folder won't be deleted.");
        return workingDirectory;
    }
}
