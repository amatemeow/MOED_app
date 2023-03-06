package moed.application.MOED_app.Entities;

import lombok.Getter;
import lombok.Setter;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.business.DataProcessor;
import moed.application.MOED_app.business.IOC;
import moed.application.MOED_app.components.AppConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageInfo {
    @Getter
    private String name;
    @Getter
    private String imagePath;
    private String histPath;
    @Getter
    private final BufferedImage image;
    @Getter
    private Integer[][] matrix;
    @Setter
    private boolean showHist = false;

    public ImageInfo(String path) {
        this.name = path.replace(".jpg", "");
        this.image = IOC.readImg(path);
        this.imagePath = createFile(path);
        this.matrix = IOC.readImgData(path);
    }

    public ImageInfo(String path, Integer[][] data) {
        this.name = path.replace(".jpg", "");
        this.matrix = data;
        this.image = buildImage();
        this.imagePath = createFile(path);
    }

    public ImageInfo(String path, Integer[][] data, boolean showHist) {
        this.name = path.replace(".jpg", "");
        this.matrix = data;
        this.showHist = showHist;
        if (showHist) {
            buildHist();
            this.image = null;
        } else {
            this.image = buildImage();
            this.imagePath = createFile(path);
        }
    }

    private String createFile(String path) {
        File output = new File(AppConfig.IOCConfig.CHART_FOLDER_WEB + "/" + path);
        try {
            ImageIO.write(this.image, "jpg", output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output.getPath();
    }

    private BufferedImage buildImage() {
        BufferedImage img = new BufferedImage(this.matrix.length, this.matrix[0].length, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        int[] pixels = new int[this.matrix.length * this.matrix[0].length];
        int idx = 0;
        for (int i = 0; i < this.matrix[0].length; i++) {
            for (int j = 0; j < this.matrix.length; j++, idx++) {
             pixels[idx] = this.matrix[j][i];
            }
        }
        raster.setPixels(0, 0, this.matrix.length, this.matrix[0].length, pixels);
        img.setData(raster);
        return img;
    }

    private void buildHist() {
        try {
            histPath = DataModeller.getTrend(
                    new Trend("Density").setSeries(DataAnalyzer.Statistics.getProbDen(
                            DataProcessor.toIntVector(this.getMatrix())
                    )),
                    new Integer[] {720, 480}
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String getImagePath() {
        return showHist ? histPath : imagePath;
    }
}
