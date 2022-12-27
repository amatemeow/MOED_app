package moed.application.MOED_app.components;

import lombok.Data;
import lombok.Getter;
import moed.application.MOED_app.ENUM.RandomType;
import moed.application.MOED_app.Entities.Trend;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.business.DataModeller;
import moed.application.MOED_app.business.DataProcessor;
import moed.application.MOED_app.business.IOC;
import org.jfree.data.xy.XYSeries;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.LinkedHashMap;

@Configuration
@EnableWebMvc
@ComponentScan
public class AppConfig implements WebMvcConfigurer {

    @Component
    public static class IOCConfig {
        public static final String CHART_FOLDER_WEB = "images";
        public static final String DATAFILES_FOLDER = "datafiles";
        public static final String CLEAN_PATH = CHART_FOLDER_WEB;
        public static final Integer[] DEFAULT_TREND_RATIO = {1080, 620};
        public static final Integer[] DEFAULT_HIST_RATIO = {720, 360};
    }

    @Component
    public static class SampleData {
        @Getter
        private final LinkedHashMap<String, Trend> TRENDS = new LinkedHashMap<>();

        @PostConstruct
        public void populateExam() {
            TRENDS.put("Filedata", new Trend("Data from file").setSeries(
                    DataModeller.getModel(IOC.readDat("v1v10.dat"))
            ));
            TRENDS.put("FourierFDT", new Trend("Original data spectrum", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Filedata").getSeries(), 0.001)
            ));
            TRENDS.put("AutocovarFDT", new Trend("Auto covariance of original data").setSeries(
                    DataAnalyzer.Statistics.getAutoCovariance(TRENDS.get("Filedata").getSeries())
            ));
            TRENDS.put("FourierAC", new Trend("Fourier auto covariance", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("AutocovarFDT").getSeries(), 0.001)
            ));
            TRENDS.put("HPFfdt", new Trend("HPFed original data").setSeries(
                    DataModeller.getShiftedX(
                            DataProcessor.cutEdges(
                                    DataProcessor.Convolution(
                                            DataProcessor.Filtering.HPF(50d, 0.001, 256),
                                            TRENDS.get("Filedata").getSeries()
                                    ), 300, 300), -256d)
            ));
            TRENDS.put("FourierHPF", new Trend("HPFed data spectrum", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("HPFfdt").getSeries(), 0.001)
            ));
//            TRENDS.put("AutocovarHPF", new Trend("Auto covariance of HPFed data").setSeries(
//                    DataProcessor.cutEdges(
//                            DataAnalyzer.Statistics.getNormalizedAutoCovariance(TRENDS.get("HPFfdt").getSeries()),
//                            0, 700
//                    )
//            ));
            TRENDS.put("BPFfdt", new Trend("BPFed filtered data").setSeries(
                    DataModeller.getShiftedX(
                        DataProcessor.cutEdges(
                            DataProcessor.Convolution(
                                    DataProcessor.Filtering.BPF(66d, 68d, 0.001, 1700),
                                    TRENDS.get("HPFfdt").getSeries()
                    ), 2000, 2000), -2000d)
            ));
            TRENDS.put("FourierBPF", new Trend("BPFed data spectrum", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("BPFfdt").getSeries(), 0.001)
            ));
            TRENDS.put("FourierBPFcut", new Trend("BPFed data spectrum cutted", "f, Hz", "A").setSeries(
                    DataProcessor.cutEdges(
                            DataProcessor.spectrumFourier(TRENDS.get("BPFfdt").getSeries(), 0.001),
                            17, 130
                    )
            ));


