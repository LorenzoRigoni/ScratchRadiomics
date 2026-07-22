package com.ds4h.scratchradiomics;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menu = {
        @Menu(label = "Plugins"),
        @Menu(label = "ScratchRadiomics"),
        @Menu(label = "Run Analysis")
})
public class ScratchRadiomicsPlugin implements Command {

    @Parameter(label = "Input Image", description = "Select image from scratch assay")
    private ImagePlus currentImage;

    @Parameter(label = "Abilita Segmentazione Automatica SAM")
    private boolean useSam = true;

    @Parameter(label = "Soglia Binning IBSI", min = "8", max = "256")
    private int ibsiBinNumber = 32;

    @Override
    public void run() {
        if (currentImage == null) {
            IJ.error("ScratchRadiomics", "Apri prima un'immagine!");
            return;
        }

        IJ.log("=== Avvio ScratchRadiomics ===");
        IJ.log("Immagine: " + currentImage.getTitle());

        // 1. Step di Segmentazione (SAM -> ROI Manager)
        if (useSam) {
            IJ.log("Esecuzione segmentazione SAM...");
            runSamSegmentation();
        }

        // 2. Notifica all'utente per eventuale editing con ROI Manager
        RoiManager rm = RoiManager.getRoiManager();
        IJ.showMessage("Segmentazione Completata",
                "La maschera è stata caricata nel ROI Manager.\n" +
                        "Usa gli strumenti di Fiji per aggiungere, rimuovere o combinare le ROI se necessario,\n" +
                        "quindi premi OK per calcolare le feature IBSI e generare l'Excel.");

        // 3. Calcolo Feature IBSI sulle ROI definite dall'utente
        IJ.log("Calcolo feature radiomiche IBSI in corso...");
        calculateIbsiFeatures(rm);

        IJ.log("=== Processo Completato ===");
    }

    private void runSamSegmentation() {
        // TODO: Integrazione modulo ONNX per SAM
        // Per ora aggiungiamo una ROI fittizia al ROI Manager come dimostrazione
        RoiManager rm = RoiManager.getRoiManager();
        Roi demoRoi = new Roi(10, 10, currentImage.getWidth() - 20, currentImage.getHeight() - 20);
        rm.addRoi(demoRoi);
    }

    private void calculateIbsiFeatures(RoiManager rm) {
        // TODO: Invoco del modulo per estrazione IBSI ed esportazione Excel
    }

    // Main metod per avviare Fiji direttamente da IDE (Eclipse/IntelliJ/VSCode)
    public static void main(final String... args) {
        final net.imagej.ImageJ ij = new net.imagej.ImageJ();
        ij.ui().showUI();

        // Apri un'immagine di test
        ImagePlus imp = IJ.openImage("http://imagej.net/images/blobs.gif");
        if (imp != null) {
            imp.show();
        }

        // Esegui il plugin
        ij.command().run(ScratchRadiomicsPlugin.class, true);
    }
}
