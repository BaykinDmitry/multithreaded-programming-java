package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.DoubleAccumulator;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }


    public double getMinPrice(long itemId) {
        DoubleAccumulator minPrice = new DoubleAccumulator(Double::min,Double.MAX_VALUE);
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executorService.setCorePoolSize(10);
        executorService.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executorService.setKeepAliveTime(3000, TimeUnit.MILLISECONDS);
        executorService.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (Long shopId : shopIds) {
            executorService.submit(() -> {
                double price = priceRetriever.getPrice(itemId, shopId);
                if(!Double.isNaN(price)) minPrice.accumulate(price);
            });
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(2950, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        double doubleMinPrice = minPrice.get();
        //todo: Не очень красивое решение, но поскоьку у нас цена в пределах 1000, то код вполне рабочий.
        return Double.compare(doubleMinPrice,Double.MAX_VALUE)==0?Double.NaN:doubleMinPrice;
    }

}
