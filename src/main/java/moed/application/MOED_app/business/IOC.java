package moed.application.MOED_app.business;

import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;

import moed.application.MOED_app.ENUM.FileType;
import moed.application.MOED_app.components.AppConfig;
import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@Component
public class IOC {

    public static String saveChartAsPNG(JFreeChart chart, Integer[] ratio) throws IOException {
        final String path = AppConfig.IOCConfig.CHART_FOLDER_WEB
                + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + ".png";
        ChartUtils.writeChartAsPNG(
                FileUtils.openOutputStream(new File(path)),
                chart,
                ratio[0],
                ratio[1]
        );
        return path;
    }

    public static ArrayList<Number> readDat(String path) {
        File file = new File(AppConfig.IOCConfig.DATAFILES_FOLDER + "/" + path);
        DataInputStream stream;
        ArrayList<Number> dataY = new ArrayList<>();
        try {
            stream = new DataInputStream(new FileInputStream(file));
            dataY = new ArrayList<>();
            byte bytes[] = stream.readAllBytes();
            stream.close();
            byte curr[] = new byte[4];
            for (int i = 0; i < bytes.length; i += 4) {
                curr[0] = bytes[i];
                curr[1] = bytes[i + 1];
                curr[2] = bytes[i + 2];
                curr[3] = bytes[i + 3];
                dataY.add(ByteBuffer.wrap(curr).order(ByteOrder.LITTLE_ENDIAN).getFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataY;
    }

    public static Number[][] imageWrapper(String path, int sizeX, int sizeY, FileType fileType) {
        Number[][] resultData = new Number[sizeX][sizeY];
        switch (fileType) {
            case DAT: 
                var data = readDat(path);
                int cnt = 0;
                for (int i = 0; i < sizeX; i++) {
                    for (int j = 0; j < sizeY; j++, cnt++) {
                        resultData[i][j] = data.get(cnt);
                    }
                }
            case JPG:
                break;
        }
        return resultData;
    }

    public static ArrayList<Number> readWav(String path) {
        File file = new File(AppConfig.IOCConfig.DATAFILES_FOLDER + "/" + path);
        ArrayList<Number> data = new ArrayList<>();
        try {
            WavFile wavFile = WavFile.openWavFile(file);
            int channels = wavFile.getNumChannels();
            long totalFrames = wavFile.getNumFrames();
            System.out.println(
                    "Sample rate: "
                    + wavFile.getSampleRate()
                    + "\nFrames total: "
                    + totalFrames
                    + "\nValid bits: "
                    + wavFile.getValidBits()
                    + "\nChannels: "
                    + channels
            );

            double[] buffer = new double[100 * channels];
            int framesRead;

            do {
                framesRead = wavFile.readFrames(buffer, 100);
                for (int i = 0; i < framesRead * channels; i++) {
                    data.add(buffer[i]);
                }
            } while (framesRead != 0);
            wavFile.close();
        } catch (IOException | WavFileException e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }

    public static boolean writeWav(int sampleRate, XYSeries data, String fileName) {
        int framesCount = data.getItemCount();
        try {
            WavFile wavFile = WavFile.newWavFile(
                    new File(AppConfig.IOCConfig.DATAFILES_FOLDER + "/" + fileName + ".wav"),
                    1,
                    framesCount,
                    16,
                    sampleRate
            );
            double[] buffer = new double[100];
            long counter = 0;
            while (counter < framesCount) {
                long remainingFrames = wavFile.getFramesRemaining();
                int framesToWrite = remainingFrames > 100 ? 100 : (int) remainingFrames;
                for (int i = 0; i < framesToWrite; i++, counter++) {
                    buffer[i] = data.getY((int) counter).doubleValue();
                }
                wavFile.writeFrames(buffer, framesToWrite);
            }
            wavFile.close();
        } catch (IOException | WavFileException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static BufferedImage readImg(String path) {
        try {
            return ImageIO.read(new File(AppConfig.IOCConfig.DATAFILES_FOLDER + "/" + path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    }

    public static Integer[][] readImgData(String path) {
        BufferedImage image = readImg(path);
        Integer[][] data = new Integer[image.getWidth()][image.getHeight()];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                data[i][j] = image.getRaster().getSample(i, j, 0);
            }
        }
        return data;
    }

    public static Integer[][] readRAW(String path, int off, int xSize, int ySize, int byteMultiplier) {
        byte[] buffer = new byte[0];
        try {
            BufferedInputStream stream = new BufferedInputStream(
                    new FileInputStream(AppConfig.IOCConfig.DATAFILES_FOLDER + "/" + path));
            stream.skip(off);
            buffer = stream.readNBytes(byteMultiplier * (xSize * ySize));
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer[][] data = new Integer[xSize][ySize];
        int idx = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++, idx+=2) {
                data[i][j] = (buffer[idx] & 0xFF) << 8 | buffer[idx + 1] & 0xFF;
            }
        }
        return data;
    }

    public static String writeTextMatrix(Number[][] arr, String path) {
        File file = new File(AppConfig.IOCConfig.CHART_FOLDER_WEB + "/" + path);
        try {
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < arr.length; i++) {
                writer.println(Arrays.deepToString(arr[i]));
            }
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return file.getPath();
    }

    public static Path getCleanPath() {
        return Path.of(AppConfig.IOCConfig.CLEAN_PATH);
    }
}
