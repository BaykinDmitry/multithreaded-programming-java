package course.concurrency.exams.auction;


import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class Notifier {

    private static final ForkJoinPool executor = (ForkJoinPool) Executors.newWorkStealingPool(0x7fff);
    LongAdder inc = new LongAdder();

    public void sendOutdatedMessage(Bid bid) {
        executor.execute(this::imitateSending);
    }

    private void imitateSending() {
        try {
            inc.increment();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        try {
            executor.shutdown();
            executor.awaitTermination(50, TimeUnit.SECONDS);
            System.out.println(inc.intValue());
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
            executor.shutdownNow();
        }
    }
}
