package io.github.superteam.resonance.lwjgl3;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.function.Consumer;

/** Native desktop file picker used by model debug tooling. */
public final class DesktopFilePicker {

    private static File lastDirectory;

    private DesktopFilePicker() {
    }

    public static void pickModelFile(Consumer<File> onPicked) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep default look and feel when system look and feel is unavailable.
            }

            JFileChooser chooser = new JFileChooser(lastDirectory);
            chooser.setDialogTitle("Open 3D Model");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "3D Models (*.gltf, *.glb, *.g3dj, *.g3db)",
                "gltf", "glb", "g3dj", "g3db"
            ));
            chooser.setAcceptAllFileFilterUsed(true);

            JFrame hiddenParent = new JFrame();
            hiddenParent.setUndecorated(true);
            hiddenParent.setAlwaysOnTop(true);
            hiddenParent.setVisible(true);
            hiddenParent.toFront();

            int result = chooser.showOpenDialog(hiddenParent);
            hiddenParent.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                lastDirectory = selected == null ? lastDirectory : selected.getParentFile();
                onPicked.accept(selected);
                return;
            }

            onPicked.accept(null);
        });
    }
}
