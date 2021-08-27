package Restful.Statistics.model.service.impl;

import Restful.Statistics.model.service.IStatisticsService;
import Restful.Statistics.builder.StatisticsResponseBuilder;
import Restful.Statistics.cache.StatisticsCache;
import Restful.Statistics.model.Statistics;
import Restful.Statistics.model.StatisticsRequest;
import Restful.Statistics.model.StatisticsResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

import static Restful.Statistics.util.Constants.ONE_MINUTE_IN_MS;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Inject
    private final StatisticsCache<Long, Statistics> cache;

    @Inject
    public StatisticsServiceImpl(StatisticsCache<Long, Statistics> cache) {
        this.cache = cache;
    }

    public boolean addStatistics(StatisticsRequest request, long timestamp) {
        long requestTime = request.getTimestamp();
        long delay = timestamp - requestTime;
        if (delay < 0 || delay >= ONE_MINUTE_IN_MS) {
            return false;
        } else {
            Long key = getKeyFromTimestamp(requestTime);
            Statistics s = cache.get(key);
            if(s == null) {
                synchronized (cache) {
                    s = cache.get(key);
                    if (s == null) {
                        s = new Statistics();
                        cache.put(key, s);
                    }
                }
            }
            s.updateStatistics(request.getAmount());
        }
        return true;
    }

    public StatisticsResponse getStatistics(long timestamp) {
        Map<Long, Statistics> copy = cache.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatistics()));
        return getStatisticsFromCacheCopy(copy, timestamp);
    }

    private StatisticsResponse getStatisticsFromCacheCopy(Map<Long, Statistics> copy, long timestamp) {
        double sum = 0;
        double avg = 0;
        double max = 0;
        double min = Double.MAX_VALUE;
        long count = 0;
        Long key = getKeyFromTimestamp(timestamp);

        for (Map.Entry<Long, Statistics> e : copy.entrySet()) {
            Long eKey = e.getKey();
            Long timeFrame = key - eKey;
            if(timeFrame >= 0 && timeFrame < cache.getCapacity()) {
                Statistics eValue = e.getValue();
                if(eValue.getCount() > 0) {
                    sum += eValue.getSum();
                    min = min < eValue.getMin() ? min : eValue.getMin();
                    max = max > eValue.getMax() ? max : eValue.getMax();
                    count += eValue.getCount();
                }
            }
        }
        if(count == 0) {
            min = 0;
            avg = 0;
        } else {
            avg = sum / count;
        }

        return StatisticsResponseBuilder.createStatisticsResponse().withSum(sum).withAvg(avg).withMax(max).withMin(min).withCount(count).build();
    }

    private Long getKeyFromTimestamp(Long timestamp) {
        return (timestamp * cache.getCapacity()) / ONE_MINUTE_IN_MS;
    }

    public void clearCache() {
        cache.clear();
    }
}
