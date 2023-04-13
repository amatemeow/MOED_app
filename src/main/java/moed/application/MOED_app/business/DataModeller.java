package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.ENUM.RotationType;
import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.components.AppConfig;
import moed.application.MOED_app.components.Charts;
import moed.application.MOED_app.utils.MyComplex;
import moed.application.MOED_app.utils.SelfRandom;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.math3.complex.Complex;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

@Component
public class DataModeller implements DisposableBean {
    private static final AppConfig.SampleData sampleData = new AppConfig.SampleData();
    public static String getTrend(Trend trend, Integer[] ratio) throws IOException {
        JFreeChart chart = Charts.getLineChart(
                trend.getChartName(),
                trend.getXAxisName(),
                trend.getYAxisName(),
                trend.getSeries()
        );
        return IOC.saveChartAsPNG(chart, ratio);
    }

    public static JFreeChart getChart(Trend trend, Integer[] ration) {
        return Charts.getLineChart(
                trend.getChartName(),
                trend.getXAxisName(),
                trend.getYAxisName(),
                trend.getSeries()
        );
    }

    public static XYSeries getModel(ArrayList<Number> points, Double... axisDividers) {
        if (points == null) {
            throw new RuntimeException("Got null input!");
        }
        Double xDivider = axisDividers.length != 0 ? axisDividers[0] : 1;
        XYSeries result = new XYSeries("");
        int N = points.size();
        for (int i = 0; i < N; i++) {
            result.add(i * xDivider, points.get(i));
        }
        return result;
    }