//            TRENDS.put("RT1", new Trend("Reversed Trend 1").setSeries(
//                    DataProcessor.reverseMergeAverage(TRENDS.get("Filedata").getSeries(), 20)
//            ));
//            TRENDS.put("RS1", new Trend("Reversed Signal 1").setSeries(
//                    DataModeller.removeTrend(TRENDS.get("RT1").getSeries(), TRENDS.get("Filedata").getSeries())
//            ));
//            TRENDS.put("RT2", new Trend("Reversed Trend 2").setSeries(
//                    DataProcessor.reverseMergeAverage(TRENDS.get("RS1").getSeries(), 10)
//            ));
//            TRENDS.put("RS2", new Trend("Reversed Signal 2").setSeries(
//                    DataModeller.removeTrend(TRENDS.get("RT2").getSeries(), TRENDS.get("RS1").getSeries())
//            ));
//            TRENDS.put("Deriv", new Trend("Derivative").setSeries(
//                    DataProcessor.reverseMergeDerivative(TRENDS.get("RS2").getSeries())
//            ));
//            TRENDS.put("AntiSpike", new Trend("Anti Spiked").setSeries(
//                    DataAnalyzer.getAntiSpike(TRENDS.get("Deriv").getSeries(), 50d)
//            ));
//            TRENDS.put("AntiNoise", new Trend("Anti Noise M=1000").setSeries(
//                    DataProcessor.antiNoise(1000, 1500, TRENDS.get("Deriv").getSeries())
//            ));
//            TRENDS.put("Fourierrs", new Trend("Fourier").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("RS2").getSeries(), 0.002)
//            ));
//            TRENDS.put("HPFfdt", new Trend("HPFed").setSeries(
//                    DataModeller.getShiftedX(
//                            DataProcessor.cutEdges(
//                                    DataProcessor.Convolution(
//                                            DataProcessor.Filtering.HPF(12d, 0.002, 128),
//                                            TRENDS.get("Filedata").getSeries()
//                                    ), 200, 200), -200d)
//            ));
//            TRENDS.put("Autocovarfdt", new Trend("Auto covariance file data").setSeries(
//                    DataProcessor.cutEdges(
//                            DataAnalyzer.Statistics.getNormalizedAutoCovariance(TRENDS.get("HPFfdt").getSeries()),
//                            0, 600
//                    )
//            ));
//            TRENDS.put("BPFfdt", new Trend("BPFed").setSeries(
//                    DataModeller.getShiftedX(
//                        DataProcessor.cutEdges(
//                            DataProcessor.Convolution(
//                                    DataProcessor.Filtering.BPF(26d, 27d, 0.002, 700),
//                                    TRENDS.get("HPFfdt").getSeries()
//                    ), 1000, 1000), -1000d)
//            ));
//            TRENDS.put("Fourierbpf", new Trend("Fourier BPF").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("BPFfdt").getSeries(), 0.002)
//            ));
//            TRENDS.put("Autocovarbpf", new Trend("Auto covariance BPF").setSeries(
//                    DataAnalyzer.Statistics.getNormalizedAutoCovariance(TRENDS.get("BPFfdt").getSeries())
//            ));
        }

//        @PostConstruct
        public void populateCW() {
            TRENDS.put("Main Noise", new Trend("Engine Reference Noise").setSeries(
                    DataModeller.getNoise(20000, 1d, RandomType.SYSTEM)
            ));
            TRENDS.put("BackNoise", new Trend("Background Noise").setSeries(
                    DataModeller.getNoise(20000, 0.005d, RandomType.SYSTEM)
            ));
            TRENDS.put("HighNoise", new Trend("Background Noise").setSeries(
                    DataModeller.getNoise(20000, 0.2d, RandomType.SYSTEM)
            ));
//            TRENDS.put("Noise Fourier", new Trend("Fourier spectrum for ERN").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Main Noise").getSeries(), 1/20001d)
//            ));
            TRENDS.put("ExpNegNorm", new Trend().setSeries(
                    DataProcessor.normalizeFunc(
                            DataModeller.getExponent(10000, 0.001, 0.1d, 0.5d, false),
                            1d)
            ));
            TRENDS.put("ExpPosNorm", new Trend().setSeries(
                    DataProcessor.normalizeFunc(
                            DataModeller.getExponent(10000, 0.001, 0.1d, 0.5d, true),
                            1d)
            ));
            TRENDS.put("Trendline", new Trend("Trend").setSeries(
                    DataModeller.getShiftedX(DataModeller.getMerged(
                            TRENDS.get("ExpPosNorm").getSeries(),
                            TRENDS.get("ExpNegNorm").getSeries()
                    ), -10000d)
            ));
            TRENDS.put("Mult", new Trend("ENR with trend line").setSeries(
                    DataModeller.getMultiplied(
                            TRENDS.get("Trendline").getSeries(),
                            TRENDS.get("Main Noise").getSeries()
                    )
            ));
            TRENDS.put("EngineIO", new Trend("EngineIO Noise").setSeries(
                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                            DataProcessor.Filtering.BPF(1710d, 1890d, 1/20001d, 512),
                            TRENDS.get("Mult").getSeries()
                    ), 512, 512), -512d)
            ));
