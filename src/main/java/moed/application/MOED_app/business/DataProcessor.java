package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.ImgFIlterType;
import moed.application.MOED_app.ENUM.InterpolationType;
import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.ENUM.RotationType;
import moed.application.MOED_app.Entities.ImageInfo;
import moed.application.MOED_app.business.DataAnalyzer.Statistics;

import moed.application.MOED_app.components.LineChartBox;
import moed.application.MOED_app.utils.MyComplex;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.core.subst.Token.Type;

import javax.swing.*;

import java.rmi.dgc.VMID;
import java.util.*;
import java.util.function.Supplier;

public class DataProcessor {

    public static class Filtering {
        ///Initial frequencies filter
        public static XYSeries LPF(Double f, Double dt, int m) {
            XYSeries ipw = new XYSeries("");
            final Double[] edgePoints = new Double[] {0.35577019, 0.24336983, 0.07211497, 0.00630165};
            double fact = f * 2 * dt;
            ipw.add(0, fact);
            double arg = fact * Math.PI;
            for (int i = 1; i <= m; i++) {
                ipw.add(i, Math.sin(arg * i) / (Math.PI * i));
            }
            ipw.updateByIndex(m, ipw.getY(m).doubleValue() / 2);
            double sumg = ipw.getY(0).doubleValue();
            for (int i = 1; i <= m; i++) {
                double tmpsum = edgePoints[0];
                arg = Math.PI * i / m;
                for (int k = 1; k <= 3; k++) {
                    tmpsum += 2 * edgePoints[k] * Math.cos(arg * k);
                }
                ipw.updateByIndex(i, ipw.getY(i).doubleValue() * tmpsum);
                sumg += 2 * ipw.getY(i).doubleValue();
            }
            for (int i = 0; i <= m; i++) {
                ipw.updateByIndex(i, ipw.getY(i).doubleValue() / sumg);
            }
            XYSeries additional = null;
            try {
                additional = ipw.createCopy(1, m);
            } catch ( Exception e) {
                e.printStackTrace();
            }
            reverseSeries(additional);
            appendSeries(additional, ipw);
            return additional;
        }

        public static XYSeries HPF(Double f, Double dt, int m) {
            XYSeries hpw = new XYSeries("");
            XYSeries ipw = LPF(f, dt, m);
            int looper = 2 * m + 1;
            for (int k = 0; k < looper; k++) {
                if (k == m) {
                    hpw.add(k, 1 - ipw.getY(k).doubleValue());
                } else {
                    hpw.add(k, -ipw.getY(k).doubleValue());
                }
            }
            return hpw;
        }

        public static XYSeries BPF(Double f1, Double f2, Double dt, int m) {
            XYSeries bpw = new XYSeries("");
            XYSeries ipw1 = LPF(f1, dt, m);
            XYSeries ipw2 = LPF(f2, dt, m);
            int looper = 2 * m + 1;
            for (int k = 0; k < looper; k++) {
                bpw.add(k, ipw2.getY(k).doubleValue() - ipw1.getY(k).doubleValue());
            }
            return bpw;
        }

        public static XYSeries BSF(Double f1, Double f2, Double dt, int m) {
            XYSeries bsw = new XYSeries("");
            XYSeries ipw1 = LPF(f1, dt, m);
            XYSeries ipw2 = LPF(f2, dt, m);
            int looper = 2 * m + 1;
            for (int k = 0; k < looper; k++) {
                if (k == m) {
                    bsw.add(k, 1 + ipw1.getY(k).doubleValue() - ipw2.getY(k).doubleValue());
                } else {
                    bsw.add(k, ipw1.getY(k).doubleValue() - ipw2.getY(k).doubleValue());
                }
            }
            return bsw;
        }

        public static Integer[] BSFInt(Double f1, Double f2, Double dt, int m) {
            XYSeries ipw1 = LPF(f1, dt, m);
            XYSeries ipw2 = LPF(f2, dt, m);
            int looper = 2 * m + 1;
            Integer[] bsw = new Integer[looper];
            for (int k = 0; k < looper; k++) {
                if (k == m) {
                    bsw[k] = (int) (1 + ipw1.getY(k).doubleValue() - ipw2.getY(k).doubleValue());
                } else {
                    bsw[k] = (int) (ipw1.getY(k).doubleValue() - ipw2.getY(k).doubleValue());
                }
            }
            return bsw;
        }

        public static Integer[][] filterImg(Integer[][] data, int maskSizeX, int maskSizeY, ImgFIlterType filterType) {
            int M = data.length;
            int N = data[0].length;
            Integer[][] filtered = Arrays.stream(data).map(Integer[]::clone).toArray(Integer[][]::new);
            switch (filterType) {
                case ARIFMETHIC_MEAN_FILTER:
                    for (int i = 0; i < M; i++) {
                        for (int j = 0; j < N; j++) {
                            filtered[i][j] = arifMask(filtered, i, j, maskSizeX, maskSizeY);
                        }
                    }
                    break;
                case MEDIAN_FILTER:
                    for (int i = 0; i < M; i++) {
                        for (int j = 0; j < N; j++) {
                            filtered[i][j] = medianMask(filtered, i, j, maskSizeX, maskSizeY);
                        }
                    }
                    break;
            }
            return filtered;
        }

