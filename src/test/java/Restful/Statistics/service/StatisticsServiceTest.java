package Restful.Statistics.service;

import Restful.Statistics.model.StatisticsRequest;
import Restful.Statistics.Application;
import Restful.Statistics.builder.StatisticsRequestBuilder;
import Restful.Statistics.model.StatisticsResponse;
import Restful.Statistics.model.service.IStatisticsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class})
public class StatisticsServiceTest {

    @Inject
    private IStatisticsService service;

    @Before
    public void init(){
        service.clearCache();
    }

    @Test
    public void testAddStatistics_withValidStats_added(){
        long current = Instant.now().toEpochMilli();
        StatisticsRequest request = StatisticsRequestBuilder.createStatisticsRequest().withAmount(1.1).withTimestamp(current).build();
        boolean added = service.addStatistics(request, current);
        Assert.assertEquals(true, added);
    }

    @Test
    public void testAddStatistics_withNegativeAmount_added(){
        long current = Instant.now().toEpochMilli();
        StatisticsRequest request = StatisticsRequestBuilder.createStatisticsRequest().withAmount(-1.1).withTimestamp(current).build();
        boolean added = service.addStatistics(request, current);
        Assert.assertEquals(true, added);
    }

    @Test
    public void testAddStatistics_withInPastTimestampMoreThanAMinute_notAdded(){
        long current = Instant.now().toEpochMilli();
        StatisticsRequest request = StatisticsRequestBuilder.createStatisticsRequest().withAmount(2.1).withTimestamp(current-60000).build();
        boolean added = service.addStatistics(request, current);
        Assert.assertEquals(false, added);
    }

    @Test
    public void testAddStatistics_withInPastTimestampWithinAMinute_created(){
        long current = Instant.now().toEpochMilli();
        StatisticsRequest request = StatisticsRequestBuilder.createStatisticsRequest().withAmount(2.1).withTimestamp(current-50000).build();
        boolean added = service.addStatistics(request, current);
        Assert.assertEquals(true, added);
    }

    @Test
    public void testGetStatistics_withAnyData_success() throws Exception{
        long timestamp = Instant.now().toEpochMilli();
        StatisticsResponse response = service.getStatistics(timestamp);
        Assert.assertEquals(0, response.getCount());
        Assert.assertEquals(0, response.getMax(), 0);
        Assert.assertEquals(0, response.getMin(), 0);
        Assert.assertEquals(0, response.getAvg(), 0);
    }

    @Test
    public void testAddAndGetStatistics_withValidTimestampMultipleThread_success() throws Exception{
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        int n = 0;
        double amount = 1.0;
        int count = 60000;
        long timestamp = Instant.now().toEpochMilli();
        long requestTime = timestamp;
        while(n<count) {
            // Time frame is managed from 0 to 59, for cache size 60.
            if(timestamp - requestTime >= 59000) {
                requestTime = timestamp;
            }
            StatisticsRequest request = StatisticsRequestBuilder.createStatisticsRequest().withAmount(amount).withTimestamp(requestTime).build();
            executorService.submit(() -> service.addStatistics(request, timestamp));
            n++;
            amount++;
            requestTime -= 1;
        }

        executorService.shutdown();
        Thread.sleep(1000);
        StatisticsResponse response = service.getStatistics(timestamp);
        Assert.assertEquals(count, response.getCount());
        Assert.assertEquals(count, response.getMax(), 0);
        Assert.assertEquals(1, response.getMin(), 0);
        Assert.assertEquals(30000.5, response.getAvg(), 0);
    }
}