//            TRENDS.put("2Filter", new Trend("EngineIO + TPA").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(961d, 1063d, 1/20001d, 512),
//                            DataProcessor.Filtering.BPF(1710d, 1890d, 1/20001d, 512)
//                    ), 512, 512), -512d)
//            ));
//            TRENDS.put("2FilterComp", new Trend("EngineIO + TPA").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("2Filter").getSeries(),
//                            TRENDS.get("Mult").getSeries()
//                    ), 1024, 1024), -1024d)
//            ));
            TRENDS.put("CompiledF", new Trend("Compiled Filter").setSeries(
                    DataModeller.getAddition(
                            DataProcessor.Filtering.BPF(961d, 1063d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(1710d, 1890d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(1292d, 1428d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(95d, 105d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(148d, 164d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(395d, 437d, 1/20001d, 512),
                            DataProcessor.Filtering.BPF(800d, 884d, 1/20001d, 512)
                    )
            ));
            TRENDS.put("Signal", new Trend("Signal").setSeries(
                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                            TRENDS.get("CompiledF").getSeries(),
                            TRENDS.get("Mult").getSeries()
                    ), 512, 512), -512d)
            ));
            TRENDS.put("AllFilterComp", new Trend("Compiled Vehicle Noise", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getAddition(
                            TRENDS.get("Signal").getSeries(),
                            TRENDS.get("BackNoise").getSeries()
                    ), 1/20000d, 1d)
            ));
            TRENDS.put("HighNoiseComp", new Trend("Compiled Vehicle with high Background Noise", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getAddition(
                            TRENDS.get("Signal").getSeries(),
                            TRENDS.get("HighNoise").getSeries()
                    ), 1/20000d, 1d)
            ));
            TRENDS.put("WheelFilter", new Trend("Compiled Wheels Filter").setSeries(
                    DataProcessor.Convolution(
                            DataProcessor.Filtering.BSF(350d, 460d, 1/20001d, 1024),
                            DataProcessor.Filtering.BSF(750d, 920d, 1/20001d, 1024)
                    )
            ));
            TRENDS.put("VehicleNoWheels", new Trend("Compiled Vehicle Noise without Wheels Noise").setSeries(
                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                            TRENDS.get("WheelFilter").getSeries(),
                            TRENDS.get("AllFilterComp").getSeries()
                    ), 1024, 1024), -1024d)
            ));
            TRENDS.put("Compiled FNoWheels", new Trend("Fourier spectrum for Compiled Vehicle Noise without Wheels Noise", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("VehicleNoWheels").getSeries(), 1/20001d)
            ));
//            TRENDS.put("Compiled FAll", new Trend("Fourier spectrum for Compiled Vehicle Noise", "f, Hz", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("AllFilterComp").getSeries(), 1/20001d)
//            ));
//            TRENDS.put("3Filter", new Trend("EngineIO + TPA + Turbo").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("2Filter").getSeries(),
//                            DataProcessor.Filtering.BPF(1292d, 1428d, 1/20001d, 512)
//                    ), 0, 0), 0d)
//            ));
//            TRENDS.put("4Filter", new Trend("EngineIO + TPA + Turbo + Cooler").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("3Filter").getSeries(),
//                            DataProcessor.Filtering.BPF(95d, 105d, 1/20001d, 512)
//                    ), 0, 0), 0d)
//            ));
//            TRENDS.put("5Filter", new Trend("EngineIO + TPA + Turbo + Cooler + Gas").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("4Filter").getSeries(),
//                            DataProcessor.Filtering.BPF(148d, 164d, 1/20001d, 512)
//                    ), 0, 0), 0d)
//            ));
//            TRENDS.put("6Filter", new Trend("EngineIO + TPA + Turbo + Cooler + Gas + WheelDef").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("5Filter").getSeries(),
//                            DataProcessor.Filtering.BPF(395d, 437d, 1/20001d, 512)
//                    ), 0, 0), 0d)
//            ));
//            TRENDS.put("7Filter", new Trend("EngineIO + TPA + Turbo + Cooler + Gas + WheelDef + WheelWeave").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("6Filter").getSeries(),
//                            DataProcessor.Filtering.BPF(800d, 884d, 1/20001d, 512)
//                    ), 0, 0), 0d)
//            ));
//            TRENDS.put("Compiled Noise", new Trend("Compiled Noise", "t,s", "Y").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("7Filter").getSeries(),
//                            TRENDS.get("Mult").getSeries()
//                    ), 512, 512), -512d)
//            ));


//            TRENDS.put("EngineIO Fourier", new Trend("Fourier spectrum for EngineIO Noise", "f, Hz", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("EngineIO").getSeries(), 1/20001d)
//            ));
//            TRENDS.put("TPA", new Trend("TPA").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(961d, 1063d, 1/20001d, 512),
//                            TRENDS.get("Mult").getSeries()
//                    ), 512, 512), -512d)
//            ));
//            TRENDS.put("Turbo", new Trend("Turbo").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(1292d, 1428d, 1/20001d, 512),
//                            TRENDS.get("Mult").getSeries()
//                    ), 512, 512), -512d)
//            ));
//            TRENDS.put("Vent", new Trend("Cooler").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(95d, 105d, 1/20001d, 256),
//                            TRENDS.get("Mult").getSeries()
//                    ), 256, 256), -256d)
//            ));
//            TRENDS.put("Gas", new Trend("Gas").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(148d, 164d, 1/20001d, 256),
//                            TRENDS.get("Mult").getSeries()
//                    ), 256, 256), -256d)
//            ));
//            TRENDS.put("WheelDef", new Trend("WheelDef").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(395d, 437d, 1/20001d, 512),
//                            TRENDS.get("Mult").getSeries()
//                    ), 512, 512), -512d)
//            ));
//            TRENDS.put("WheelWeave", new Trend("WheelWeave").setSeries(
//                    DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
//                            DataProcessor.Filtering.BPF(800d, 884d, 1/20001d, 512),
//                            TRENDS.get("Mult").getSeries()
//                    ), 512, 512), -512d)
//            ));
//            TRENDS.put("Addition", new Trend("Compiled Vehicle Noise", "t, s", "Y").setSeries(
//                    DataProcessor.alterAxis(DataModeller.getAddition(
//                            TRENDS.get("EngineIO").getSeries(),
//                            TRENDS.get("TPA").getSeries(),
//                            TRENDS.get("Turbo").getSeries(),
//                            TRENDS.get("Vent").getSeries(),
//                            TRENDS.get("Gas").getSeries(),
//                            DataProcessor.amplify(TRENDS.get("WheelDef").getSeries(), 1.5d),
//                            DataProcessor.amplify(TRENDS.get("WheelWeave").getSeries(), 1.5d),
//                            DataProcessor.amplify(TRENDS.get("BackNoise").getSeries(), 0.05d)
//                    ), 1/20000d, 1d)
//            ));
//            TRENDS.put("Compiled Fourier", new Trend("Fourier spectrum for Compiled Vehicle Noise", "f, Hz", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Addition").getSeries(), 1/20001d)
//            ));

