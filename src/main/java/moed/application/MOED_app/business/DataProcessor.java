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
            result.add(series.getX(i), (Double) series.getY(i + 1) - (Double) series.getY(i));
        }
        return DataAnalyzer.getAntiShift(result);
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
        double df = rate/N;
        XYSeries spectrum = DataModeller.fourier(series, windowSize);
        for (int i = 0; i < N/2; i++) {
            result.add(i * df, spectrum.getY(i));
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
}
