package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.components.AppConfig;
import moed.application.MOED_app.components.Charts;
import moed.application.MOED_app.utils.SelfRandom;
import org.apache.commons.io.file.PathUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class DataModeller implements DisposableBean {
    private static final AppConfig.SampleData sampleData = new AppConfig.SampleData();
    public static String getTrend(Trend trend, Integer[] ratio) throws IOException {
        JFreeChart chart = null;
        switch (trend.getChartType()) {
            case LINE:
                chart = Charts.getLineChart(
                        trend.getChartName(),
                        trend.getXAxisName(),
                        trend.getYAxisName(),
                        trend.getSeries()
                );
                break;
        }
        return IOC.saveChartAsPNG(chart, ratio);
    }

    public static Double[] getNoise(int N, double R, RandomType rndType) {
        Double[] data = new Double[N];
        Random rnd;
        switch (rndType) {
            case SELF:
                rnd = new SelfRandom();
                break;
            default:
                rnd = new Random();
                break;

        }
        double Xmax = 0.0;
        double Xmin = 1.0;
        for (int i = 0; i < data.length; i++) {
            data[i] = rnd.nextDouble();
            if (data[i] > Xmax) { Xmax = data[i]; }
            if (data[i] < Xmin) { Xmin = data[i]; }
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = ((data[i] - Xmin)/(Xmax - Xmin) - 0.5) * 2 * R;
        }
        return data;
    }

    public static Trend getShiftedTrend(Trend trend, Double shift, int... splitIndexes) {
        XYSeries series = trend.getSeries();
        int startIdx = 0;
        int endIdx = series.getItemCount();
        if (splitIndexes.length == 2) {
            startIdx = splitIndexes[0];
            endIdx = splitIndexes[1] + 1;
        }
        for (int i = startIdx; i < endIdx; i++) {
            series.updateByIndex(i, (Double) series.getY(i) + shift);
        }
        trend.setSeries(series);
        return trend;
    }

    public static Double[] getImpulseNoise(int N, Double noiseRange, Double percentile, Double noiseSubRange) {
        Double[] randomData = new Double[N];
        for (int i = 0; i < N; i++) {
            randomData[i] = 0d;
        }
        int noiseCount = (int) (N * percentile);
        Integer[] xNoiseIndexes = new Integer[noiseCount];
        Random rnd = new Random();
        for (int i = 0; i < noiseCount; i++) {
            int newIdx = 0;
            do {
                newIdx = Math.abs(rnd.nextInt(N));
            } while (arrayContains(xNoiseIndexes, newIdx));
            xNoiseIndexes[i] = newIdx;
        }
        Double[] subNoiseValues = getNoise(noiseCount, noiseSubRange, RandomType.SYSTEM);
        Double[] noiseValues = new Double[noiseCount];
        for (int i = 0; i < noiseCount; i++) {
            noiseValues[i] = (Math.abs(subNoiseValues[i]) + noiseRange * 5) * (rnd.nextBoolean() ? 1 : -1);
        }
        for (int i = 0; i < noiseCount; i++) {
            randomData[xNoiseIndexes[i]] = noiseValues[i];
        }
        return randomData;
    }

    public static XYSeries getHarm(int N, int A0, int f0, double deltaT) {
        XYSeries series = new XYSeries("");
        for (double num = 0; num < N; num++) {
            series.add(num, A0 * Math.sin(2 * Math.PI * f0 * num * deltaT));
        }
        return series;
    }
    public static XYSeries getPolyHarm(int N, int[] Ai, int[] fi, double deltaT) {
        XYSeries series = new XYSeries("");
        for (double num = 0; num < N; num++) {
            double yValue = 0;
            for (int val = 0; val < Ai.length; val++) {
                yValue += Ai[val] * Math.sin(2 * Math.PI * fi[val] * num * deltaT);
            }
            series.add(num, yValue);
        }
        return series;
    }

    //Соединение наборов данных
    public static XYSeries getMerged(XYSeries series1, XYSeries series2) {
        XYSeries result = new XYSeries("");
        int count = Math.min(series1.getItemCount(), series2.getItemCount());
        for (int i = 0; i < count; i++) {
            result.add(i, (Double) series1.getY(i) + (Double) series2.getY(i));
        }
        return result;
    }

    public static XYSeries removeTrend(XYSeries trend, XYSeries series) {
        XYSeries result = new XYSeries("");
        int count = Math.min(trend.getItemCount(), series.getItemCount());
        for (int i = 0; i < count; i++) {
            result.add(i, (Double) series.getY(i) - (Double) trend.getY(i));
        }
        return result;
    }

    public static XYSeries fourier(XYSeries series, int... windowSize) {
        int N = series.getItemCount();
        XYSeries windowedSeries = new XYSeries("");
        try {
           windowedSeries = series.createCopy(0, N - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (windowSize.length != 0) {
            for (int i = N; i < N + windowSize[0]; i++) {
                windowedSeries.add(i, 0);
            }
        }
        XYSeries result = new XYSeries("");
        ArrayList<Double> listRe = new ArrayList<>();
        ArrayList<Double> listIm = new ArrayList<>();
        N = windowedSeries.getItemCount();
        for (int i = 0; i < N; i++) {
            Double Re = 0d;
            Double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += (Double) windowedSeries.getY(j) * Math.cos(2*Math.PI*i*j/N);
                Im += (Double) windowedSeries.getY(j) * Math.sin(2*Math.PI*i*j/N);
            }
            listRe.add(Re/N);
            listIm.add(Im/N);
            result.add(i, Math.sqrt(Math.pow(Re, 2) + Math.pow(Im, 2)));
        }
        return result;
    }

    private static <T> boolean arrayContains(T[] arr, T value) {
        for (T item : arr) {
            if (item == value) return true;
        }
        return false;
    }

    public void destroy() throws IOException {
        PathUtils.cleanDirectory(IOC.getCleanPath());
    }
}
