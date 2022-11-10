package moed.application.MOED_app.Entities;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NonNull;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.components.AppConfig;

import java.io.IOException;

public class TrendInfo {
    @Getter private String trendPath;
    @NonNull @Getter private final Trend trend;
    @Getter private String histPath;
    @NonNull @Getter private final Trend hist;
    @Getter private String autoCovariancePath;
    @NonNull @Getter private final Trend autoCovariance;
    @Getter private String normalizedAutoCovariancePath;
    @NonNull @Getter private final Trend normalizedAutoCovariance;
    @NonNull @Getter private final Stats stats;
    @Getter private String statsJSON;

    public TrendInfo(Trend trend) {
        this.trend = trend;
        this.stats = DataAnalyzer.Statistics.getStats(trend.getSeries());
        try {
            this.statsJSON = new GsonBuilder().setPrettyPrinting().create().toJson(this.stats);
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
        try {
            this.trendPath = DataModeller.getTrend(this.trend, AppConfig.IOCConfig.DEFAULT_TREND_RATIO);
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
        this.hist = DataAnalyzer.Statistics.getProbDen(this.trend.getSeries());
        try {
            this.histPath = DataModeller.getTrend(this.hist, AppConfig.IOCConfig.DEFAULT_HIST_RATIO);
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
        this.autoCovariance = new Trend("Auto Covariance")
                .setSeries(DataAnalyzer.Statistics.getAutoCovariance(this.trend.getSeries()));
        try {
            this.autoCovariancePath = DataModeller
                    .getTrend(this.autoCovariance, AppConfig.IOCConfig.DEFAULT_HIST_RATIO);
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
        this.normalizedAutoCovariance = new Trend("Normalized Auto Covariance")
                .setSeries(DataAnalyzer.Statistics.getNormalizedAutoCovariance(this.trend.getSeries()));
        try {
            this.normalizedAutoCovariancePath = DataModeller
                    .getTrend(this.normalizedAutoCovariance, AppConfig.IOCConfig.DEFAULT_HIST_RATIO);
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
    }
}
