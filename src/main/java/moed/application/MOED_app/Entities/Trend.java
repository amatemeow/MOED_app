package moed.application.MOED_app.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moed.application.MOED_app.components.AppConfig;
import org.jfree.data.xy.XYSeries;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Trend {
    private final AppConfig.SampleData SAMPLE_DATA = new AppConfig.SampleData();
    private String chartName = "Sample Chart";
    private String XAxisName = "X";
    private String YAxisName = "Y";
    @Setter
    private XYSeries series;

    public Trend(String chartName) {
        this.chartName = chartName;
    }

    public Trend(
            String chartName,
            String XAxisName,
            String YAxisName) {
        this.chartName = chartName;
        this.XAxisName = XAxisName;
        this.YAxisName = YAxisName;
    }
    
    public XYSeries getSeries() {
        return this.series;

//        switch (lineType) {
//            case SIMPLE:
//                for (double i : SAMPLE_DATA.getSampleData(N)) {
//                    double Yvalue = dir * k * i + b;
//                    if (Yvalue == Double.POSITIVE_INFINITY) { break; }
//                    series.add(i,Yvalue);
//                }
//                break;
//            case EXPONENT:
//                for (double i : SAMPLE_DATA.getSampleData(N)) {
//                    double Yvalue = b * Math.exp(dir * k * i);
//                    if (Yvalue == Double.POSITIVE_INFINITY) { break; }
//                    series.add(i, Yvalue);
//                }
//                break;
//            case COMBINED:
//                double firstMax = 0;
//                double secondMax = 0;
//                int firstEdge = SAMPLE_DATA.getSampleData(N).length / 3;
//                int secondEdge = firstEdge * 2;
//                for (double i : SAMPLE_DATA.getSampleData(N)) {
//                    if (i <= firstEdge) {
//                        double Yvalue = b * Math.exp(dir * k * i);
//                        if (Yvalue == Double.POSITIVE_INFINITY) { continue; }
//                        series.add(i, Yvalue);
//                    } else if (i <= secondEdge) {
//                        if (i == firstEdge + 1) { firstMax = series.getMaxY() - (dir * k * i); }
//                        series.add(i,dir * k * i + firstMax);
//                    } else {
//                        if (i == secondEdge + 1) {
//                            secondMax = series.getMaxY() - (-b/3 * Math.exp(dir * k * i));
//                        }
//                        double Yvalue = -b/3 * Math.exp(dir * k * i) + secondMax;
//                        if (Yvalue == Double.POSITIVE_INFINITY) { continue; }
//                        series.add(i, Yvalue);
//                    }
//                }
//                break;
//            case NOISE:
//                Double[] dataY = DataModeller.getNoise(N, R, randomType);
//                Double[] dataX = SAMPLE_DATA.getSampleData(N);
//                for (int i = 0; i < dataX.length; i++) {
//                    series.add(dataX[i], dataY[i]);
//                }
//                break;
//            case IMPULSE_NOISE:
//                Double[] yData = DataModeller.getImpulseNoise(
//                        N,
//                        R,
//                        impulsePercentile,
//                        impulseRs
//                );
//                Double[] xData = SAMPLE_DATA.getSampleData(N);
//                for (int i = 0; i < xData.length; i++) {
//                    series.add(xData[i], yData[i]);
//                }
//                break;
//        }
//        this.series = series;
//        return this.series;
    }

    public Trend setSeries(XYSeries series) {
        this.series = series;
        return this;
    }
}
