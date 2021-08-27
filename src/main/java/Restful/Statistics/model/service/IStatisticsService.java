package Restful.Statistics.model.service;

import Restful.Statistics.model.StatisticsRequest;
import Restful.Statistics.model.StatisticsResponse;

public interface IStatisticsService {
    boolean addStatistics(StatisticsRequest request, long timestamp);
    StatisticsResponse getStatistics(long timestamp);
    void clearCache();
}
