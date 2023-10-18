package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        //return getMinPriceExecutorService(itemId);
        return getMinPriceCompletableFuture(itemId);
    }

    private double getMinPriceCompletableFuture(long itemId) {
        /*Тут ну очень интересная ситуация если не использовать свой ExecutorService, то проваливаются 2 и 5 тесты
         * получается что если не увеличить пул у стандартного или не использовать свой, то он просто не успевает
         * отработать все потоки за 3 секунды
         * */
        ExecutorService executorService = Executors.newCachedThreadPool();

        /*Сборка перечня CompletableFuture Для опроса магазинов*/
        List<CompletableFuture<Double>> listCF = shopIds.stream()
                .map(shopId ->
                        CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                                .orTimeout(2950, TimeUnit.MILLISECONDS)
                                .handle((v, th) -> v))
                .collect(Collectors.toList());

        /*Опрос+ расчет минимума*/
        Optional<Double> finalResult = listCF.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(v -> !Double.isNaN(v))
                .min(Double::compare);
        if (finalResult.isEmpty())
            return Double.NaN;
        return finalResult.get();
    }

    private double getMinPriceExecutorService(long itemId) {
        DoubleAccumulator minPrice = new DoubleAccumulator(Double::min, Double.POSITIVE_INFINITY);
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executorService.setCorePoolSize(10);
        executorService.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executorService.setKeepAliveTime(3000, TimeUnit.MILLISECONDS);
        for (Long shopId : shopIds) {
            executorService.submit(() -> {
                double price = priceRetriever.getPrice(itemId, shopId);
                if (!Double.isNaN(price)) minPrice.accumulate(price);
            });
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(2950, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        double price = minPrice.get();
        return Double.isInfinite(price) ? Double.NaN : price;
    }


}
