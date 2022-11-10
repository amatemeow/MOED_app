package moed.application.MOED_app.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

@Component
public class Charts {

    public static JFreeChart getLineChart(
            String chartName,
            String XAxisName,
            String YAxisName,
            XYSeries series) {

        JFreeChart chart = ChartFactory.createXYLineChart(
                chartName,
                XAxisName,
                YAxisName,
                new XYSeriesCollection(series),
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        return chart;
    }
}