        private static int arifMask(Integer[][] data, int pX, int pY, int maskSizeX, int maskSizeY) {
            int sum = 0;
            int minusCount = 0;
            for (int i = -maskSizeX / 2; i <= maskSizeX / 2; i++) {
                for (int j = -maskSizeY / 2; j <= maskSizeY / 2; j++) {
                    if (i == 0 && j == 0) continue;
                    try {
                        int value = Optional.ofNullable(data[pX + i][pY + j]).orElse(0);
                        if (value > 255) {
                            value = 255;
                        } else if (value < 0) {
                            value = 0;
                        }
                        sum += value;
                    } catch(IndexOutOfBoundsException e) {
                        minusCount++;
                    }
                }
            }
            int result = (int) ((double) sum / (maskSizeX * maskSizeY - minusCount));
            if (result > 255) {
                result = 255;
            } else if (result < 0) {
                result = 0;
            }
            return result;
        }

        private static int medianMask(Integer[][] data, int pX, int pY, int maskSizeX, int maskSizeY) {
            ArrayList<Integer> arr = new ArrayList<>();
            for (int i = -maskSizeX / 2; i <= maskSizeX / 2; i++) {
                for (int j = -maskSizeY / 2; j <= maskSizeY / 2; j++) {
                    if (i == 0 && j == 0) continue;
                    try {
                        arr.add(Optional.ofNullable(data[pX + i][pY + j]).orElse(0));
                    } catch(IndexOutOfBoundsException e) {}
                }
            }
            arr.sort(Integer::compareTo);
            int size = arr.size();
            int result = size % 2 == 0 ? (arr.get(size / 2 - 1) + arr.get(size / 2)) / 2 : arr.get(size / 2);
            if (result > 255) {
                result = 255;
            } else if (result < 0) {
                result = 0;
            }
            return result;
        }

        public static Number[][] LPF2D(Number[][] data, double f0, double dt, int m) {
            double f = f0 * 0.5;
            int N = data.length;
            int M = data[0].length;
            Number[][] filtered = new Number[N][M];
            var filter = LPF(f, dt, m);
            for (int i = 0; i < N; i++) {
                filtered[i] = Arrays.copyOfRange(ConvolutionIntF(data[i], filter), m, m + M);
            }
            filtered = rotate(filtered, RotationType.LEFT);
            for (int i = 0; i < M; i++) {
                filtered[i] = Arrays.copyOfRange(ConvolutionIntF(filtered[i], filter), m, m + N);
            }
            filtered = rotate(filtered, RotationType.RIGHT);
            return filtered;
        }

        public static Number[][] HPF2D(Number[][] data, double f0, double dt, int m) {
            double f = f0 * 0.5;
            int N = data.length;
            int M = data[0].length;
            Number[][] filtered = new Number[N][M];
            var filter = HPF(f, dt, m);
            for (int i = 0; i < N; i++) {
                filtered[i] = Arrays.copyOfRange(ConvolutionIntF(data[i], filter), m, m + M);
            }
            filtered = rotate(filtered, RotationType.LEFT);
            for (int i = 0; i < M; i++) {
                filtered[i] = Arrays.copyOfRange(ConvolutionIntF(filtered[i], filter), m, m + N);
            }
            filtered = rotate(filtered, RotationType.RIGHT);
            return filtered;
        }
    }

    public static class DPMath {

        public static void zeros2D(Number[][] toZero) {
            for (int i = 0; i < toZero.length; i++) {
                for (int j = 0; j < toZero[0].length; j++) {
                    toZero[i][j] = 0d;
                }
            }
        }

