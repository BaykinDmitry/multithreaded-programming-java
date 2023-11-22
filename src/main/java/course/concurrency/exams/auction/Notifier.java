package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Notifier {

    ExecutorService executor = Executors.newCachedThreadPool();

    public void sendOutdatedMessage(Bid bid) {
        executor.submit(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
       try{
           executor.shutdown();
           executor.awaitTermination(1, TimeUnit.MINUTES);
       }catch (InterruptedException ex){
           executor.shutdownNow();
       }
    }
}
