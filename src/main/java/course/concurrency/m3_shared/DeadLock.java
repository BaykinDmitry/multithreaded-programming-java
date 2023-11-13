package course.concurrency.m3_shared;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DeadLock {

    private static Map<String, Integer> map = new ConcurrentHashMap<>(16);

    public static void main(String[] args) {
        Thread t1 = new Thread(()->{DeadLock.block("res1","res2");});
        Thread t2 = new Thread(()->{DeadLock.block("res2","res1");});
        t1.start();
        t2.start();
    }

    private static void block(final String a,final String b ) {
        map.computeIfAbsent(a, k -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace( System.err);
            }
            return  map.computeIfAbsent(b, l -> 5);
        });
    }



}







