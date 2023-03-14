package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.InterpolationType;
import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.ENUM.RotationType;
import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.business.DataAnalyzer.Statistics;

import moed.application.MOED_app.components.LineChartBox;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DataProcessor {
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

    public static XYSeries spectrumFourier(XYSeries series, Double dt, int... windowSize) {
        XYSeries result = new XYSeries("");
        int N = series.getItemCount();
        double rate = 1/dt;
        double df = rate/N;
        XYSeries spectrum = DataModeller.fourier(series, windowSize);
        for (int i = 0; i < N/2; i++) {
            result.add(i * df, spectrum.getY(i));
        }
        return result;
    }

    public static XYSeries spectrumFourier(Integer[] data, Double dt, int... windowSize) {
        XYSeries result = new XYSeries("");
        int N = data.length;
        double rate = 1/dt;
        double df = rate/N;
        XYSeries spectrum = DataModeller.fourier(data, windowSize);
        for (int i = 0; i < N/2; i++) {
            result.add(i * df, spectrum.getY(i));
        }
        return result;
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

    public static Integer[] ConvolutionIntF(Integer[] series1, XYSeries series2) {
        int N = series2.getItemCount();
        int M = series1.length;
        Integer[] result = new Integer[N + M];
        for (int k = 0; k < N + M; k++) {
            Integer val = 0;
            for (int m = 0; m < M; m++) {
                if (k - m < 0 || k - m >= N) continue;
                val += (int) (series1[m] * series2.getY(k - m).doubleValue());
            }
            result[k] = val;
        }
        return result;
    }

    public static Integer[] ConvolutionIntF2(Integer[] series2, XYSeries series1) {
        int N = series2.length;
        int M = series1.getItemCount();
        Integer[] result = new Integer[N + M];
        for (int k = 0; k < N + M; k++) {
            Integer val = 0;
            for (int m = 0; m < M; m++) {
                if (k - m < 0 || k - m >= N) continue;
                val += (int) (series1.getY(m).doubleValue() * series2[k - m]);
            }
            result[k] = val;
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

    public static Integer[][] narrowGSRange(Integer[][] data) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        Integer[][] narrowed = new Integer[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                if (min > data[i][j]) min = data[i][j];
                if (max < data[i][j]) max = data[i][j];
            }
        }
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                narrowed[i][j] = (int) (((double) (data[i][j] - min) / (max - min)) * 255);
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

    public static Integer[][] rescale(Integer[][] data, Double multiplier, InterpolationType type) {
        Integer[][] rescaled = new Integer[(int) (data.length * multiplier)][(int) (data[0].length * multiplier)];
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
                        int top = 0;
                        try {
                            top = data[oldX][oldY - 1];
                        } catch (RuntimeException e) {}
                        int bot = 0;
                        try {
                            bot = data[oldX][oldY + 1];
                        } catch (RuntimeException e) {}
                        int left = 0;
                        try {
                            left = data[oldX - 1][oldY];
                        } catch (RuntimeException e) {}
                        int right = 0;
                        try {
                            right = data[oldX + 1][oldY];
                        } catch (RuntimeException e) {}
                        rescaled[i][j] = (top + bot + left + right) / 4;
                    }
                }
                break;
        }
        return rescaled;
    }

    public static Integer[][] rotate(Integer[][] data, RotationType rotation) {
        Integer[][] rotated = null;
        switch (rotation) {
            case RIGHT:
                rotated = new Integer[data[0].length][data.length];
                for (int i = 0; i < rotated.length; i++) {
                    for (int j = 0; j < rotated[0].length; j++) {
                        rotated[i][j] = data[j] == null ? 0 : data[j][rotated.length - 1 - i];
                    }
                }
                break;
            case LEFT:
                rotated = new Integer[data[0].length][data.length];
                for (int i = 0; i < rotated.length; i++) {
                    for (int j = 0; j < rotated[0].length; j++) {
                        rotated[i][j] = data[rotated[0].length - 1 - j][i];
                    }
                }
                break;
            case UPSIDE:
                rotated = new Integer[data.length][data[0].length];
                for (int i = 0; i < rotated.length; i++) {
                    for (int j = 0; j < rotated[0].length; j++) {
                        rotated[i][j] = data[rotated.length - 1 - i][rotated[0].length - 1 - j];
                    }
                }
                break;
        }
        return rotated;
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

    public static Double[] getCDF(Integer[][] data) {
        Double[] denVec = DataAnalyzer.Statistics.getDensityVector(toIntVector(data));
        Double[] CDFed = new Double[denVec.length];
        for (int i = 0; i < CDFed.length; i++) {
            double sum = 0;
            for (int j = 0; j < i; j++) {
                sum += denVec[j] == null ? 0.0 : denVec[j];
            }
            CDFed[i] = sum;
        }
        return CDFed;
    }

    public static Integer[][] translateCDF(Integer[][] data) {
        Integer[][] translated = new Integer[data.length][data[0].length];
        Double[] CDF = getCDF(data);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                translated[i][j] = (int) Math.round(data[i][j] * CDF[data[i][j]]);
            }
        }
        return translated;
    }

    public static Integer[][] getDiff(Integer[][] data1, Integer[][] data2) {
        Integer[][] diff = new Integer[data1.length][data1[0].length];
        for (int i = 0; i < diff.length; i++) {
            for (int j = 0; j < diff[0].length; j++) {
                diff[i][j] = data1[i][j] - data2[i][j];
            }
        }
        return diff;
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
        data = rotate(data, RotationType.LEFT);
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
            acfspectrums[i] = spectrumFourier(acfs[i], dt);
        }
        XYSeries[] cfspectrums = new XYSeries[rown - 1];
        for (int i = 0; i < rown - 1; i++) {
            cfspectrums[i] = spectrumFourier(cfs[i], dt);
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
        Integer[][] result = new Integer[data.length][];
        double f0 = detector(data, incr, dt);
        double f1 = f0 - 0.15;
        double f2 = f0 + 0.15;
        System.out.println("f0: " + f0);
        data = rotate(data, RotationType.LEFT);
        for (int i = 0; i < data.length; i++) {
            result[i] = ConvolutionIntF(data[i], Filtering.BSF(f1, f2, dt, m));
        }
        result = rotate(result, RotationType.RIGHT);
        return result;
    }
}
