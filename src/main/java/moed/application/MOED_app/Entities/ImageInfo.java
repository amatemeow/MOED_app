package moed.application.MOED_app.Entities;

import lombok.Getter;
import lombok.Setter;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.business.DataProcessor;
import moed.application.MOED_app.business.IOC;
import moed.application.MOED_app.components.AppConfig;
import org.jfree.data.xy.XYSeries;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageInfo {
    @Getter
    private String name;
    private String imagePath;
    private String trendPath;
    @Getter
    private final BufferedImage image;
    @Getter
    private Integer[][] matrix;
    @Getter
    private Number[][] numMatrix;
    @Setter
    private boolean isTrend = false;
    @Getter
    private boolean isBreak = false;

    public ImageInfo() {
        this.image = null;
        this.isBreak = true;
    }

    public ImageInfo(String path) {
        this.name = path.replace(".jpg", "");
        this.image = IOC.readImg(path);
        this.imagePath = createFile(path);
        this.matrix = IOC.readImgData(path);
        this.numMatrix = Arrays.stream(this.matrix)
            .map(v -> Arrays.stream(v)
                .toArray(Number[]::new))
            .toArray(Number[][]::new);
    }

    public ImageInfo(String path, Integer[][] data) {
        this.name = path.replace(".jpg", "");
        this.matrix = data;
        this.numMatrix = Arrays.stream(this.matrix)
            .map(v -> Arrays.stream(v)
                .toArray(Number[]::new))
            .toArray(Number[][]::new);
        this.image = buildImage();
        this.imagePath = createFile(path);
    }

    public ImageInfo(String path, Number[][] data) {
        this.name = path.replace(".jpg", "");
        this.numMatrix = data;
        this.matrix = DataProcessor.Num2Int(data);
        this.image = buildImageNum();
        this.imagePath = createFile(path);
    }

    public ImageInfo(String path, Integer[][] data, boolean showHist) {
        this.name = path.replace(".jpg", "");
        this.matrix = data;
        this.numMatrix = Arrays.stream(this.matrix)
            .map(v -> Arrays.stream(v)
                .toArray(Number[]::new))
            .toArray(Number[][]::new);
        this.isTrend = showHist;
        if (this.isTrend) {
            buildHist();
            this.image = null;
        } else {
            this.image = buildImage();
            this.imagePath = createFile(path);
        }
    }

    public ImageInfo(String path, XYSeries series, boolean isTrend) {
        this.name = path.replace(".jpg", "");
        this.isTrend = isTrend;
        if (this.isTrend) {
            buildTrend(series, path);
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

    private BufferedImage buildImageNum() {
        int N = this.numMatrix.length;
        int M = this.numMatrix[0].length;
        BufferedImage img = new BufferedImage(N, M, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        double[] pixels = new double[N * M];
        int idx = 0;
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++, idx++) {
             pixels[idx] = this.numMatrix[j][i].doubleValue();
            }
        }
        raster.setPixels(0, 0, N, M, pixels);
        img.setData(raster);
        return img;
    }

    private void buildHist() {
        try {
            trendPath = DataModeller.getTrend(
                    new Trend("Density").setSeries(DataAnalyzer.Statistics.getProbDen(
                            DataProcessor.toIntVector(this.getMatrix())
                    )),
                    new Integer[] {720, 480}
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void buildTrend(XYSeries series, String name) {
        try {
            trendPath = DataModeller.getTrend(
                    new Trend(name).setSeries(series),
                    new Integer[] {720, 480}
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String getImagePath() {
        return isTrend ? trendPath : imagePath;
    }
}
