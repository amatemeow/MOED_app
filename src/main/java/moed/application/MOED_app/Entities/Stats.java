package moed.application.MOED_app.Entities;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Stats {
    private final Double MIN_VALUE;
    private final Double MAX_VALUE;
    private final Double AVG_VALUE;
    private final Double DISPERSION;
    private final Double MEAN_DEVIATION;
    private final Double MEAN_SQUARE;
    private final Double ROOT_MEAN_SQUARE_ERROR;
    private final Double ASYMMETRICAL_INDEX;
    private final Double KURTOSIS_INDEX;
    private final Boolean IS_STATIONARY;
}
