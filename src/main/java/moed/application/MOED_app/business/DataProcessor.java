package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.business.DataAnalyzer.Statistics;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;

public class DataProcessor {
    //Выделение изначальных трендов из смешанных данных с помощью производной
    public static XYSeries reverseMergeDerivative(XYSeries series) {
        XYSeries result = new XYSeries("");
        for (int i  = 0; i < series.getItemCount() - 1; i++) {
            result.add(i, (Double) series.getY(i + 1) - (Double) series.getY(i));
        }
        return DataAnalyzer.Statistics.getAntiShift(result);
    }

    //Выделение изначальных трендов из смешанных данных с помощью среднего
    public static XYSeries reverseMergeAverage(XYSeries series) {
        XYSeries result = new XYSeries("");
        int delta = series.getItemCount() / 80;
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
        Double shift = result.getMaxY() - shifted.getMinY();
        for (var item : shifted.getItems()) {
            result.add(((XYDataItem) item).getX(), (Double) ((XYDataItem) item).getY() + shift);
        }
        return result;
    }

    public static XYSeries spectrumFourier(XYSeries series, Double dt, int... windowSize) {
        XYSeries result = new XYSeries("");
        int N = series.getItemCount();
        double rate = 1/dt;
        int df = (int) rate/N;
        df = Math.max(df, 1);
        XYSeries spectrum = DataModeller.fourier(series, windowSize);
        for (int i = 0; i < N/2; i += df) {
            result.add(i, spectrum.getY(i));
        }
        return result;
    }

    public static XYSeries antiNoise(int M, XYSeries... series) {
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
                Double[] noiseVals = DataModeller.getNoiseOptimized(N, 30, RandomType.SYSTEM);
                for (int j = 0; j < N; j++) {
                    sysNoise.add(j, noiseVals[j]);
                }
                additive = DataModeller.getMerged(series[0], sysNoise);
                additives.add(additive);
            } else {
                noises.add(DataModeller.getNoiseOptimized(N, 30, RandomType.SYSTEM));
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
        try {
            result =  result.createCopy(0, N - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
