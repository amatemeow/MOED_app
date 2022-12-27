package moed.application.MOED_app.business;

import moed.application.MOED_app.Entities.Stats;
import moed.application.MOED_app.Entities.Trend;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.List;

public class DataAnalyzer {
    public static class Statistics {
        public static Stats getStats(XYSeries series) {
            Double[] arrMinMax = getMinMax(series);
            return new Stats(
                    arrMinMax[0],
                    arrMinMax[1],
                    getAverage(series),
                    getMomentum(2, series),
                    getMeanDeviation(series),
                    getMomentum(2, series, false),
                    getRootMeanSquareError(series),
                    getAsymmetricalIndex(series),
                    getKurtosisIndex(series),
                    isStationary(10, series)
            );
        }

        public static Double[] getMinMax(XYSeries series) {
            Double[] result = new Double[2];
            result[0] = series.getMinY();
            result[1] = series.getMaxY();
            return result;
        }

        public static Double getAverage(XYSeries series) {
            int N = series.getItemCount();
            double xSum = 0;
            for (var item : (List<XYDataItem>) series.getItems()) {
                xSum += item.getYValue();
            }
            return xSum / N;
        }

        public static Double getMomentum(int pow, XYSeries series, boolean... withAverage) {
            double avg = (withAverage.length == 0 || withAverage[0]) ? getAverage(series) : 0;
            int N = series.getItemCount();
            double sum = 0;
            for (var item : (List<XYDataItem>) series.getItems()) {
                sum += Math.pow(item.getYValue() - avg, pow);
            }
            return sum / N;
        }

        public static Double getMeanDeviation(XYSeries series) {
            return Math.sqrt(getMomentum(2, series));
        }

        public static Double getRootMeanSquareError(XYSeries series) {
            return Math.sqrt(getMomentum(2, series, false));
        }

        public static Double getAsymmetricalIndex(XYSeries series) {
            return getMomentum(3, series) / Math.pow(getMeanDeviation(series), 3);
        }

        public static Double getKurtosisIndex(XYSeries series) {
            return getMomentum(4, series) / Math.pow(getMeanDeviation(series), 4) - 3;
        }

        public static boolean isStationary(int splitCount, XYSeries series) {
            final Double[] averages = new Double[splitCount];
            final Double[] meanDeviations = new Double[splitCount];
            final int delta = series.getItemCount() / splitCount;
            final XYSeries[] dataSeries = new XYSeries[splitCount];

            for (int i = 0; i < splitCount; i++) {
                int start = delta * i;
                dataSeries[i] = new XYSeries("");
                for (int j = start; j < start + delta; j++) {
                    if (j >= series.getItemCount()) break;
                    dataSeries[i].add(series.getDataItem(j));
                }
            }
            for (int i = 0; i < splitCount; i++) {
                averages[i] = getAverage(dataSeries[i]);
                meanDeviations[i] = getMeanDeviation(dataSeries[i]);
            }
            for (int i = 0; i < splitCount; i++) {
                for (int j = i + 1; j < splitCount; j++) {
                    if (i == j) continue;
                    if (Math.abs((averages[i] - averages[j]) / getMinMax(series)[1] * 100) > 10) return false;
                }
                for (int j = i + 1; j < splitCount; j++) {
                    if (i == j) continue;
                    if (Math.abs((meanDeviations[i] - meanDeviations[j]) / getMinMax(series)[1] * 100) > 10) return false;
                }
            }

            return true;
        }

        //Изменить реализацию на series
        public static XYSeries getProbDen(XYSeries series) {
            //Разбить на M интервалов и считать, сколько в каждый интервал попадает значений
            //Min, Max, Avg
            //Noise, SelfNoise, etc.
            XYSeries densitySeries = new XYSeries("");
            int N = series.getItemCount();
            Double[] borders = getMinMax(series);
            double intervalLength = (borders[1] - borders[0]) / 100;
            for (double i = borders[0]; i < borders[1]; i += intervalLength) {
                int cnt = 0;
                for (var item : series.getItems()) {
                    Double yValue = ((XYDataItem) item).getYValue();
                    if (yValue >= i && yValue < i + intervalLength) {
                        cnt++;
                    }
                }
                densitySeries.add((i + intervalLength) / 2, cnt);
            }
            return densitySeries;
        }

        //Автокорреляция
        public static XYSeries getAutoCovariance(XYSeries series) {
            XYSeries result = new XYSeries("");
            Double R;
            int N = series.getItemCount();
            Double avg = getAverage(series);
            for (int l = 0; l < N - 1; l++) {
                R = 0d;
                for (int i = 0; i < N - l; i++) {
                    R += 1d/N * ((Double) series.getY(i) - avg) * ((Double) series.getY(i + l) - avg);
                }
                result.add(l, R);
            }
            return result;
        }

        //Нормализованная корреляция
        public static XYSeries getNormalizedAutoCovariance(XYSeries series) {
            XYSeries result = getAutoCovariance(series);
            Double maxR = getMinMax(result)[1];
            for (int i = 0; i < result.getItemCount(); i++) {
                result.updateByIndex(i, (Double) result.getY(i) / maxR);
            }
            return result;
        }

        //Взаимная корреляция
        public static XYSeries getCovariance(XYSeries series1, XYSeries series2) {
            XYSeries result = new XYSeries("");
            Double R;
            int N = Math.min(series1.getItemCount(), series2.getItemCount());
            Double xAvg = getAverage(series1);
            Double yAvg = getAverage(series2);
            for (int l = 0; l < N - 1; l++) {
                R = 0d;
                for (int i = 0; i < N - l; i++) {
                    R += 1d/N * ((Double) series1.getY(i) - xAvg) * ((Double) series2.getY(i + l) - yAvg);
                }
                result.add(l, R);
            }
            return result;
        }
    }

    //Восстановление сдвига
    public static XYSeries getAntiShift(XYSeries series) {
        XYSeries result = new XYSeries("");
        Double avg = Statistics.getAverage(series);
        for (var item : series.getItems()) {
            result.add(((XYDataItem) item).getX(), (Double) ((XYDataItem) item).getY() - avg);
        }
        return result;
    }

    //Без спайков
    public static XYSeries getAntiSpike(XYSeries series, Double R) {
        XYSeries result = new XYSeries("");
        int count = series.getItemCount();
        for (int i = 0; i < count; i++) {
            if (Math.abs((Double) series.getY(i)) > R) {
                if (i == 0) {
                    result.add(i,
                            ((Double) series.getY(i + 1) + (Double) series.getY(i + 2)) / 2);
                } else if (i == count - 1) {
                    result.add(i,
                            ((Double) series.getY(i - 1) + (Double) series.getY(i - 2)) / 2);
                } else {
                    result.add(i,
                            ((Double) series.getY(i - 1) + (Double) series.getY(i + 1)) / 2);
                }
            } else {
                result.add(i, series.getY(i));
            }
        }
        return result;
    }

    public static XYSeries analyzeAntiNoiseSTD(int step, int ceil, int R) {
        XYSeries result = new XYSeries("");
        for (int i = 1; i <= ceil; i += step) {
            result.add(i, Statistics.getMeanDeviation(DataProcessor.antiNoise(i, R)));
        }
        return result;
    }

    public static XYSeries filterFreq(XYSeries filter, Double dt, int m) {
        XYSeries result = new XYSeries("");
        XYSeries spectrum = DataProcessor.spectrumFourier(filter, dt);
        for (int i = 0; i < spectrum.getItemCount(); i++) {
            result.add(i, spectrum.getY(i).doubleValue() * (2 * m + 1));
        }
        return result;
    }
}