    public static XYSeries getStraight(int N, Double k, Double b, boolean direction) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < N; i++) {
            result.add(i, (direction ? 1 : -1) * k * i * b);
        }
        return result;
    }

    public static XYSeries getExponent(int N, Double k, Double b, Double alpha, boolean direction) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < N; i++) {
            Double yValue = b * Math.exp((direction ? 1 : -1) * alpha * k * i);
            if (yValue == Double.POSITIVE_INFINITY) { break; }
            result.add(i, yValue);
        }
        return result;
    }

    public static XYSeries getNoise(int N, double R, RandomType rndType) {
        XYSeries result = new XYSeries("");
        Double[] data = getNoiseOptimized(N, R, rndType);
        for (int i = 0; i < N; i++) {
            result.add(i, data[i]);
        }
        return result;
    }

    public static Double[] getNoiseOptimized(int N, double R, RandomType rndType) {
        Double[] data = new Double[N];
        Random rnd;
        if (rndType == RandomType.SELF) {
            rnd = new SelfRandom();
        } else {
            rnd = new Random();
        }
        double Xmax = 0.0;
        double Xmin = 1.0;
        for (int i = 0; i < N; i++) {
            data[i] = rnd.nextDouble();
            if (data[i] > Xmax) { Xmax = data[i]; }
            if (data[i] < Xmin) { Xmin = data[i]; }
        }
        if (N == 1) {
            data[0] *= R;
        } else {
            for (int i = 0; i < N; i++) {
                data[i] = ((data[i] - Xmin)/(Xmax - Xmin) - 0.5) * 2 * R;
            }
        }
        return data;
    }

    //Изменить реализацию на series
    public static XYSeries getShiftedY(XYSeries series, Double shift, int... splitIndexes) {
        XYSeries result = new XYSeries("");
        int startIdx = 0;
        int endIdx = series.getItemCount();
        if (splitIndexes.length == 2) {
            startIdx = splitIndexes[0];
            endIdx = splitIndexes[1] + 1;
        }
        for (int i = startIdx; i < endIdx; i++) {
            result.add(series.getX(i), (Double) series.getY(i) + shift);
        }
        return result;
    }

    public static XYSeries getShiftedX(XYSeries series, Double shift) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < series.getItemCount(); i++) {
            result.add((Double) series.getX(i) + shift, series.getY(i));
        }
        return result;
    }

    public static XYSeries getImpulseNoise(int N, Double noiseRange, Double percentile, Double noiseSubRange) {
        XYSeries result = new XYSeries("");
        Double[] randomData = new Double[N];
        for (int i = 0; i < N; i++) {
            randomData[i] = 0d;
        }
        int noiseCount = (int) (N * percentile);
        Integer[] xNoiseIndexes = new Integer[noiseCount];
        Random rnd = new Random();
        for (int i = 0; i < noiseCount; i++) {
            int newIdx;
            do {
                newIdx = Math.abs(rnd.nextInt(N));
            } while (arrayContains(xNoiseIndexes, newIdx));
            xNoiseIndexes[i] = newIdx;
        }
        XYSeries subNoiseValues = getNoise(noiseCount, noiseSubRange, RandomType.SYSTEM);
        Double[] noiseValues = new Double[noiseCount];
        for (int i = 0; i < noiseCount; i++) {
            noiseValues[i] = (Math.abs((Double) subNoiseValues.getY(i)) + noiseRange * 5) * (rnd.nextBoolean() ? 1 : -1);
        }
        for (int i = 0; i < noiseCount; i++) {
            randomData[xNoiseIndexes[i]] = noiseValues[i];
        }
        for (int i = 0; i < N; i++) {
            result.add(i, randomData[i]);
        }
        return result;
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

    //Аддитивная модель
    public static XYSeries getAddition(XYSeries... series) {
        XYSeries result = new XYSeries("");
        int count = Arrays.stream(series).min(Comparator.comparingInt(XYSeries::getItemCount)).orElse(result).getItemCount();
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum = 0;
            for (var ser : series) {
                sum += ser.getY(i).doubleValue();
            }
            result.add(series[0].getX(i), sum);
        }
        return result;
    }

    public static XYSeries getMultiplied(XYSeries series1, XYSeries series2) {
        XYSeries result = new XYSeries("");
        int count = Math.min(series1.getItemCount(), series2.getItemCount());
        for (int i = 0; i < count; i++) {
            result.add(series1.getX(i), (Double) series1.getY(i) * (Double) series2.getY(i));
        }
        return result;
    }

    public static XYSeries getMerged(XYSeries series1, XYSeries series2) {
        XYSeries result = new XYSeries("");
        Double i = 0d;
        for (var item : series1.getItems()) {
            result.add(i, ((XYDataItem) item).getY());
            i++;
        }
        for (var item : series2.getItems()) {
            result.add(i, ((XYDataItem) item).getY());
            i++;
        }
        return result;
    }

    public static XYSeries removeTrend(XYSeries trend, XYSeries series) {
        XYSeries result = new XYSeries("");
        int count = Math.min(trend.getItemCount(), series.getItemCount());
        for (int i = 0; i < count; i++) {
            result.add(series.getX(i), (Double) series.getY(i) - (Double) trend.getY(i));
        }
        return result;
    }

    public static MyComplex[] fourierComplexes(XYSeries series, boolean normalize, boolean inverse) {
        int N = series.getItemCount();
        MyComplex[] result = new MyComplex[N];
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                double base = (2d * Math.PI * i * j) / (double) N;
                Re += series.getY(j).doubleValue() * Math.cos(base);
                Im += series.getY(j).doubleValue() * Math.sin(base);
            }
            if (normalize) {
                if (inverse) {
                    Re *= N;
                    Im *= N;
                } else {
                    Re /= N;
                    Im /= N;
                }
            }
            result[i] = new MyComplex(Re, Im);
        }
        return result;
    }

    public static MyComplex[] fourierComplexes(Number[] data, boolean normalize, boolean inverse) {
        int N = data.length;
        MyComplex[] result = new MyComplex[N];
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                double base = (2d * Math.PI * i * j) / (double) N;
                Re += data[j].doubleValue() * Math.cos(base);
                Im += data[j].doubleValue() * Math.sin(base);
            }
            if (normalize) {
                if (inverse) {
                    Re *= N;
                    Im *= N;
                } else {
                    Re /= N;
                    Im /= N;
                }
            }
            result[i] = new MyComplex(Re, Im);
        }
        return result;
    }

    public static XYSeries fourier(XYSeries series, boolean complex, boolean ermit, int... windowSize) {
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
        N = windowedSeries.getItemCount();
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += (Double) windowedSeries.getY(j) * Math.cos(2d*Math.PI*i*j/N);
                Im += (Double) windowedSeries.getY(j) * Math.sin(2d*Math.PI*i*j/N);
            }
            Re /= N;
            Im /= N;
            result.add(windowedSeries.getX(i), complex ? (ermit ? Re - Im : Re + Im) : Math.sqrt(Math.pow(Re, 2) + Math.pow(Im, 2)));
        }
        return result;
    }

    public static XYSeries fourier(Number[] data, boolean complex, boolean ermit, int... windowSize) {
        int N = data.length;
        Number[] resultData;
        if (windowSize.length != 0) {
            resultData = new Number[N + windowSize[0]];
            for (int i = 0; i < N; i++) {
                resultData[i] = data[i];
            }
            N = resultData.length;
        } else {
            resultData = data;
        }
        XYSeries result = new XYSeries("");
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += resultData[j].doubleValue() * Math.cos(2d*Math.PI*i*j/N);
                Im += resultData[j].doubleValue() * Math.sin(2d*Math.PI*i*j/N);
            }
            Re /= N;
            Im /= N;
            result.add(resultData[i], complex ? (ermit ? Re - Im : Re + Im) : Math.sqrt(Math.pow(Re, 2) + Math.pow(Im, 2)));
        }
        return result;
    }

    public static Number[] fourierNum(Number[] data, boolean complex, boolean normalize, boolean inverse, int... windowSize) {
        int N = data.length;
        Number[] resultData;
        if (windowSize.length != 0) {
            resultData = new Number[N + windowSize[0]];
            for (int i = 0; i < N; i++) {
                resultData[i] = data[i];
            }
            N = resultData.length;
        } else {
            resultData = data;
        }
        Number[] result = new Number[N];
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += resultData[j].doubleValue() * Math.cos(2d*Math.PI*i*j/N);
                Im += resultData[j].doubleValue() * Math.sin(2d*Math.PI*i*j/N);
            }
            if (normalize) {
                if (inverse) {
                    Re *= N;
                    Im *= N;
                } else {
                    Re /= N;
                    Im /= N;
                }
            }
            result[i] = complex ? Re + Im : Math.sqrt(Math.pow(Re, 2) + Math.pow(Im, 2));
        }
        return result;
    }

    public static XYSeries inverseFourier(XYSeries series, int... windowSize) {
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
        N = windowedSeries.getItemCount();
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += windowedSeries.getY(j).doubleValue() * Math.cos(2d*Math.PI*i*j/N);
                Im += windowedSeries.getY(j).doubleValue() * Math.sin(2d*Math.PI*i*j/N);
            }
            result.add(windowedSeries.getX(i), Re * N + Im * N);
        }
        return result;
    }

    public static Number[] inverseFourier(Number[] data) {
        int N = data.length;
        Number[] result = new Number[N];
        for (int i = 0; i < N; i++) {
            double Re = 0d;
            double Im = 0d;
            for (int j = 0; j < N; j++) {
                Re += data[j].doubleValue() * Math.cos(2d*Math.PI*i*j/N);
                Im += data[j].doubleValue() * Math.sin(2d*Math.PI*i*j/N);
            }
            result[i] = Re + Im;
        }
        return result;
    }

    public static Number[][] fourier2D(Number[][] data, boolean complex, boolean normalize, boolean inverse) {
        int N = data.length;
        int M = data[0].length;
        Number[][] resultData = new Number[N][M];
        for (int i = 0; i < N; i++) {
            resultData[i] = fourierNum(data[i], complex, normalize, inverse);
        }
        resultData = DataProcessor.rotate(resultData, RotationType.LEFT);
        for (int i = 0; i < M; i++) {
            resultData[i] = fourierNum(resultData[i], complex, normalize, inverse);
        }
        resultData = DataProcessor.rotate(resultData, RotationType.RIGHT);
        return resultData;
    }

    public static Number[][] inverseFourier2D(Number[][] data, boolean normalize, boolean inverse) {
        int N = data.length;
        int M = data[0].length;
        var resultData = DataProcessor.rotate(data, RotationType.LEFT);
        for (int i = 0; i < M; i++) {
            resultData[i] = DataProcessor.spectrumFourierNum(fourierComplexes(resultData[i], normalize, inverse), true, false);
        }
        resultData = DataProcessor.rotate(resultData, RotationType.RIGHT);
        for (int i = 0; i < N; i++) {
            resultData[i] = DataProcessor.spectrumFourierNum(fourierComplexes(resultData[i], normalize, inverse), true, false);
        }
        return resultData;
    }

    //window looks like: { x1, x2, y }
    public static XYSeries squareFilter(int N, Double shift, Double[]... windows) {
        XYSeries result = getStraight(N, 0d, 0d, true);
        result = getShiftedX(result, shift);
        for (int i = 0; i < windows.length; i++) {
            for (Double j = windows[i][0]; j <= windows[i][1]; j++) {
                result.update(j, windows[i][2]);
            }
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
