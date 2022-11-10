package moed.application.MOED_app.business;

import moed.application.MOED_app.components.AppConfig;
import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class IOC {

    public static String saveChartAsPNG(JFreeChart chart, Integer[] ratio) throws IOException {
        final String path = AppConfig.IOCConfig.CHART_FOLDER_WEB
                + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + ".png";
        ChartUtils.writeChartAsPNG(
                FileUtils.openOutputStream(new File(path)),
                chart,
                ratio[0],
                ratio[1]
        );
        return path;
    }

    public static Path getCleanPath() {
        return Path.of(AppConfig.IOCConfig.CLEAN_PATH);
    }
}
