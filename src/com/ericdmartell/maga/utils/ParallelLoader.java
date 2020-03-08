package com.ericdmartell.maga.utils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParallelLoader {


    public static AtomicInteger ACTIVE_THREADS = new AtomicInteger(0);

    public static int DEFAULT_NUM_THREADS = 10;

    public static <T, S> List<S> load(int numThreads, Collection<T> input, Function<T, S> map1) {
        return load(numThreads, input, map1, Function.identity());
    }

    public static <U, T, S> List<S> load(int numThreads, Collection<U> input, Function<U, T> map1, Function<T, S> map2) {
        return load(numThreads, input, map1, map2, Function.identity());
    }

    public static <T, S, K> Map<K, S> loadMapped(Collection<T> input, Function<T, K> keyMap, Function<T, S> map1) {
        return loadMapped(DEFAULT_NUM_THREADS, input, keyMap, map1);
    }

    public static <T, S> Map<T, S> loadMapped(Collection<T> input, Function<T, S> map1) {
        return loadMapped(DEFAULT_NUM_THREADS, input, map1);
    }

    public static <T, S> List<S> load(Collection<T> input, Function<T, S> map1) {
        return load(DEFAULT_NUM_THREADS, input, map1);
    }

    public static <T> void process(Collection<T> input, final Consumer<T> consumer) {
        load(DEFAULT_NUM_THREADS, input, t -> {
            consumer.accept(t);
            return t;
        });
    }

    public static <T, S> Map<T, S> loadMapped(int numThreads, Collection<T> input, Function<T, S> map1) {
        return loadMapped(numThreads, input, Function.identity(), map1);
    }

    public static <T, S, K> Map<K, S> loadMapped(int numThreads, Collection<T> input, Function<T, K> keyMap, Function<T, S> map1) {
        List<T> inputList = input instanceof List ? (List) input : new ArrayList<>(input);
        List<S> loaded = load(numThreads, inputList, map1);
        Map<K, S> mapped = new HashMap<>();
        for (int i = 0; i < inputList.size(); i++) {
            mapped.put(keyMap.apply(inputList.get(i)), loaded.get(i));
        }
        return mapped;
    }

    public static <U, T, S> List<S> load(Collection<U> input, Function<U, T> map1, Function<T, S> map2) {
        return load(DEFAULT_NUM_THREADS, input, map1, map2);
    }

    public static <U, T, S, R> List<R> load(Collection<U> input, Function<U, T> map1, Function<T, S> map2, Function<S, R> map3) {
        return load(DEFAULT_NUM_THREADS, input, map1, map2, map3);
    }

    public static <U, T, S, R> List<R> load(int numThreads, Collection<U> input, Function<U, T> map1, Function<T, S> map2, Function<S, R> map3) {

        ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
        ACTIVE_THREADS.addAndGet(numThreads);
        try {
            List<R> results = (forkJoinPool.submit(() -> input.parallelStream().map(map1).map(map2).map(map3).collect(Collectors.toList())).get());
            ACTIVE_THREADS.addAndGet(-numThreads);
            return results;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


}