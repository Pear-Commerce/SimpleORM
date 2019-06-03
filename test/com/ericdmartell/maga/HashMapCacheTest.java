package com.ericdmartell.maga;

import com.ericdmartell.maga.cache.HashMapCache;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by alexwyler on 8/3/18.
 */
public class HashMapCacheTest {

    @Test
    public void testMultiThreaded() throws InterruptedException, ExecutionException {
        int                numThreads = 100;
        final HashMapCache cache      = new HashMapCache(100);
        ExecutorService    executor   = Executors.newFixedThreadPool(numThreads);

        List<Future> futures = new ArrayList<>();
        for (int k = 0; k < 100; k++) {
            Runnable backgroundTask = () -> {
                for (int i = 1; i < 2000; i++) {
                    cache.get(String.valueOf(i % 200));
                    cache.set(String.valueOf(i % 200), i);
                    cache.get(String.valueOf(i % 200));
                }
            };

            System.out.println("About to submit the background task");
            futures.add(executor.submit(backgroundTask));
            System.out.println("Submitted the background task");
        }

        executor.shutdown();
        for (Future future : futures) {
            future.get();
        }
        executor.awaitTermination(100, TimeUnit.MINUTES);
        System.out.println("Finished the background tasks");
    }
}
