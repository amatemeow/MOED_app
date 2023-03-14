package moed.application.MOED_app.components;

import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.business.DataModeller;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class LineChartBox extends JFrame {

    public LineChartBox(XYSeries data, String name) {
        initUI(data, name);
    }

    public LineChartBox(XYSeries data, String name, Integer[] size) {
        initUI(data, name, size);
    }

    private void initUI(XYSeries data, String name, Integer[]... size) {
        Integer[] ratio = size.length == 0 ? new Integer[] {480, 360} : size[0];
        JFreeChart chart = DataModeller.getChart(
                new Trend(name).setSeries(data),
                ratio
        );

        XYPlot plot = chart.getXYPlot();
        var renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        plot.setRenderer(renderer);

//        StandardChartTheme theme = new StandardChartTheme(name);


        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setPreferredSize(new Dimension(ratio[0], ratio[1]));
        add(chartPanel);

        pack();
        setTitle(name);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
