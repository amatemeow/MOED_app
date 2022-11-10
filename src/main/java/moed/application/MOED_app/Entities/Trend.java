package moed.application.MOED_app.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moed.application.MOED_app.ENUM.ChartType;
import moed.application.MOED_app.ENUM.LineType;
import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.components.AppConfig;
import org.jfree.data.xy.XYSeries;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Trend {
    private final AppConfig.SampleData SAMPLE_DATA = new AppConfig.SampleData();
    private LineType lineType = LineType.SIMPLE;
    private ChartType chartType = ChartType.LINE;
    private int N = 1000;
    private double k = 10;
    private double b = 10;
    private Boolean direction = true;
    private String chartName = "Sample Chart";
    private String XAxisName = "X";
    private String YAxisName = "Y";
    private RandomType randomType = RandomType.SYSTEM;
    private double R = 50;
    private double impulseRs;
    private double impulsePercentile;
    @Setter
    private XYSeries series;

    public Trend(String chartName) {
        this.chartName = chartName;
    }

    public Trend(String chartName, boolean direction) {
        this.chartName = chartName;
        this.direction = direction;
    }

    public Trend(
            String chartName,
            String XAxisName,
            String YAxisName) {
        this.chartName = chartName;
        this.XAxisName = XAxisName;
        this.YAxisName = YAxisName;
    }

    public Trend(
            LineType lineType,
            int N,
            double k,
            double b,
            boolean direction) {
        this.lineType = lineType;
        this.N = N;
        this.k = k;
        this.b = b;
        this.direction = direction;
    }

    public Trend(
            LineType lineType,
            int N,
            double k,
            double b,
            boolean direction,
            String chartName) {
        this.lineType = lineType;
        this.N = N;
        this.k = k;
        this.b = b;
        this.direction = direction;
        this.chartName = chartName;
    }

    public Trend(
            LineType lineType,
            int N,
            double k,
            double b,
            boolean direction,
            String chartName,
            String XAxisName,
            String YAxisName) {
        this.lineType = lineType;
        this.N = N;
        this.k = k;
        this.b = b;
        this.direction = direction;
        this.chartName = chartName;
        this.XAxisName = XAxisName;
        this.YAxisName = YAxisName;
    }

    public Trend(
            int N,
            double R,
            RandomType randomType) {
        this.lineType = LineType.NOISE;
        this.N = N;
        this.R = R;
        this.randomType = randomType;
    }

    public Trend(
            int N,
            double R,
            RandomType randomType,
            String chartName) {
        this.lineType = LineType.NOISE;
        this.N = N;
        this.R = R;
        this.randomType = randomType;
        this.chartName = chartName;
    }

    public Trend(
            int N,
            double R,
            RandomType randomType,
            String chartName,
            String XAxisName,
            String YAxisName) {
        this.lineType = LineType.NOISE;
        this.N = N;
        this.R = R;
        this.randomType = randomType;
        this.chartName = chartName;
        this.XAxisName = XAxisName;
        this.YAxisName = YAxisName;
    }

    public Trend(
            LineType lineType,
            int N,
            double R,
            double Rs,
            double Pcnt,
            RandomType randomType,
            String chartName) {
        this.lineType = lineType;
        this.N = N;
        this.R = R;
        this.impulseRs = Rs;
        this.impulsePercentile = Pcnt;
        this.randomType = randomType;
        this.chartName = chartName;
    }
    
    public XYSeries getSeries() {
        if (this.series != null) return this.series;
        final int dir = this.direction ? 1 : -1;
        XYSeries series = new XYSeries("");

        switch (lineType) {
            case SIMPLE:
                for (double i : SAMPLE_DATA.getSampleData(N)) {
                    double Yvalue = dir * k * i + b;
                    if (Yvalue == Double.POSITIVE_INFINITY) { break; }
                    series.add(i,Yvalue);
                }
                break;
            case EXPONENT:
                for (double i : SAMPLE_DATA.getSampleData(N)) {
                    double Yvalue = b * Math.exp(dir * k * i);
                    if (Yvalue == Double.POSITIVE_INFINITY) { break; }
                    series.add(i, Yvalue);
                }
                break;
            case COMBINED:
                double firstMax = 0;
                double secondMax = 0;
                int firstEdge = SAMPLE_DATA.getSampleData(N).length / 3;
                int secondEdge = firstEdge * 2;
                for (double i : SAMPLE_DATA.getSampleData(N)) {
                    if (i <= firstEdge) {
                        double Yvalue = b * Math.exp(dir * k * i);
                        if (Yvalue == Double.POSITIVE_INFINITY) { continue; }
                        series.add(i, Yvalue);
                    } else if (i <= secondEdge) {
                        if (i == firstEdge + 1) { firstMax = series.getMaxY() - (dir * k * i); }
                        series.add(i,dir * k * i + firstMax);
                    } else {
                        if (i == secondEdge + 1) {
                            secondMax = series.getMaxY() - (-b/3 * Math.exp(dir * k * i));
                        }
                        double Yvalue = -b/3 * Math.exp(dir * k * i) + secondMax;
                        if (Yvalue == Double.POSITIVE_INFINITY) { continue; }
                        series.add(i, Yvalue);
                    }
                }
                break;
            case NOISE:
                Double[] dataY = DataModeller.getNoise(N, R, randomType);
                Double[] dataX = SAMPLE_DATA.getSampleData(N);
                for (int i = 0; i < dataX.length; i++) {
                    series.add(dataX[i], dataY[i]);
                }
                break;
            case IMPULSE_NOISE:
                Double[] yData = DataModeller.getImpulseNoise(
                        N,
                        R,
                        impulsePercentile,
                        impulseRs
                );
                Double[] xData = SAMPLE_DATA.getSampleData(N);
                for (int i = 0; i < xData.length; i++) {
                    series.add(xData[i], yData[i]);
                }
                break;
        }
        this.series = series;
        return this.series;
    }

    public Trend setSeries(XYSeries series) {
        this.series = series;
        return this;
    }
}
