package moed.application.MOED_app.business;

import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.business.DataAnalyzer.Statistics;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;

import static moed.application.MOED_app.business.DataModeller.removeTrend;

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
        XYSeries spectrum = DataModeller.fourier(series, windowSize);
        for (int i = 0; i < N/2; i += df) {
            result.add(i, spectrum.getY(i));
        }
        return result;
    }

    public static XYSeries antiNoise(int M, XYSeries... series) {
        XYSeries result = new XYSeries("");
        XYSeries additive = new XYSeries("");
        int N = 1000;
        if (series.length != 0) {
            N = series[0].getItemCount();
            XYSeries addNoise = new XYSeries("");
            Double[] sysNoise = DataModeller.getNoise(N, 30, RandomType.SYSTEM);
            for (int i = 0; i < N; i++) {
                addNoise.add(i, sysNoise[i]);
            }
            additive = DataModeller.getMerged(series[0], addNoise);
        }
        ArrayList<Double[]> noises = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            noises.add(DataModeller.getNoise(N, 30, RandomType.SYSTEM));
        }
        for (int i = 0; i < N; i++) {
            Double value = 0.d;
            for (var noise : noises) {
                value += noise[i];
            }
            result.add(i, value / M);
        }
        if (series.length != 0) {
            XYSeries addResult = new XYSeries("");
            for (int i = 0; i < N; i++) {
                addResult.add(i, (Double) additive.getY(i) - (Double) result.getY(i));
            }
            return addResult;
        }
        return  result;
    }
}