        public static Number[][] erose2D(Number[][] data, int maskSize) {
            int N = data.length;
            int M = data[0].length;
            var erosed = new Number[N][M];
            int maskShift = maskSize / 2;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    var maskVals = new ArrayList<Double>();
                    for (int o = -maskShift; o <= maskShift; o++) {
                        for (int p = -maskShift; p <= maskShift; p++) {
                            try {
                                maskVals.add(data[i + o][j + p].doubleValue());
                            } catch(IndexOutOfBoundsException e) {}
                        }
                    }
                    erosed[i][j] = maskVals.stream().min(Double::compareTo).orElse(maskVals.get(0));
                }
            }
            return erosed;
        }

        public static Number[][] dilate2D(Number[][] data, int maskSize) {
            int N = data.length;
            int M = data[0].length;
            var dilated = new Number[N][M];
            int maskShift = maskSize / 2;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    var maskVals = new ArrayList<Double>();
                    for (int o = -maskShift; o <= maskShift; o++) {
                        for (int p = -maskShift; p <= maskShift; p++) {
                            try {
                                maskVals.add(data[i + o][j + p].doubleValue());
                            } catch(IndexOutOfBoundsException e) {}
                        }
                    }
                    dilated[i][j] = maskVals.stream().max(Double::compareTo).orElse(maskVals.get(0));
                }
            }
            return dilated;
        }

        public static double laplacianMask(Number[][] data, int x, int y, int[][] mask) {
            int N = data.length;
            int M = data[0].length;
            double x1 = x + 1 >= N ? 0 : data[x + 1][y].doubleValue() * mask[1][2];
            double x2 = x - 1 < 0 ? 0 : data[x - 1][y].doubleValue() * mask[1][0];
            double y1 = y + 1 >= M ? 0 : data[x][y + 1].doubleValue() * mask[2][1];
            double y2 = y - 1 < 0 ? 0 : data[x][y - 1].doubleValue() * mask[0][1];
            double result = x1 + x2 + y1 + y2 - data[x][y].doubleValue() * mask[1][1];
            if (mask[0][0] != 0) {
                double x1y1 = x + 1 >= N || y + 1 >= M ? 0 : data[x + 1][y + 1].doubleValue() * mask[2][2];
                double x1y2 = x + 1 >= N || y - 1 < 0 ? 0 : data[x + 1][y - 1].doubleValue() * mask[0][2];
                double x2y1 = x - 1 < 0 || y + 1 >= M ? 0 : data[x - 1][y + 1].doubleValue() * mask[2][0];
                double x2y2 = x - 1 < 0 || y - 1 < 0 ? 0 : data[x - 1][y - 1].doubleValue() * mask[0][0];
                result = x1 + x2 + y1 + y2 + x1y1 + x1y2 + x2y1 + x2y2 - 8 * data[x][y].doubleValue() * mask[1][1];
            }
            return result;
        }

        public static int[][] multiplyIntMask(int[][] mask, double multiplier) {
            int N = mask.length;
            int M = mask[0].length;
            var result = new int[N][M];
            for (int i = 0; i <N; i++) {
                for (int j = 0; j < M; j++) {
                    result[i][j] = (int) (mask[i][j] * multiplier);
                }
            }
            return result;
        }

        public static Number[][] multiplyNumMask(Number[][] mask, double multiplier) {
            int N = mask.length;
            int M = mask[0].length;
            var result = new Number[N][M];
            for (int i = 0; i <N; i++) {
                for (int j = 0; j < M; j++) {
                    result[i][j] = mask[i][j].doubleValue() * multiplier;
                }
            }
            return result;
        }
    }

    //Выделение изначальных трендов из смешанных данных с помощью производной
    public static XYSeries reverseMergeDerivative(XYSeries series) {
        XYSeries result = new XYSeries("");
        for (int i  = 0; i < series.getItemCount() - 1; i++) {
            result.add(series.getX(i), (Double) series.getY(i + 1) - (Double) series.getY(i));
        }
        return DataAnalyzer.getAntiShift(result);
    }

    //Выделение изначальных трендов из смешанных данных с помощью среднего
    public static XYSeries reverseMergeAverage(XYSeries series, int windowSize) {
        XYSeries result = new XYSeries("");
        int delta = series.getItemCount() / windowSize;
        int windowCount = series.getItemCount() / delta;
        for (int j = 0; j < series.getItemCount() - delta; j ++) {
            if (j >= series.getItemCount()) break;
            Double avg = 0d;
            try {
                avg = Statistics.getAverage(series.createCopy(j,
                        j + delta >= series.getItemCount() ? series.getItemCount() - 1 : j + delta));
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.add(j, avg);
        }
        XYSeries shifted = new XYSeries("");
        for (int j = series.getItemCount() - 1; j >= series.getItemCount() - delta - 1; j--) {
            Double avg = 0d;
            try {
                avg = Statistics.getAverage(series.createCopy(j - delta, j));
            } catch (Exception e) {
                e.printStackTrace();
            }
            shifted.add(j, avg);
        }
        Double shift = result.getY(result.getItemCount() - 1).doubleValue() - shifted.getY(0).doubleValue();
        for (var item : shifted.getItems()) {
            result.add(((XYDataItem) item).getX(), (Double) ((XYDataItem) item).getY() + shift);
        }
        return result;
    }

    public static XYSeries spectrumFourier(XYSeries series, Double dt, boolean complex, int... windowSize) {
        XYSeries result = new XYSeries("");
        int N = series.getItemCount();
        double rate = 1/dt;
        double df = rate/N;
        XYSeries spectrum = DataModeller.fourier(series, complex, false, windowSize);
        for (int i = 0; i < N/2; i++) {
            result.add(i * df, spectrum.getY(i));
        }
        return result;
    }

    public static XYSeries spectrumFourier(Integer[] data, Double dt, boolean complex, int... windowSize) {
        XYSeries result = new XYSeries("");
        int N = data.length;
        double rate = 1/dt;
        double df = rate/N;
        XYSeries spectrum = DataModeller.fourier(data, complex, false, windowSize);
        for (int i = 0; i < N/2; i++) {
            result.add(i * df, spectrum.getY(i));
        }
        return result;
    }
    public static Number[][] spectrumFourier2D(Number[][] data,  boolean complex, boolean normalize, boolean inverse) {
        Number[][] spectrum = DataModeller.fourier2D(data, complex, normalize, inverse);
        if (!complex) spectrum = Num2Double(cycleShift2D(spectrum, spectrum.length/2, spectrum[0].length/2, false));
        return spectrum;
    }

    public static Number[][] inverseFourier2D(Number[][] data, boolean normalize, boolean inverse) {
        return DataModeller.inverseFourier2D(data, normalize, inverse);
    }

    public static XYSeries antiNoise(int M, int R, XYSeries... series) {
        XYSeries result = new XYSeries("");
        XYSeries additive;
        int N = 1000;
        if (series.length != 0) {
            N = series[0].getItemCount();
        }
        ArrayList<Double[]> noises = new ArrayList<>();
        ArrayList<XYSeries> additives = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            if (series.length != 0) {
                XYSeries sysNoise = new XYSeries("");
                Double[] noiseVals = DataModeller.getNoiseOptimized(N, R, RandomType.SYSTEM);
                for (int j = 0; j < N; j++) {
                    sysNoise.add(j, noiseVals[j]);
                }
                additive = DataModeller.getAddition(series[0], sysNoise);
                additives.add(additive);
            } else {
                noises.add(DataModeller.getNoiseOptimized(N, R, RandomType.SYSTEM));
            }
        }
        for (int i = 0; i < N; i++) {
            Double value = 0.d;
            if (series.length != 0) {
                for (XYSeries merged : additives) {
                    value += (Double) merged.getY(i);
                }
            } else {
                for (var noise : noises) {
                    value += noise[i];
                }
            }
            result.add(i, value / M);
        }
        return  result;
    }

    public static XYSeries normalizeFunc(XYSeries series, Double multiplier) {
        XYSeries result = new XYSeries("");
        Double max = DataAnalyzer.Statistics.getMinMax(series)[1];
        for (int i = 0; i < series.getItemCount(); i++) {
            result.add(series.getX(i), ((Double) series.getY(i)) * multiplier / max);
        }
        return result;
    }

    public static Number[] normalizeData(Number[] data, Double multiplier) {
        int N = data.length;
        Number[] result = new Number[N];
        Double max = Arrays.stream(data).map(Number::doubleValue).max(Double::compareTo).get();
        Double min = Arrays.stream(data).map(Number::doubleValue).min(Double::compareTo).get();
        for (int i = 0; i < N; i++) {
            result[i] = (((data[i].doubleValue() - min) / (max - min)) * multiplier);
        }
        return result;
    }

    public static XYSeries amplify(XYSeries series, Double multiplier) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < series.getItemCount(); i++) {
            result.add(series.getX(i), ((Double) series.getY(i)) * multiplier);
        }
        return result;
    }

    public static XYSeries alterAxis(XYSeries series, Double xMultiplier, Double yMultiplier) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < series.getItemCount(); i++) {
            result.add(series.getX(i).doubleValue() * xMultiplier, series.getY(i).doubleValue() * yMultiplier);
        }
        return result;
    }

    public static XYSeries Convolution(XYSeries series1, XYSeries series2) {
        XYSeries result = new XYSeries("");
        int N = series2.getItemCount();
        int M = series1.getItemCount();
        for (int k = 0; k < N + M; k++) {
            Double val = 0d;
            for (int m = 0; m < M; m++) {
                if (k - m < 0 || k - m >= N) continue;
                val += (Double) series1.getY(m) * (Double) series2.getY(k - m);
            }
            result.add(k, val);
        }
        return result;
    }

    public static Number[] ConvolutionIntF(Number[] series1, XYSeries series2) {
        int N = series2.getItemCount();
        int M = series1.length;
        Number[] result = new Number[N + M];
        for (int k = 0; k < N + M; k++) {
            Double val = 0d;
            for (int m = 0; m < M; m++) {
                if (k - m < 0 || k - m >= N) continue;
                val += series1[m].doubleValue() * series2.getY(k - m).doubleValue();
            }
            result[k] = val;
        }
        return result;
    }

    public static Number[][] convol2D(Number[][] data1, Number[][] data2) {
        int N = data1.length;
        int M = data1[0].length;
        int a = (data2.length - 1) / 2;
        int b = (data2[0].length - 1) / 2;
        var result = new Number[N][M];
        DPMath.zeros2D(result);
        for (int i = a; i < N - a; i++) {
            for (int j = b; j < M - b; j++) {
                double sum1 = 0d;
                for (int s = -a; s <= a; s++) {
                    double sum2 = 0d;
                    for (int t = -b; t <= b; t++) {
                        try {
                            sum2 += data1[i - s][j - t].doubleValue() * data2[a + s][b + t].doubleValue();
                        } catch (IndexOutOfBoundsException e) {}
                    }
                    sum1 += sum2;
                }
                result[i][j] = (int) sum1;
            }
        }
        return result;
    }

    public static Number[][] findZeros(Number[][] data) {
        int N = data.length;
        int M = data[0].length;
        var result = new Number[N][M];
        DPMath.zeros2D(result);
        for (int i = 1; i < N - 1; i++) {
            for (int j = 1; j < M - 1; j++) {
                int negCnt = 0;
                int posCnt = 0;
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if (a != 0 && b != 0) {
                            if (data[i + a][j + b].doubleValue() < 0) negCnt++;
                            else if (data[i + a][j + b].doubleValue() > 0) posCnt++;
                        }
                    }
                }
                if (negCnt > 0 && posCnt > 0) result[i][j] = 255;
            }
        }
        return result;
    }

    public static XYSeries cutEdges(XYSeries series, int... edges) {
        XYSeries result = null;
        try {
            result = series.createCopy(0, series.getItemCount() - 1);
            result =  result.createCopy(0, result.getItemCount() - edges[1] - 1);
            result =  result.createCopy(edges[0], result.getItemCount() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //reverse series
    public static void reverseSeries(XYSeries series) {
        int N = series.getItemCount();
        Double[] values = new Double[N];
        for (int i = 0; i < N; i++) {
            values[i] = series.getY(i).doubleValue();
        }
        for (int i = 0; i < N; i++) {
            series.updateByIndex(i, values[N - i - 1]);
        }
    }

    public static void appendSeries(XYSeries initial, XYSeries append) {
        double idx = initial.getMaxX() + 1;
        for (int i = 0; i < append.getItemCount(); i++) {
            initial.add(idx, append.getY(i));
            idx++;
        }
    }

    public static Integer[][] picShift(Integer[][] data, int cnst) {
        Integer[][] shifted = new Integer[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                shifted[i][j] = data[i][j] + cnst;
            }
        }
        return shifted;
    }

    public static Integer[][] picMultiply(Integer[][] data, double cnst) {
        Integer[][] multiplied = new Integer[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                multiplied[i][j] = (int) (data[i][j] * cnst);
            }
        }
        return multiplied;
    }

    public static Number[][] narrowGSRange(Number[][] data) {
        double min = Arrays.stream(DataProcessor.toVector(data)).map(Number::doubleValue).min(Double::compareTo).get();
        double max = Arrays.stream(DataProcessor.toVector(data)).map(Number::doubleValue).max(Double::compareTo).get();
        Number[][] narrowed = new Number[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                // if (data[i][j].doubleValue() == 0) narrowed[i][j] = 0;
                narrowed[i][j] = (((data[i][j].doubleValue() - min) / (max - min)) * 255);
            }
        }
        return narrowed;
    }

    public static Integer[][] negateGC(Integer[][] data) {
        Integer[][] negated = new Integer[data.length][data[0].length];
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                if (max < data[i][j]) max = data[i][j];
            }
        }
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                negated[i][j] = max - data[i][j];
            }
        }
        return negated;
    }

    public static Integer[][] gammaCorrection(Integer[][] data, Double gamma, Integer constant) {
        Integer[][] corrected = new Integer[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                corrected[i][j] = (int) (constant * Math.pow(data[i][j], gamma));
            }
        }
        return corrected;
    }

    public static Integer[][] logCorrection(Integer[][] data, Integer constant) {
        Integer[][] corrected = new Integer[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                corrected[i][j] = (int) (constant * Math.log(data[i][j] + 1));
            }
        }
        return corrected;
    }

    public static Number[][] rescale(Number[][] data, Double multiplier, InterpolationType type) {
        Number[][] rescaled = new Number[(int) (data.length * multiplier)][(int) (data[0].length * multiplier)];
        switch (type) {
            case NEAREST_NEIGHBOUR:
                for (int i = 0; i < rescaled.length; i++) {
                    for (int j = 0; j < rescaled[0].length; j++) {
                        rescaled[i][j] = data[(int) (i / multiplier)][(int) (j / multiplier)];
                    }
                }
                break;
            case BILINEAR:
                for (int i = 0; i < rescaled.length; i++) {
                    for (int j = 0; j < rescaled[0].length; j++) {
                        int oldX = (int) (i / multiplier);
                        int oldY = (int) (j / multiplier);
                        double top = 0;
                        try {
                            top = data[oldX][oldY - 1].doubleValue();
                        } catch (RuntimeException e) {}
                        double bot = 0;
                        try {
                            bot = data[oldX][oldY + 1].doubleValue();
                        } catch (RuntimeException e) {}
                        double left = 0;
                        try {
                            left = data[oldX - 1][oldY].doubleValue();
                        } catch (RuntimeException e) {}
                        double right = 0;
                        try {
                            right = data[oldX + 1][oldY].doubleValue();
                        } catch (RuntimeException e) {}
                        rescaled[i][j] = (top + bot + left + right) / 4;
                    }
                }
                break;
            case FOURIER:
                Number[][] transformedF = null;
                int N = data.length;
                int M = data[0].length;
                int newN = (int) (N * multiplier);
                int newM = (int) (M * multiplier);
                Number[][] scaledF = new Number[newN][newM];
                if (multiplier > 1) {
                    transformedF = spectrumFourier2D(data, true, true, false);
                    int shiftN = (newN - N);
                    int shiftM = (newM - M);
                    for (int i = 0; i < newN; i++) {
                        for (int j = 0; j < newM; j++) {
                            if (i >= N/2 && i < N/2 + shiftN) scaledF[i][j] = 0;
                            else if (j >= M/2 && j < M/2 + shiftM) scaledF[i][j] = 0;
                            else {
                                int newX = i < N/2 ? i : i - shiftN;
                                int newY = j < M/2 ? j : j - shiftM;
                                scaledF[i][j] = transformedF[newX][newY];
                            }
                        }
                    }
                } else if (multiplier < 1) {
                    transformedF = new Number[N][M];
                    int m = 32;
                    for (int i = 0; i < N; i++) {
                        transformedF[i] = ConvolutionIntF(data[i], Filtering.LPF(multiplier * 0.5, 1d, m));
                    }
                    transformedF = rotate(transformedF, RotationType.RIGHT);
                    for (int i = 0; i < transformedF.length; i++) {
                        transformedF[i] = ConvolutionIntF(transformedF[i], Filtering.LPF(multiplier * 0.5, 1d, m));
                    }
                    transformedF = rotate(transformedF, RotationType.LEFT);
                    transformedF = cropImg(transformedF, N, M);
                    transformedF = spectrumFourier2D(transformedF, true, false, false);
                    int shiftN = N - newN;
                    int shiftM = M - newM;
                    for (int i = 0; i < newN; i++) {
                        int newX = i;
                        if (i >= newN / 2) {
                            newX += shiftN;
                        }
                        for (int j = 0; j < newM; j++) {
                            int newY = j;
                            if (j >= newM / 2) {
                                newY += shiftM;
                            }
                            scaledF[i][j] = transformedF[newX][newY];
                        }
                    }
                } else scaledF = transformedF;
                rescaled = inverseFourier2D(scaledF, false, false);
                break;
        }
        return rescaled;
    }

    public static Number[][] rotate(Number[][] data, RotationType rotation) {
        Number[][] rotated = null;
        int N = data.length;
        int M = data[0].length;
        switch (rotation) {
            case RIGHT:
                rotated = new Number[M][N];
                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < N; j++) {
                        rotated[i][j] = data[j][M - 1 - i];
                    }
                }
                break;
            case LEFT:
                rotated = new Number[M][N];
                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < N; j++) {
                        rotated[i][j] = data[N - 1 - j][i];
                    }
                }
                break;
            case UPSIDE:
                rotated = new Number[data.length][data[0].length];
                for (int i = 0; i < rotated.length; i++) {
                    for (int j = 0; j < rotated[0].length; j++) {
                        rotated[i][j] = data[rotated.length - 1 - i][rotated[0].length - 1 - j];
                    }
                }
                break;
        }
        return rotated;
    }

    public static Number[] toVector(Number[][] data) {
        Number[] vector = new Number[data.length * data[0].length];
        int iter = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++, iter++) {
                vector[iter] = data[i][j];
            }
        }
        return vector;
    }

    public static Integer[] toIntVector(Integer[][] data) {
        Integer[] vector = new Integer[data.length * data[0].length];
        int iter = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++, iter++) {
                vector[iter] = data[i][j];
            }
        }
        return vector;
    }

    public static <T> Number[][] toMatrix(T[] data, Integer dim1, Integer dim2) {
        Number[][] matrix = new Number[dim1][dim2];
        int iter = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++, iter++) {
                matrix[i][j] = (Number) data[iter];
            }
        }
        return matrix;
    }

    public static Integer[][] toIntMatrix(Integer[] data, Integer dim1, Integer dim2) {
        Integer[][] matrix = new Integer[dim1][dim2];
        int iter = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++, iter++) {
                matrix[i][j] = data[iter];
            }
        }
        return matrix;
    }

    public static Map<Double, Double> getCDF(Number[][] data) {
        var density = DataAnalyzer.Statistics.getProbDen(data, true);
        for (double i = 1; i < density.size(); i++) {
            density.replace(i, density.get(i) + density.get(i - 1));
        }
        // for (int i = 0; i < denVec.getItemCount(); i++) {
        //     CDFed.put(denVec.getX(i).doubleValue(), denVec.getY(i).doubleValue());
        // }
        return density;
    }

    public static XYSeries getCDFSeries(Number[][] data) {
        XYSeries cdf = new XYSeries("");
        getCDF(data).forEach((k, v) -> cdf.add(k, v));
        return cdf;
    }

    public static Number[][] translateCDF(Number[][] data) {
        Number[][] translated = new Number[data.length][data[0].length];
        double max = Arrays.stream(toVector(data)).map(Number::doubleValue).max(Double::compareTo).get();
        var CDF = getCDF(data);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                var curr = data[i][j].doubleValue();
                double currcdf = CDF.getOrDefault(curr, 1d);
                translated[i][j] = max * currcdf;
            }
        }
        return translated;
    }

    public static Number[][] getDiff(Number[][] data1, Number[][] data2) {
        var diff = new Number[data1.length][data1[0].length];
        for (int i = 0; i < diff.length; i++) {
            for (int j = 0; j < diff[0].length; j++) {
                diff[i][j] = data1[i][j].doubleValue() - data2[i][j].doubleValue();
            }
        }
        return diff;
    }

    public static Number[][] getAdd(Number[][] data1, Number[][] data2) {
        var add = new Number[data1.length][data1[0].length];
        for (int i = 0; i < add.length; i++) {
            for (int j = 0; j < add[0].length; j++) {
                add[i][j] = data1[i][j].doubleValue() + data2[i][j].doubleValue();
            }
        }
        return add;
    }

    public static Integer[] getFirstDeriv(Integer[] data) {
        Integer[] result = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            if (i == 0) {
                result[i] = (data[i + 1] - data[i]) / 2;
            } else if (i == data.length - 1) {
                result[i] = (data[i] - data[i - 1]) / 2;
            } else {
                result[i] = (data[i + 1] - data[i - 1]) / 2;
            }
        }
        return result;
    }

    public static Double detector(Integer[][] data, int incr, double dt) {
        int rown = data.length / incr;
        data = Num2Int(rotate(data, RotationType.LEFT));
        Integer[][] firstderivs = new Integer[rown][];
        for (int i = 0; i < rown; i++) {
            firstderivs[i] = getFirstDeriv(data[Math.min(i * incr, data.length - 1)]);
        }
//        XYSeries[] spectrums = new XYSeries[rown];
//        for (int i = 0; i < rown; i++) {
//            spectrums[i] = spectrumFourier(firstderivs[i], dt);
//        }
        XYSeries[] acfs = new XYSeries[rown];
        for (int i = 0; i < rown; i++) {
            acfs[i] = DataAnalyzer.Statistics.getNormalizedAutoCovariance(firstderivs[i]);
        }
        XYSeries[] cfs = new XYSeries[rown - 1];
        for (int i = 0; i < rown - 1; i++) {
            cfs[i] = DataAnalyzer.Statistics.getNormalizedCovariance(firstderivs[i], firstderivs[i + 1]);
        }
        XYSeries[] acfspectrums = new XYSeries[rown];
        for (int i = 0; i < rown; i++) {
            acfspectrums[i] = spectrumFourier(acfs[i], dt, false);
        }
        XYSeries[] cfspectrums = new XYSeries[rown - 1];
        for (int i = 0; i < rown - 1; i++) {
            cfspectrums[i] = spectrumFourier(cfs[i], dt, false);
        }
        int splitter = acfspectrums[0].getItemCount() / 2;
        Double[] maxacfs = new Double[rown];
        for (int i = 0; i < rown; i++) {
            maxacfs[i] = Math.round(findMaxValKey(DataProcessor.cutEdges(acfspectrums[i], splitter, 0)).doubleValue() * 100) / 100d;
        }
        Double[] maxcfs = new Double[rown - 1];
        for (int i = 0; i < rown - 1; i++) {
            maxcfs[i] = Math.round(findMaxValKey(DataProcessor.cutEdges(cfspectrums[i], splitter, 0)).doubleValue() * 100) / 100d;
        }
        Double maxacfsVal = findMostDenVal(maxacfs).doubleValue();
        Double maxcfsVal = findMostDenVal(maxcfs).doubleValue();
        double f0 = Math.round(((maxacfsVal + maxcfsVal) / 2) * 100) / 100d;

        SwingUtilities.invokeLater(() -> {
            new LineChartBox(acfspectrums[0], "ACF Spectrum 1").setVisible(true);
            new LineChartBox(acfspectrums[1], "ACF Spectrum 2").setVisible(true);
            new LineChartBox(cfspectrums[0], "CF Spectrum 1").setVisible(true);
            new LineChartBox(cfspectrums[1], "CF Spectrum 2").setVisible(true);
        });

        return f0;
    }

    private static Number findMaxValKey(XYSeries series) {
        Number maxKey = 0;
        double maxVal = Double.MIN_VALUE;
        for (int i = 0; i < series.getItemCount(); i++) {
            if (series.getY(i).doubleValue() > maxVal) {
                maxVal = series.getY(i).doubleValue();
                maxKey = series.getX(i).doubleValue();
            }
        }
        return maxKey;
    }

    private static Number findMostDenVal(Number[] data) {
        Number mostVal = 0;
        int mostDen = 0;
        int currentDen = 0;
        for (int i = 0; i < data.length; i++) {
            Number currVal = data[i];
            for (int j = 0; j < data.length; j++) {
                if (currVal == data[i]) {
                    currentDen++;
                }
            }
            if (currentDen > mostDen) {
                mostDen = currentDen;
                mostVal = currVal;
                currentDen = 0;
            }
        }
        return mostVal;
    }

    public static Integer[][] suppressor(Integer[][] data, int incr, int m, double dt) {
        Number[][] result = new Number[data.length][data[0].length];
        result = rotate(result, RotationType.LEFT);
        double f0 = detector(data, incr, dt);
        double f1 = f0 - 0.15;
        double f2 = f0 + 0.15;
        System.out.println("f0: " + f0);
        data = Num2Int(rotate(data, RotationType.LEFT));
        for (int i = 0; i < data.length; i++) {
            result[i] = ConvolutionIntF(data[i], Filtering.BSF(f1, f2, dt, m));
        }
        result = rotate(result, RotationType.RIGHT);
        return Num2Int(result);
    }

    public static Integer[][] randomNoiseImg(Integer[][] data, double R) {
        int N = data[0].length;
        Integer[][] result = new Integer[data.length][N];
        for (int i = 0; i < data.length; i++) {
            XYSeries noise = DataModeller.getNoise(N, R, RandomType.SYSTEM);
            for (int j = 0; j < N; j++) {
                result[i][j] = data[i][j] + noise.getY(j).intValue();
            }
        }
        return result;
    }

    public static Integer[][] impulseNoiseImg(Integer[][] data, double R, double shift, double percentile) {
        int N = data[0].length;
        Integer[][] result = new Integer[data.length][N];
        for (int i = 0; i < data.length; i++) {
            XYSeries noise = DataModeller.getImpulseNoise(N, shift, percentile, R);
            for (int j = 0; j < N; j++) {
                result[i][j] = data[i][j] + noise.getY(j).intValue();
            }
        }
        return result;
    }

    public static Number[][] combinedNoiseImg(Number[][] data, double rndR, double impR, double shift, double percentile) {
        int N = data[0].length;
        Number[][] result = new Number[data.length][N];
        for (int i = 0; i < data.length; i++) {
            XYSeries impNoise = DataModeller.getImpulseNoise(N, shift, percentile, impR);
            XYSeries rndNoise = DataModeller.getNoise(N, rndR, RandomType.SYSTEM);
            for (int j = 0; j < N; j++) {
                result[i][j] = data[i][j].doubleValue() + rndNoise.getY(j).intValue() + impNoise.getY(j).intValue();
            }
        }
        return result;
    }

    public static XYSeries spectrumFourierSeries(Complex[] data, boolean complex, boolean ermit) {
        int N = data.length;
        XYSeries resultData = new XYSeries("");
        if (complex) {
            for (int i = 0; i < N; i++) {
                resultData.add(i, ermit ? data[i].getReal() - data[i].getImaginary() : data[i].getReal() + data[i].getImaginary());
            }
        } else {
            for (int i = 0; i < N; i++) {
                resultData.add(i, Math.sqrt(Math.pow(data[i].getReal(), 2) + Math.pow(data[i].getImaginary(), 2)));
            }
        }
        return resultData;
    }

    public static Number[] spectrumFourierNum(Complex[] data, boolean complex, boolean ermit) {
        int N = data.length;
        Number[] resultData = new Number[N];
        if (complex) {
            for (int i = 0; i < N; i++) {
                resultData[i] = ermit ? data[i].getReal() - data[i].getImaginary() : data[i].getReal() + data[i].getImaginary();
            }
        } else {
            for (int i = 0; i < N; i++) {
                resultData[i] = Math.sqrt(Math.pow(data[i].getReal(), 2) + Math.pow(data[i].getImaginary(), 2));
            }
        }
        return resultData;
    }

    public static XYSeries inverseFilter(XYSeries original, XYSeries part, boolean hasNoise, double alpha) {
        XYSeries resultSeries = new XYSeries("");
        int N = original.getItemCount();
        int M = part.getItemCount();
        XYSeries partFilled = new XYSeries("", false, false);
        try {
            partFilled = part.createCopy(0, M - 1);
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = M; i < N; i++) {
            partFilled.add(i, 0);
        }
        MyComplex[] originalComplexes = DataModeller.fourierComplexes(original, true, false);
        MyComplex[] partComplexes = DataModeller.fourierComplexes(partFilled, true, false);
        if (hasNoise) {
            for (int i = 0; i < N; i++) {
                Complex ermit = partComplexes[i].conjugate();
                Complex dividend = originalComplexes[i].multiply(ermit);
                var divider = Math.pow(partComplexes[i].abs(), 2) + Math.pow(alpha, 2);
                resultSeries.add(i, new MyComplex(dividend.divide(divider)).getComplexSpectrum());
            }
        } else {
            for (int i = 0; i < N; i++) {
                Complex H = Complex.valueOf(1).divide(partComplexes[i]);
                MyComplex res = new MyComplex(originalComplexes[i].multiply(H));
                resultSeries.add(i, res.getComplexSpectrum());
            }
        }
        return spectrumFourierSeries(DataModeller.fourierComplexes(resultSeries, true, false), true, false);
    }

    public static Number[] inverseFilterNum(Number[] original, XYSeries part, boolean hasNoise, double alpha) {
        XYSeries resultSeries = new XYSeries("");
        int N = original.length;
        int M = part.getItemCount();
        XYSeries partFilled = new XYSeries("", false, false);
        try {
            partFilled = part.createCopy(0, M - 1);
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = M; i < N; i++) {
            partFilled.add(i, 0);
        }
        MyComplex[] originalComplexes = DataModeller.fourierComplexes(original, true, false);
        MyComplex[] partComplexes = DataModeller.fourierComplexes(partFilled, true, false);
        if (hasNoise) {
            for (int i = 0; i < N; i++) {
                Complex ermit = partComplexes[i].conjugate();
                Complex dividend = originalComplexes[i].multiply(ermit);
                var divider = Math.pow(partComplexes[i].abs(), 2) + Math.pow(alpha, 2);
                resultSeries.add(i, new MyComplex(dividend.divide(divider)).getComplexSpectrum());
            }
        } else {
            for (int i = 0; i < N; i++) {
                Complex H = Complex.valueOf(1).divide(partComplexes[i]);
                MyComplex res = new MyComplex(originalComplexes[i].multiply(H));
                resultSeries.add(i, res.getComplexSpectrum());
            }
        }
        return spectrumFourierNum(DataModeller.fourierComplexes(resultSeries, false, false), true, false);
    }

    public static Number[][] inverseFilter2D(Number[][] data, XYSeries function, boolean hasNoise, double alpha) {
        int M = data.length;
        int N = data[0].length;
        Number[][] resultData = new Number[N][M];
        data = rotate(data, RotationType.RIGHT);
        for (int i = 0; i < N; i++) {
            resultData[i] = inverseFilterNum(data[i], function, hasNoise, alpha);
        }
        return rotate(resultData, RotationType.LEFT);
    }

    // TODO auto-find T
    public static Number[][] edgeTranslate(Number[][] data, int... T) {
        final int edge2 = T.length > 1 ? T[1] : Integer.MAX_VALUE;
        return Arrays.stream(data)
            .map(s -> Arrays.stream(s)
                .map(n -> (n.doubleValue() <= edge2 && n.doubleValue() > T[0]) ? 255 : 0)
                .toArray(Number[]::new))
            .toArray(Number[][]::new);
    }

    public static Number[][] laplacian(Number[][] data, int[][] mask) {
        int N = data.length;
        int M = data[0].length;
        var result = new Number[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                result[i][j] = DPMath.laplacianMask(data, i, j, mask);
            }
        }
        return result;
    }

    public static <T> XYSeries Array2Series(T[] arr) {
        XYSeries result = new XYSeries("");
        for (int i = 0; i < arr.length; i++) result.add(i, (Number) arr[i]);
        return result;
    }

    public static <T> T[] Series2Array(XYSeries series, Supplier<T[]> supplier) {
        int N = series.getItemCount();
        T[] result = supplier.get();
        for (int i = 0; i < N; i++) result[i] = (T)(Object) series.getY(i);
        return result;
    }

    public static Double[][] Num2Double(Number[][] data) {
        return Arrays.stream(data)
                .map(ar -> Arrays.stream(ar).map(Number::doubleValue).toArray(Double[]::new))
                .toArray(Double[][]::new);
    }

    public static Integer[][] Num2Int(Number[][] data) {
        Integer[][] arr = Arrays.stream(data)
        .map(ar -> Arrays.stream(ar).map(Number::intValue).toArray(Integer[]::new))
        .toArray(Integer[][]::new);
        return arr;
    }

    public static Number[][] cycleShift2D(Number[][] data, int shiftX, int shiftY, boolean reverse) {
        int N = data.length;
        int M = data[0].length;
        Number[][] shifted = new Number[N][M];
        if (!reverse) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int sX = i - shiftX < 0 ? N - 1 + i - shiftX : i - shiftX;
                    int sY = j - shiftY < 0 ? M - 1 + j - shiftY : j - shiftY;
                    shifted[i][j] = data[sX][sY];
                }
            }
        } else {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int sX = i + shiftX > N - 1 ? shiftX - N + i : i + shiftX;
                    int sY = j + shiftY > M - 1 ? shiftY - M + j: j + shiftY;
                    shifted[i][j] = data[sX][sY];
                }
            }
        }
        
        return shifted;
    }

    public static Number[][] recomputeGSRange(Number[][] data) {
        int N = data.length;
        int M = data[0].length;
        Number[][] resultData = new Number[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                Number value = data[i][j];
                if (value.doubleValue() > 255) value = 255;
                if (value.doubleValue() < 0) value = 0;
                resultData[i][j] = value;
            }
        }
        return resultData;
    }

    public static Number[][] cropImg(Number[][] data, int newX, int newY) {
        int shiftN = (data.length - newX) / 2;
        int shiftM = (data[0].length - newY) / 2;
        Number[][] resultData = new Number[newX][newY];
        for (int i = 0; i < newX; i++) {
            for (int j = 0; j < newY; j++) {
                resultData[i][j] = data[i + shiftN][j + shiftM];
            }
        }
        return resultData;
    }

    public static Number[][] cloneNumMatrix(Number[][] data) {
        return Arrays.stream(data).map(Number[]::clone).toArray(Number[][]::new);
    }
}
