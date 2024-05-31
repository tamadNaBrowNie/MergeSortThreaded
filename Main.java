import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Test mode? 0 is no, else yes: ");
        if (scanner.nextInt() == 0) {
            demo();
            return;
        }

        System.out.println("Write where:");
        scanner.nextLine(); // consume newline
        String locale = scanner.nextLine();
        BufferedWriter writer = new BufferedWriter(new FileWriter(locale), 131072);
        int[] cores = {0, 2, 4, 6, 8, 10};
        int[] data = {8, 27, 24, 31, 16380, (1 << 16) - 1, (1 << 23)};

        for (int h = 1; h <= 2; h++) {
            writer.write("\n\nrun" + h);
            for (int dat : data) {
                for (int core : cores) {
                    int siz = 1 << core;
                    Long avg = 0L;
                    for (int k = 1; k <= 3; k++) {
                        long startTime = System.currentTimeMillis();
                        int[] arr = new int[dat];
                        doTasks(siz, arr);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        avg += elapsedTime;
                        String msg = String.format("\nTest %d size = %d  threads = %d took %d ms sorted? %b",
                                k, dat, siz, elapsedTime, isSorted(arr));
                        writer.write(msg);
                    }
                    writer.write("\nMean: " + (float) avg / 3 + " ms");
                }
            }
        }

        writer.flush();
        writer.close();
        scanner.close();
    }

    private static void demo() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter array size N and # of threads (exponent of 2): ");
        int n = scanner.nextInt();
        int p = 1 << scanner.nextInt();

        while (n < 2 || n > 1 << 23 || p < 0 || p > 10) {
            System.out.println("Bad input, try again");
            System.out.print("Enter array size N and # of threads (exponent of 2): ");
            n = scanner.nextInt();
            p = 1 << scanner.nextInt();
        }

        long startTime = System.currentTimeMillis();
        int[] arr = new int[n];
        doTasks(p, arr);
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.printf("Took %d ms. Array sorted? %b\n", elapsedTime, isSorted(arr));

        scanner.close();
    }

    private static boolean isSorted(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1])
                return false;
        }
        return true;
    }

    private static void doTasks(int threads, int[] arr) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Task> tasks = generateTasks(0, arr.length - 1, arr);

        try {
            pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pool.shutdown();
        while (!pool.isTerminated()) {
            // Wait for all tasks to finish
        }
    }

    private static List<Task> generateTasks(int start, int end, int[] arr) {
        List<Task> tasks = new ArrayList<>();
        Task head = new Task(new Interval(start, end), arr);
        tasks.add(head);

        int i = 0;
        while (i < tasks.size()) {
            head = tasks.get(i);
            int s = head.getStart();
            int e = head.getEnd();
            i++;

            if (s == e) {
                continue;
            }

            int m = s + ((e - s) >> 1);
            Task left = new Task(new Interval(s, m), arr);
            Task right = new Task(new Interval(m + 1, e), arr);
            head.setChildren(left, right);
            tasks.add(left);
            tasks.add(right);
        }

        return tasks;
    }
}

class Interval {
    private int start;
    private int end;

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

class Task implements Callable<Void> {
    private Interval interval;
    private int[] array;
    private Task leftChild;
    private Task rightChild;

    public Task(Interval interval, int[] array) {
        this.interval = interval;
        this.array = array;
    }

    public void setChildren(Task leftChild, Task rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    public int getStart() {
        return interval.getStart();
    }

    public int getEnd() {
        return interval.getEnd();
    }

    @Override
    public Void call() throws Exception {
        if (leftChild == null || rightChild == null) {
            merge(array, interval.getStart(), interval.getEnd());
        } else {
            leftChild.call();
            rightChild.call();
            merge(array, leftChild.getStart(),
            rightChild.getEnd());
        }
        return null;
    }

    private static void merge(int[] array, int start, int end) {
        int mid = (start + end) / 2;
        int[] temp = new int[end - start + 1];
        int i = start, j = mid + 1, k = 0;
        
        while (i <= mid && j <= end) {
            if (array[i] <= array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }
        
        while (i <= mid) {
            temp[k++] = array[i++];
        }
        
        while (j <= end) {
            temp[k++] = array[j++];
        }
        
        for (i = start, k = 0; i <= end; i++, k++) {
            array[i] = temp[k];
        }
    }
}
