package moed.application.MOED_app.Entities;

import lombok.Getter;
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
    @Getter
    private final BufferedImage image;
    @Getter
    private Integer[][] matrix;
    @Getter
    private String textMatrix;

    public ImageInfo(String path) {
        this.name = path.replace(".jpg", "");
        this.image = IOC.readImg(path);
        this.imagePath = createFile(path);
        this.matrix = IOC.readImgData(path);
        this.textMatrix = matrixToString();
    }

    public ImageInfo(String path, Integer[][] data) {
        this.name = path.replace(".jpg", "");
        this.matrix = data;
        this.image = buildImage();
        this.imagePath = createFile(path);
        this.textMatrix = matrixToString();
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

    private String matrixToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
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
}
