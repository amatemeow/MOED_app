package moed.application.MOED_app.business;

import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;
import moed.application.MOED_app.components.AppConfig;
import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
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
        ArrayList<Number> dataY = null;
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
                dataY.add((double) ByteBuffer.wrap(curr).order(ByteOrder.LITTLE_ENDIAN).getFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataY;
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

    public static Path getCleanPath() {
        return Path.of(AppConfig.IOCConfig.CLEAN_PATH);
    }
}