//            TRENDS.put("Compiled Fourier", new Trend("Fourier spectrum for Compiled Vehicle Noise", "f, Hz", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Compiled Noise").getSeries(), 1/20001d)
//            ));

//            IOC.writeWav(20000, DataProcessor.amplify(TRENDS.get("Addition").getSeries(), 0.35d), "TestCW");
            IOC.writeWav(20000, DataProcessor.amplify(TRENDS.get("AllFilterComp").getSeries(), 1d), "CW_NormalNoise");
            IOC.writeWav(20000, DataProcessor.amplify(TRENDS.get("HighNoiseComp").getSeries(), 1d), "CW_HighNoise");
            IOC.writeWav(20000, DataProcessor.amplify(TRENDS.get("VehicleNoWheels").getSeries(), 1d), "CW_NoWheelsNoise");

        }
//        @PostConstruct
        public void populateTrends() {
//            TRENDS.put("Straight Positive", new Trend("Straight Positive").setSeries(DataModeller.getStraight(1000, 10d, 10d, true)));
//            TRENDS.put("Straight Negative", new Trend("Straight Negative").setSeries(DataModeller.getStraight(1000, 10d, 10d, false)));
//            TRENDS.put("Exponential Positive", new Trend("Exponential Positive").setSeries(DataModeller.getExponent(1000, 0.005, 50d, 1d, true)));
//            TRENDS.put("Exponential Negative", new Trend("Exponential Negative").setSeries(DataModeller.getExponent(1000, 0.005, 50d, 1d,false)));
//            TRENDS.put("Combined Chart", new Trend(
//                    LineType.COMBINED,
//                    1000,
//                    0.005,
//                    100,
//                    true,
//                    "Combined Chart"));
//            TRENDS.put("System Noise", new Trend("System Noise").setSeries(DataModeller.getNoise(1000, 105.15, RandomType.SYSTEM)));
//            TRENDS.put("Custom Noise", new Trend("Custom Noise").setSeries(DataModeller.getNoise(1000, 105.15, RandomType.SELF)));
//            TRENDS.put("Shifted Straight", new Trend("Shifted Straight").setSeries(DataModeller.getShiftedY(
//                    TRENDS.get("Straight Positive").getSeries(), 450d, 40, 520)));
//            TRENDS.put("Impulse Noise", new Trend("Impulse Noise").setSeries(DataModeller.getImpulseNoise(1000, 105.2, 0.006, 24.6)));
//            TRENDS.put("Harmonic", new Trend("Harmonic").setSeries(DataModeller.getHarm(1000, 100, 33, 0.001)));
//            TRENDS.put("Wrong Harmonic", new Trend("Harmonic, but f0 is wrong").setSeries(DataModeller.getHarm(1000, 100, 501, 0.001)));
//            TRENDS.put("Poly Harmonic", new Trend("Poly Harmonic").setSeries(
//                    DataModeller.getPolyHarm(1000, new int[] {100, 15, 30}, new int[] {33, 5, 170}, 0.001)));
//            TRENDS.put("Normalized Noise Covariance", new Trend("Normalized Noise Covariance").setSeries(
//                    DataAnalyzer.Statistics.getNormalizedAutoCovariance(
//                            DataAnalyzer.Statistics.getCovariance(
//                                    TRENDS.get("System Noise").getSeries(), TRENDS.get("Custom Noise").getSeries()))));
//            TRENDS.put("Normalized Harmonic Covariance", new Trend("Normalized Harmonic Covariance").setSeries(
//                    DataAnalyzer.Statistics.getNormalizedAutoCovariance(
//                            DataAnalyzer.Statistics.getCovariance(
//                                    TRENDS.get("Harmonic").getSeries(), TRENDS.get("Poly Harmonic").getSeries()))));
//            TRENDS.put("Antishift Straight", new Trend("Anti Shifted").setSeries(
//                    DataAnalyzer.Statistics.getAntiShift(TRENDS.get("Shifted Straight").getSeries())));
//            TRENDS.put("Spiked Noise", new Trend("Spiked").setSeries(
//                            DataModeller.getAddition(TRENDS.get("System Noise").getSeries(), new Trend(
//                                    LineType.IMPULSE_NOISE,
//                                    1000,
//                                    DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1],
//                                    24.6,
//                                    0.006,
//                                    RandomType.SYSTEM,
//                                    "").getSeries())));
//            TRENDS.put("Antispiked Noise", new Trend("Anti Spiked").setSeries(
//                    DataAnalyzer.Statistics.getAntiSpike(
//                            DataModeller.getAddition(TRENDS.get("System Noise").getSeries(), new Trend(
//                                    LineType.IMPULSE_NOISE,
//                                    1000,
//                                    DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1],
//                                    24.6,
//                                    0.006,
//                                    RandomType.SYSTEM,
//                                    "").getSeries()),
//                            DataAnalyzer.Statistics.getMinMax(TRENDS.get("System Noise").getSeries())[1])));
//            TRENDS.put("Merged with Harmonic", new Trend("Merged").setSeries(
//                    DataModeller.getAddition(
//                            new Trend(
//                                    LineType.SIMPLE,
//                                    1000,
//                                    0.3,
//                                    2,
//                                    true,
//                                    ""
//                            ).getSeries(),
//                            DataModeller.getHarm(
//                                    1000,
//                                    5,
//                                    250,
//                                    0.003
//                            )
//                    )));
//            TRENDS.put("Harmonic in Merged", new Trend("Harmonic in Merged").setSeries(
//                            DataModeller.getHarm(
//                                    1000,
//                                    5,
//                                    250,
//                                    0.003
//                            )));
//            TRENDS.put("Reversed Harmonic Merge", new Trend("Reverse Merge with Derivative").setSeries(
//                    DataProcessor.reverseMergeDerivative(TRENDS.get("Merged with Harmonic").getSeries())));
//            TRENDS.put("Merged with Noise", new Trend("Merged").setSeries(
//                    DataModeller.getAddition(
//                            new Trend(
//                                    LineType.EXPONENT,
//                                    1000,
//                                    0.002,
//                                    30,
//                                    true,
//                                    ""
//                            ).getSeries(),
//                            new Trend(
//                                    1000,
//                                    10,
//                                    RandomType.SYSTEM
//                                    ).getSeries()
//                    )));
//            TRENDS.put("Reversed Trend in Noise Merge", new Trend("Reversed Trend").setSeries(
//                    DataProcessor.reverseMergeAverage(TRENDS.get("Merged with Noise").getSeries())));
//            TRENDS.put("Reversed Noise with Average", new Trend("Reversed Merge with Average").setSeries(
//               DataAnalyzer.Statistics.getAntiSpike(DataAnalyzer.Statistics.getAntiShift(
//                       DataModeller.removeTrend(TRENDS.get("Reversed Trend in Noise Merge").getSeries(),
//                               TRENDS.get("Merged with Noise").getSeries())), 10.0)));
//            TRENDS.put("Harmonic Fourier Spectrum",
//                    new Trend("Fourier Spectrum for Harmonic", "f, Hz", "A").setSeries(
//                        DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001)));
//            TRENDS.put("Straight Trend Fourier Spectrum",
//                    new Trend("Fourier Spectrum for Straight Trend", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Straight Positive").getSeries(), 0.001)));
//            TRENDS.put("Harmonic Noise Fourier Spectrum",
//                    new Trend("Fourier Spectrum for Harmonic Noise", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(DataModeller.getAddition(
//                                    TRENDS.get("Harmonic").getSeries(), TRENDS.get("System Noise").getSeries()), 0.001)));
//            TRENDS.put("Impulse Noise Fourier Spectrum",
//                    new Trend("Fourier Spectrum for Impulse Noise", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Impulse Noise").getSeries(), 0.001)));
//            TRENDS.put("Poly Harmonic Fourier Spectrum",
//                    new Trend("Fourier Spectrum for Poly Harmonic", "f, Hz", "A").setSeries(
//                        DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001)));
//            TRENDS.put("Harmonic Fourier Spectrum with window size 24",
//                    new Trend("Fourier Spectrum for Harmonic with window 24", "f, Hz", "A").setSeries(
//                        DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 24)));
//            TRENDS.put("Harmonic Fourier Spectrum with window size 124",
//                    new Trend("Fourier Spectrum for Harmonic with window 124", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 124)));
//            TRENDS.put("Harmonic Fourier Spectrum with window size 224",
//                    new Trend("Fourier Spectrum for Harmonic with window 224", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Harmonic").getSeries(), 0.001, 224)));
//            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 24",
//                    new Trend("Fourier Spectrum for Poly Harmonic with window 24", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 24)));
//            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 124",
//                    new Trend("Fourier Spectrum for Poly Harmonic with window 124", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 124)));
//            TRENDS.put("Poly Harmonic Fourier Spectrum with window size 224",
//                    new Trend("Fourier Spectrum for Poly Harmonic with window 224", "f, Hz", "A").setSeries(
//                            DataProcessor.spectrumFourier(TRENDS.get("Poly Harmonic").getSeries(), 0.001, 224)));
//            TRENDS.put("AntiNoise with M=1",
//                    new Trend("AntiNoise with M=1").setSeries(
//                            DataProcessor.antiNoise(1)));
//            TRENDS.put("AntiNoise with M=10",
//                    new Trend("AntiNoise with M=10").setSeries(
//                            DataProcessor.antiNoise(10)));
//            TRENDS.put("AntiNoise with M=100",
//                    new Trend("AntiNoise with M=100").setSeries(
//                            DataProcessor.antiNoise(100)));
//            TRENDS.put("AntiNoise with M=1000",
//                    new Trend("AntiNoise with M=1000").setSeries(
//                            DataProcessor.antiNoise(1000)));
//            TRENDS.put("AnalyzeAntiNoiseSTD",
//                    new Trend("Anti Noise STD Analysis").setSeries(
//                            DataAnalyzer.Statistics.analyzeAntiNoiseSTD(10, 1000)));
//            TRENDS.put("AntiNoise from HarmMerged with M=1",
//                    new Trend("AntiNoise from merged with harmonic with M=1").setSeries(
//                            DataProcessor.antiNoise(1, DataModeller.getHarm(1000, 10, 5, 0.001))));
//            TRENDS.put("AntiNoise from HarmMerged with M=10",
//                    new Trend("AntiNoise from merged with harmonic with M=10").setSeries(
//                            DataProcessor.antiNoise(10, DataModeller.getHarm(1000, 10, 5, 0.001))));
//            TRENDS.put("AntiNoise from HarmMerged with M=100",
//                    new Trend("AntiNoise from merged with harmonic with M=100").setSeries(
//                            DataProcessor.antiNoise(100, DataModeller.getHarm(1000, 10, 5, 0.001))));
//            TRENDS.put("AntiNoise from HarmMerged with M=1000",
//                    new Trend("AntiNoise from merged with harmonic with M=1000").setSeries(
//                            DataProcessor.antiNoise(1000, DataModeller.getHarm(1000, 10, 5, 0.001))));
//            TRENDS.put("From file", new Trend("From file").setSeries(
//                    DataModeller.getModel(IOC.readDat("pgp_2ms.dat"))
//            ));
//            TRENDS.put("Fourier Spectrum from file",
//                    new Trend("Fourier Spectrum from file", "f, Hz", "A").setSeries(
//                        DataProcessor.spectrumFourier(TRENDS.get("From file").getSeries(), 0.002)
//            ));
//            TRENDS.put("Heartbeat Harm", new Trend("Heartbeat Harm").setSeries(
//                    DataModeller.getHarm(200, 1, 7, 0.005)
//            ));
//            TRENDS.put("Heartbeat Exp", new Trend("Heartbeat Exp").setSeries(
//                    DataModeller.getExponent(200, 0.005, 1d, 30d, false)
//            ));
//            TRENDS.put("Heartbeat", new Trend("Heartbeat").setSeries(
//                    DataProcessor.normalizeFunc(
//                            DataModeller.getMultiplied(
//                                    DataModeller.getHarm(200, 1, 7, 0.005),
//                                    DataModeller.getExponent(200, 0.005, 1d, 30d, false)),
//                            120d
//                    )
//            ));
//            //TRASH BLOCK (for Rhyme Function)
//                XYSeries trashSeries = DataModeller.getStraight(1000, 0d, 0d, true);
//                trashSeries.updateByIndex(200, 1d);
//                trashSeries.updateByIndex(400, 1.1);
//                trashSeries.updateByIndex(600, 1d);
//                trashSeries.updateByIndex(800, 0.9);
//            //END OF TRASH BLOCK
//            TRENDS.put("Rhyme Function", new Trend("Rhyme Function").setSeries(trashSeries));
//            TRENDS.put("Cardiogram", new Trend("Cardiogram").setSeries(
//                    DataProcessor.cutEdges(
//                            DataProcessor.Convolution(
//                                TRENDS.get("Heartbeat").getSeries(),
//                                TRENDS.get("Rhyme Function").getSeries()),
//                            0,
//                            200
//                    )));
//            TRENDS.put("LPF", new Trend("LPF").setSeries(DataProcessor.Filtering.LPF(50d, 0.002, 64)));
//            TRENDS.put("HPF", new Trend("HPF").setSeries(DataProcessor.Filtering.HPF(50d, 0.002, 64)));
//            TRENDS.put("BPF", new Trend("BPF").setSeries(DataProcessor.Filtering.BPF(35d, 75d, 0.002, 64)));
//            TRENDS.put("BSF", new Trend("BSF").setSeries(DataProcessor.Filtering.BSF(35d, 75d, 0.002, 64)));
//            TRENDS.put("Frequencies LPF", new Trend("Frequency analysis LPF").setSeries(
//                    DataAnalyzer.filterFreq(TRENDS.get("LPF").getSeries(), 0.002, 64)
//            ));
//            TRENDS.put("Frequencies HPF", new Trend("Frequency analysis HPF").setSeries(
//                    DataAnalyzer.filterFreq(TRENDS.get("HPF").getSeries(), 0.002, 64)
//            ));
//            TRENDS.put("Frequencies BPF", new Trend("Frequency analysis BPF").setSeries(
//                    DataAnalyzer.filterFreq(TRENDS.get("BPF").getSeries(), 0.002, 64)
//            ));
//            TRENDS.put("Frequencies BSF", new Trend("Frequency analysis BSF").setSeries(
//                    DataAnalyzer.filterFreq(TRENDS.get("BSF").getSeries(), 0.002, 64)
//            ));
//            TRENDS.put("Dat with LPF", new Trend("Dat with LPF").setSeries(
//                    DataProcessor.cutEdges(DataProcessor.Convolution(
//                        TRENDS.get("From file").getSeries(), DataProcessor.Filtering.LPF(1d, 0.002, 64)), 129, 129)));
//            TRENDS.put("Dat with LPF spectrum", new Trend("Dat with LPF spectrum", "f", "A").setSeries(
//               DataProcessor.spectrumFourier(TRENDS.get("Dat with LPF").getSeries(), 0.002)));
//            TRENDS.put("Dat with HPF", new Trend("Dat with HPF").setSeries(
//                    DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("From file").getSeries(), DataProcessor.Filtering.HPF(120d, 0.002, 64)), 400, 400)));
//            TRENDS.put("Dat with HPF spectrum", new Trend("Dat with HPF spectrum", "f", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Dat with HPF").getSeries(), 0.002)));
//            TRENDS.put("Dat with BPF", new Trend("Dat with BPF").setSeries(
//                    DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("From file").getSeries(), DataProcessor.Filtering.BPF(20d, 40d, 0.002, 115)), 200, 200)));
//            TRENDS.put("Dat with BPF spectrum", new Trend("Dat with BPF spectrum", "f", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Dat with BPF").getSeries(), 0.002)));
//            TRENDS.put("Dat with BSF", new Trend("Dat with BSF").setSeries(
//                    DataProcessor.cutEdges(DataProcessor.Convolution(
//                            TRENDS.get("From file").getSeries(), DataProcessor.Filtering.BSF(15d, 40d, 0.002, 128)), 400, 400)));
//            TRENDS.put("Dat with BSF spectrum", new Trend("Dat with BSF spectrum", "f", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("Dat with BSF").getSeries(), 0.002)));
//            TRENDS.put("WAV", new Trend("WAV", "t, ms", "Y").setSeries(
//                    DataProcessor.cutEdges(DataModeller.getModel(IOC.readWav("himono.wav"), 1/16d), 15000, 14000)));
//            TRENDS.put("WAV Fourier Spectrum", new Trend("WAV Fourier Spectrum", "f, Hz", "A").setSeries(
//                    DataProcessor.spectrumFourier(TRENDS.get("WAV").getSeries(), 1/16000d)));
            TRENDS.put("Rama1 orig", new Trend("Рама оригинальный 1").setSeries(
                    DataProcessor.cutEdges(DataModeller.getModel(IOC.readWav("rama1.wav")), 0, 0)));
//            TRENDS.put("Square changer 1", new Trend("Square window model").setSeries(
//                    DataModeller.squareFilter(14000, 0d,
//                            new Double[]{4050d, 8500d, 0.5},
//                            new Double[]{8600d, 12000d, 2d})));
            TRENDS.put("Rama1 changed", new Trend("Рама с измененным ударением 1").setSeries(
                    DataModeller.getMultiplied(
                            TRENDS.get("Rama1 orig").getSeries(),
                            DataModeller.squareFilter(14000, 0d,
                                    new Double[]{4050d, 8500d, 0.5},
                                    new Double[]{8600d, 12000d, 2d}))));
            IOC.writeWav(8000, TRENDS.get("Rama1 changed").getSeries(), "changedRama1");
//            TRENDS.put("Rama2 orig", new Trend("Рама оригинальный 2").setSeries(
//                    DataProcessor.cutEdges(DataModeller.getModel(IOC.readWav("rama2.wav")), 14000, 8000)));
//            TRENDS.put("Square changer 2", new Trend("Square window model").setSeries(
//                    DataModeller.squareFilter(12000, 14000d,
//                            new Double[]{14100d, 22000d, 0.5},
//                            new Double[]{22100d, 25000d, 2d})));
//            TRENDS.put("Rama2 changed", new Trend("Рама с измененным ударением 2").setSeries(
//                    DataModeller.getMultiplied(TRENDS.get("Rama2 orig").getSeries(), TRENDS.get("Square changer 2").getSeries())));
//            IOC.writeWav(16000, TRENDS.get("Rama2 changed").getSeries(), "changedRama2");
            TRENDS.put("Rama1-1", new Trend("Первый слог").setSeries(DataProcessor.cutEdges(
                    TRENDS.get("Rama1 orig").getSeries(), 4500, 5400)));
            TRENDS.put("Rama1-1 Fourier", new Trend("Спектр первого слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-1").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-1 MT", new Trend("Первый слог, основной тон", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-1").getSeries(),
                                    DataProcessor.Filtering.LPF(375d, 1/8000d, 128)),
                            256, 256), 4628d), 1/16000d, 1d)));
            TRENDS.put("Rama1-1 MT Fourier", new Trend("Спектр основного тона первого слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-1 MT").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-1 F1", new Trend("Первый слог, форманта 1", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-1").getSeries(),
                                    DataProcessor.Filtering.BPF(580d, 950d, 1/8000d, 256)),
                            512, 512), 4756d), 1/16000d, 1d)));
            TRENDS.put("Rama1-1 F1 Fourier", new Trend("Спектр форманты 1 первого слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-1 F1").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-1 F2", new Trend("Первый слог, форманта 2", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-1").getSeries(),
                                    DataProcessor.Filtering.BPF(1150d, 1370d, 1/8000d, 128)),
                            256, 256), 4628d), 1/16000d, 1d)));
            TRENDS.put("Rama1-1 F2 Fourier", new Trend("Спектр форманты 2 первого слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-1 F2").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-1 F3", new Trend("Первый слог, форманта 3", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-1").getSeries(),
                                    DataProcessor.Filtering.BPF(2400d, 2570d, 1/8000d, 256)),
                            512, 512), 4756d), 1/16000d, 1d)));
            TRENDS.put("Rama1-1 F3 Fourier", new Trend("Спектр форманты 3 первого слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-1 F3").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-2", new Trend("Второй слог").setSeries(DataProcessor.cutEdges(
                    TRENDS.get("Rama1 orig").getSeries(), 8500, 2000)));
            TRENDS.put("Rama1-2 Fourier", new Trend("Спектр второго слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-2").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-2 MT", new Trend("Второй слог, основной тон", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-2").getSeries(),
                                    DataProcessor.Filtering.LPF(350d, 1/8000d, 128)),
                            256, 256), 8628d), 1/16000d, 1d)));
            TRENDS.put("Rama1-2 MT Fourier", new Trend("Спектр основного тона второго слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-2 MT").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-2 F1", new Trend("Второй слог, форманта 1", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-2").getSeries(),
                                    DataProcessor.Filtering.BPF(570d, 850d, 1/8000d, 256)),
                            512, 512), 8756d), 1/16000d, 1d)));
            TRENDS.put("Rama1-2 F1 Fourier", new Trend("Спектр форманты 1 второго слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-2 F1").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-2 F2", new Trend("Второй слог, форманта 2", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-2").getSeries(),
                                    DataProcessor.Filtering.BPF(1170d, 1300d, 1/8000d, 256)),
                            512, 512), 8756d), 1/16000d, 1d)));
            TRENDS.put("Rama1-2 F2 Fourier", new Trend("Спектр форманты 2 второго слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-2 F2").getSeries(), 1/8000d)));
            TRENDS.put("Rama1-2 F3", new Trend("Второй слог, форманта 3", "t, s", "Y").setSeries(
                    DataProcessor.alterAxis(DataModeller.getShiftedX(DataProcessor.cutEdges(DataProcessor.Convolution(
                                    TRENDS.get("Rama1-2").getSeries(),
                                    DataProcessor.Filtering.BPF(2500d, 2650d, 1/8000d, 256)),
                            512, 512), 8756d), 1/16000d, 1d)));
            TRENDS.put("Rama1-2 F3 Fourier", new Trend("Спектр форманты 3 второго слога", "f, Hz", "A").setSeries(
                    DataProcessor.spectrumFourier(TRENDS.get("Rama1-2 F3").getSeries(), 1/8000d)));

            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-1").getSeries(), 1d), "Rama1-1");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-2").getSeries(), 1d), "Rama1-2");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-1 MT").getSeries(), 1d), "Rama1-1_MT");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-1 F1").getSeries(), 1d), "Rama1-1_F1");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-1 F2").getSeries(), 1d), "Rama1-1_F2");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-1 F3").getSeries(), 1d), "Rama1-1_F3");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-2 MT").getSeries(), 1d), "Rama1-2_MT");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-2 F1").getSeries(), 1d), "Rama1-2_F1");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-2 F2").getSeries(), 1d), "Rama1-2_F2");
            IOC.writeWav(8000, DataProcessor.amplify(TRENDS.get("Rama1-2 F3").getSeries(), 1d), "Rama1-2_F3");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**", "/css/**", "/img/**", "/js/**")
                .addResourceLocations("file:" + "images" + "/", "classpath:/static/css/","classpath:/static/img/",
                        "classpath:/static/js/");
    }
}
