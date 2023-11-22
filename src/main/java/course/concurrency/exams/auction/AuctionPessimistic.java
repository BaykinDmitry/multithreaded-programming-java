package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionPessimistic implements Auction {

    private static final Object lock = new Object();

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new Bid(0L, 0L, -1L);
    }

    private volatile Bid latestBid;

    public boolean propose(Bid bid) {
    if (latestBid != bid && bid.getPrice() > latestBid.getPrice()) {
            return updateLatestBid(bid);
        }
        return false;
    }

    private boolean updateLatestBid(Bid bid) {
        synchronized (lock) {
            if (latestBid.getPrice() > bid.getPrice()) return false;
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
        }
        return true;
    }

    public Bid getLatestBid() {
        synchronized (lock) {
            return latestBid;
        }
    }
}
