package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Set;
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
        //return getMinPriceExecutorService(itemId);
        return getMinPriceCompletableFuture(itemId);
    }

    private double getMinPriceCompletableFuture(long itemId) {
        /*Вместо DoubleAccumulator можно было использовать коллекцию и у же в ней найти минимум,
         *Но поскольку У нас реш идет про многопоточное решение использовать DoubleAccumulator, думаю правильно
         */
        DoubleAccumulator minPrice = new DoubleAccumulator(Double::min, Double.POSITIVE_INFINITY);

        /*Тут ну очень интересная ситуация если не использовать свой ExecutorService, то проваливаются 2 и 5 тесты
        * получается что если не увеличить пул у стандартного или не использовать свой, то он просто не успевает
        * отработать все потоки за 3 секунды
        * */
        ExecutorService executorService = Executors.newCachedThreadPool();
        /*Выполнить запросы для всех магазинов */
        CompletableFuture.allOf(shopIds.stream().map(shopId ->

                  CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                        /*при получении результата из потока смотрим:
                          что есть результат
                        * нет ошибки
                        * результат определен
                        * если все условия удовлетворены то отправляем результат в накопитель*/
                        .whenComplete((v, th) -> {
                            if (v != null && th == null)
                                if (!Double.isNaN(v)) minPrice.accumulate(v);
                        })
                        /*добавляем ограничение по времени на конкретный поток*/
                        .orTimeout(2900L, TimeUnit.MILLISECONDS)
        ).toArray(CompletableFuture[]::new))
                /*ограничение по времени на все потоки 2900сек*/
                .orTimeout(2900L, TimeUnit.MILLISECONDS)
                /*подавление ошибок (в первую очередь и TimeoutException)*/
                .handle(((v, th) -> v)).join();
        double price = minPrice.get();
        return Double.isInfinite(price)?Double.NaN:price;
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
        return Double.isInfinite(price)?Double.NaN:price;
    }

}
