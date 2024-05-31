import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {

    public static final int SEED = 1;

    public static void main(String[] args) throws IOException {
        Random rand = new Random(SEED);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Test mode? 0 is no else yes: ");
        if (scanner.nextInt() == 0) {
            demo(rand, scanner);
            scanner.close();
            return;
        }

        System.out.println("Write where:");
        scanner.nextLine(); // consume newline
        String locale = scanner.nextLine();
        BufferedWriter writer = new BufferedWriter(new FileWriter(locale), 131072);
        int[] cores = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] data = {8, 27, 24, 31, 16380, (1 << 16) - 1, (1 << 23)};

        for (int dat : data) {
            for (int core : cores) {
                int siz = 1 << core;
                long totalTime = 0;
                for (int k = 0; k < 5; k++) {
                    long startTime = System.currentTimeMillis();
                    int[] arr = new int[dat];
                    doTasks(siz, rand, arr);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    totalTime += elapsedTime;
                    String msg = String.format("\nTest %d size = %d  threads = %d took %d ms sorted? %b",
                            k + 1, dat, siz, elapsedTime, isSorted(arr));
                    writer.write(msg);
                }
                writer.write("\nMean: " + (float) totalTime / 5 + " ms");
            }
        }
        writer.flush();
        writer.close();
    }

    private static void demo(Random rand, Scanner scanner) {
        System.out.print("Enter array size N and # of threads (exponent of 2): ");
        int n = scanner.nextInt();
        int p = scanner.nextInt();

        while (n < 2 || n > 1 << 23 || p < 0 || p > 10) {
            System.out.println("Bad input, try again");
            System.out.print("Enter array size N and # of threads (exponent of 2): ");
            n = scanner.nextInt();
            p = 1 << scanner.nextInt();
        }

        long startTime = System.currentTimeMillis();
        int[] arr = new int[n];
        doTasks(p, rand, arr);
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.printf("Took %d ms, array sorted? %b\n", elapsedTime, isSorted(arr));
    }

    private static boolean isSorted(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) {
                return false;
            }
        }
        return true;
    }

    private static void doTasks(int threads, Random rand, int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1;
        }
        for (int i = arr.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            int a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }

        if (threads == 1) {
            List<Interval> intervals = generate_intervals(0, arr.length - 1);
            for (Interval interval : intervals) {
                merge(arr, interval.getStart(), interval.getEnd());
            }
        } else {
            parallelMergeSort(arr, threads);
        }
    }

    private static void parallelMergeSort(int[] arr, int threads) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(arr.length);
        List<Interval> intervals = generate_intervals(0, arr.length - 1);
        for (Interval interval : intervals) {
            pool.submit(() -> {
                merge(arr, interval.getStart(), interval.getEnd());
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
    }

    public static List<Interval> generate_intervals(int start, int end) {
        List<Interval> intervals = new ArrayList<>();
        intervals.add(new Interval(start, end));
        for (int i = 0; i < intervals.size(); i++) {
            int s = intervals.get(i).getStart();
            int e = intervals.get(i).getEnd();
            if (s < e) {
                int m = s + (e - s) / 2;
                intervals.add(new Interval(s, m));
                intervals.add(new Interval(m + 1, e));
            }
        }
        Collections.reverse(intervals);
        return intervals;
    }

    public static void merge(int[] array, int s, int e) {
        int m = s + (e - s) / 2;
        int[] left = new int[m - s + 1];
        int[] right = new int[e - m];
        System.arraycopy(array, s, left, 0, left.length);
        System.arraycopy(array, m + 1, right, 0, right.length);
        int lPtr = 0, rPtr = 0;
        for (int i = s; i <= e; i++) {
            if (lPtr >= left.length) {
                array[i] = right[rPtr++];
            } else if (rPtr >= right.length || left[lPtr] <= right[rPtr]) {
                array[i] = left[lPtr++];
            } else {
                array[i] = right[rPtr++];
            }
        }
    }
}

class Interval {
    private final int start;
    private final int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
