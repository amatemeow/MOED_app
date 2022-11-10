package moed.application.MOED_app.components;

import lombok.Getter;
import moed.application.MOED_app.ENUM.LineType;
import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.business.DataProcessor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;

@Configuration
@EnableWebMvc
@ComponentScan
public class AppConfig implements WebMvcConfigurer {

    @Component
    public static class IOCConfig {
        public static final String CHART_FOLDER_WEB = "images";
        public static final String CLEAN_PATH = CHART_FOLDER_WEB;
        public static final Integer[] DEFAULT_TREND_RATIO = {1080, 620};
        public static final Integer[] DEFAULT_HIST_RATIO = {720, 360};
    }

    @Component
    public static class SampleData {
        @Getter
        private final LinkedHashMap<String, Trend> TRENDS = new LinkedHashMap<>();

        @PostConstruct
        public void populateTrends() {
            TRENDS.put("Straight Positive", new Trend("Straight Positive"));
            TRENDS.put("Straight Negative", new Trend("Straight Negative", false));
            TRENDS.put("Exponential Positive", new Trend(
                    LineType.EXPONENT,
                    1000,
                    0.005,
                    50,
                    true,
                    "Exponential Positive"));
            TRENDS.put("Exponential Negative", new Trend(
                    LineType.EXPONENT,
                    1000,
                    0.005,
                    50,
                    false,
                    "Exponential Negative"));
            TRENDS.put("Combined Chart", new Trend(
                    LineType.COMBINED,
                    1000,
                    0.005,
                    100,
                    true,
                    "Combined Chart"));
            TRENDS.put("System Noise", new Trend(
                    10000, 105.15, RandomType.SYSTEM, "System Noise"));
            TRENDS.put("Custom Noise", new Trend(
                    10000, 105.15, RandomType.SELF, "Custom Noise"));
            TRENDS.put("Shifted Straight", DataModeller.getShiftedTrend(
                    new Trend("Shifted"), 569.2, 16, 650));
            TRENDS.put("Impulse Noise", new Trend(
                    LineType.IMPULSE_NOISE, 1000, 105.2, 24.6, 0.006, RandomType.SYSTEM, "Impulse Noise"));
            TRENDS.put("Harmonic", new Trend("Harmonic").setSeries(DataModeller.getHarm(1000, 100, 33, 0.001)));
            TRENDS.put("Wrong Harmonic", new Trend("Harmonic, but f0 is wrong").setSeries(DataModeller.getHarm(1000, 100, 501, 0.001)));
            TRENDS.put("Poly Harmonic", new Trend("Poly Harmonic").setSeries(
                    DataModeller.getPolyHarm(1000, new int[] {100, 15, 30}, new int[] {33, 5, 170}, 0.001)));
            TRENDS.put("Normalized Noise Covariance", new Trend("Normalized Noise Covariance").setSeries(
                    DataAnalyzer.Statistics.getNormalizedAutoCovariance(
                            DataAnalyzer.Statistics.getCovariance(
                                    TRENDS.get("System Noise").getSeries(), TRENDS.get("Custom Noise").getSeries()))));
            TRENDS.put("Normalized Harmonic Covariance", new Trend("Normalized Harmonic Covariance").setSeries(
                    DataAnalyzer.Statistics.getNormalizedAutoCovariance(
                            DataAnalyzer.Statistics.getCovariance(
                                    TRENDS.get("Harmonic").getSeries(), TRENDS.get("Poly Harmonic").getSeries()))));
            TRENDS.put("Antishift Straight", new Trend("Anti Shifted").setSeries(
                    DataAnalyzer.Statistics.getAntiShift(TRENDS.get("Shifted Straight").getSeries())));
            TRENDS.put("Spiked Noise", new Trend("Spiked").setSeries(
                            DataModeller.getMerged(TRENDS.get("System Noise").getSeries(), new Trend(
                                    LineType.IMPULSE_NOISE,
                                    1000,
                                    DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1],
                                    24.6,
                                    0.006,
                                    RandomType.SYSTEM,
                                    "").getSeries())));
            TRENDS.put("Antispiked Noise", new Trend("Anti Spiked").setSeries(
                    DataAnalyzer.Statistics.getAntiSpike(
                            DataModeller.getMerged(TRENDS.get("System Noise").getSeries(), new Trend(
                                    LineType.IMPULSE_NOISE,
                                    1000,
                                    DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1],
                                    24.6,
                                    0.006,
                                    RandomType.SYSTEM,
                                    "").getSeries()),
                            DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1])));
            TRENDS.put("Merged with Harmonic", new Trend("Merged").setSeries(
                    DataModeller.getMerged(
                            new Trend(
                                    LineType.SIMPLE,
                                    1000,
                                    0.3,
                                    2,
                                    true,
                                    ""
                            ).getSeries(),
                            DataModeller.getHarm(
                                    1000,
                                    5,
                                    250,
                                    0.003
                            )
                    )));
            TRENDS.put("Harmonic in Merged", new Trend("Harmonic in Merged").setSeries(
                            DataModeller.getHarm(
                                    1000,
                                    5,
                                    250,
                                    0.003
                            )));
            TRENDS.put("Reversed Harmonic Merge", new Trend("Reverse Merge with Derivative").setSeries(
                    DataProcessor.reverseMergeDerivative(TRENDS.get("Merged with Harmonic").getSeries())));
            TRENDS.put("Merged with Noise", new Trend("Merged").setSeries(
                    DataModeller.getMerged(
                            new Trend(
                                    LineType.EXPONENT,
                                    1000,
                                    0.002,
                                    30,
                                    true,
                                    ""
                            ).getSeries(),
                            new Trend(
                                    1000,
                                    10,
                                    RandomType.SYSTEM
                                    ).getSeries()
                    )));
            TRENDS.put("Reversed Trend in Noise Merge", new Trend("Reversed Trend").setSeries(
                    DataProcessor.reverseMergeAverage(TRENDS.get("Merged with Noise").getSeries())));
            TRENDS.put("Reversed Noise with Average", new Trend("Reversed Merge with Average").setSeries(
               DataAnalyzer.Statistics.getAntiSpike(DataAnalyzer.Statistics.getAntiShift(
                       DataModeller.removeTrend(TRENDS.get("Reversed Trend in Noise Merge").getSeries(),
                               TRENDS.get("Merged with Noise").getSeries())), 10.0)));
            TRENDS.put("Harmonic Fourier Spectrum",
                    new Trend("Fourier Spectrum for Harmonic").setSeries(
                        DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001)));
            TRENDS.put("Poly Harmonic Fourier Spectrum",
                    new Trend("Fourier Spectrum for Poly Harmonic").setSeries(
                        DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001)));
            TRENDS.put("Harmonic Fourier Spectrum with window size 24",
                    new Trend("Fourier Spectrum for Harmonic with window 24").setSeries(
                        DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 24)));
            TRENDS.put("Harmonic Fourier Spectrum with window size 124",
                    new Trend("Fourier Spectrum for Harmonic with window 124").setSeries(
                            DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 124)));
            TRENDS.put("Harmonic Fourier Spectrum with window size 224",
                    new Trend("Fourier Spectrum for Harmonic with window 224").setSeries(
                            DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 224)));
            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 24",
                    new Trend("Fourier Spectrum for Poly Harmonic with window 24").setSeries(
                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 24)));
            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 124",
                    new Trend("Fourier Spectrum for Poly Harmonic with window 124").setSeries(
                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 124)));
            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 224",
                    new Trend("Fourier Spectrum for Poly Harmonic with window 224").setSeries(
                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 224)));
            TRENDS.put("AntiNoise with M=1",
                    new Trend("AntiNoise with M=1").setSeries(
                            DataProcessor.antiNoise(1)));
            TRENDS.put("AntiNoise with M=10",
                    new Trend("AntiNoise with M=10").setSeries(
                            DataProcessor.antiNoise(10)));
            TRENDS.put("AntiNoise with M=100",
                    new Trend("AntiNoise with M=100").setSeries(
                            DataProcessor.antiNoise(100)));
            TRENDS.put("AntiNoise with M=1000",
                    new Trend("AntiNoise with M=1000").setSeries(
                            DataProcessor.antiNoise(1000)));
            TRENDS.put("AnalyzeAntiNoiseSTD",
                    new Trend("Anti Noise STD Analysis").setSeries(
                            DataAnalyzer.Statistics.analyzeAntiNoiseSTD(10, 1000)));
            TRENDS.put("AntiNoise from HarmMerged with M=1",
                    new Trend("AntiNoise from merged with harmonic with M=1").setSeries(
                            DataProcessor.antiNoise(1, DataModeller.getHarm(1000, 10, 5, 0.001))));
            TRENDS.put("AntiNoise from HarmMerged with M=10",
                    new Trend("AntiNoise from merged with harmonic with M=10").setSeries(
                            DataProcessor.antiNoise(10, DataModeller.getHarm(1000, 10, 5, 0.001))));
            TRENDS.put("AntiNoise from HarmMerged with M=100",
                    new Trend("AntiNoise from merged with harmonic with M=100").setSeries(
                            DataProcessor.antiNoise(100, DataModeller.getHarm(1000, 10, 5, 0.001))));
            TRENDS.put("AntiNoise from HarmMerged with M=1000",
                    new Trend("AntiNoise from merged with harmonic with M=1000").setSeries(
                            DataProcessor.antiNoise(1000, DataModeller.getHarm(1000, 10, 5, 0.001))));
        }

        public Double[] getSampleData(int N) {
            final Double[] sampleData = new Double[N];
            for (int i = 0; i < N; i++) {
                sampleData[i] = (double) i;
            }
            return sampleData;
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**", "/css/**", "/img/**", "/js/**")
                .addResourceLocations("file:" + "images" + "/", "classpath:/static/css/","classpath:/static/img/",
                        "classpath:/static/js/");
    }
}
