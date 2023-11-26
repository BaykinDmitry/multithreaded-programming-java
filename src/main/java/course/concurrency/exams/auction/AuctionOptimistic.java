package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid.set(new Bid(0L, 0L, -1L));
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>();

    public boolean propose(Bid bid) {
        Bid localBid;
        do {
            localBid = latestBid.get();
            if (localBid.getPrice() > bid.getPrice()) return false;
        } while (!latestBid.compareAndSet(localBid, bid));
        notifier.sendOutdatedMessage(bid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
