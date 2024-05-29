import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

import java.util.function.Consumer;

public class Main {

    public static void main(String[] args) throws IOException {
        // TODO: Seed your randomizer
        Random rand = new Random(1);
        // TODO: Get array size and thread count from user'
        int[] cores = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },

                data = { 8, 16, 27, 31, 1 << 23 };

        Scanner scanner = new Scanner(System.in);
        System.out.print("Test mode? 0 is no else yes");
        long startTime = 0, elapsedTime = 0;
        if (0 == scanner.nextInt()) {
            demo(rand, scanner);
            scanner.close();
            return;
        }
        System.out.println("Write where:");
        scanner.nextLine();
        String locale = scanner.nextLine();
        BufferedWriter writer = new BufferedWriter(new FileWriter(locale), 131072);
        // Test area
        scanner.close();
        int siz;
        for (int h = 1; h < 6; h++) {
            writer.write("\n\nrun" + h);
            for (int dat : data) {
                for (int core : cores) {
                    siz = 1 << core;
                    Long avg = 0L;
                    for (int k = 1; k < 4; k++) {
                        startTime = System.currentTimeMillis();
                        int[] arr = new int[dat];
                        doTasks(siz, rand, arr);
                        elapsedTime = System.currentTimeMillis() - startTime;
                        avg += elapsedTime;
                        writer.write(String.format("\nTest %d size = %d  threads= %d took %d ms sorted? %b",
                                k, dat, siz, elapsedTime, isSorted(arr)));
                    }
                    writer.write("\n Mean:" + (float) avg / 3 + " ms");

                }

            }
        }
        writer.flush();
        writer.close();
    }

    private static void demo(Random rand, Scanner scanner) {
        long startTime;
        long elapsedTime;
        System.out.print("Enter array size N and # of threads (its an exponent raising 2): ");

        int n = scanner.nextInt(), p = scanner.nextInt();

        while (n < 2 || n > 1 << 23 || p < 0 || p > 10) {
            System.out.println("Bad input, try again");
            System.out.print("Enter array size N and # of threads (its an exponent raising 2): ");
            n = scanner.nextInt();
            p = 1 << scanner.nextInt();
        }

        startTime = System.currentTimeMillis();
        int[] arr = new int[n];
        doTasks(p, rand, arr);

        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.printf(" took %d ms array sorted? %b\n", elapsedTime, isSorted(arr));
    }

    private static boolean isSorted(int[] arr) {
        // for (int i : arr) {
        // System.out.println(i);
        // }
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1])
                return false;
        }
        return true;
    }

    private static void doTasks(int threads, Random rand, int[] arr) {

        // System.out.println("Array done");

        for (int i = 0; i < arr.length; i++)
            arr[i] = i + 1;
        for (int i = arr.length - 1; i > 0; i--) {
            // from https://stackoverflow.com/a/1520212
            int index = rand.nextInt(i + 1);
            // Simple swap
            int a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }

        // System.out.println("Array shuffed");
        List<Interval> intervals = generate_intervals(0, arr.length - 1);
        if (threads > 1)
            threaded(arr, threads, intervals);
        else
            intervals.forEach((c) -> merge(arr, c.getStart(), c.getEnd()));

    }

    private static void threaded(int[] arr, int threads, List<Interval> intervals) {
        ArrayList<Task> tasks = new ArrayList<Task>();
        intervals.forEach(i -> tasks.add(new Task(i, arr)));
        Consumer<List<Task>> func = ((arr.length & -arr.length) == arr.length) ? Main::getTree : Main::mapTree;
        func.accept(tasks);
        try {
            // Slow? yes. Stupid? its not stupid if it works.
            // Using Executor service basically makes this pull based.
            ThreadFactory ThreadFactory = Executors.defaultThreadFactory();
            ExecutorService pool = Executors.newFixedThreadPool(5, ThreadFactory);
            // ExecutorService pool = Executors.newFixedThreadPool(threads);
            // TODO: Change from spin lock
            // This spins my head right round...

            while (tasks.stream().anyMatch(task -> task.isDone() == false))
                // TODO: ACTUALLY order it as it should be executed
                pool.invokeAll(tasks.stream()
                        .filter(task -> !task.isDone()).toList());

            pool.shutdown();
            // wait for pool to dry
            while (!pool.awaitTermination(0, TimeUnit.NANOSECONDS))
                ;

        } catch (InterruptedException e) {
            System.err.println("Exec interrupted");
        }
    }

    public static int key(int start, int end) {
        // from https://stackoverflow.com/a/13871379
        return start < end ? start + end * end : start * start + start + end;
    }

    private static void mapTree(List<Task> tasks) {
        HashMap<Integer, Task> task_map = new HashMap<Integer, Task>();
        // tasks.forEach(task -> System.out.println(task));
        Task l_child, r_child;
        for (Task t : tasks) {
            task_map.put(key(t.getStart(), t.getEnd()), t);
        }

        for (Task task : tasks) {
            // System.out.println(i);

            if (task.isBase()) {
                // System.out.println("Based\n");
                continue;

            }
            final int m = task.getStart() + ((task.getEnd() - task.getStart()) >> 1);
            // THANK FUCK FOR HASHMAPS
            l_child = task_map.get(key(task.getStart(), m));
            // tasks.stream()
            // .filter(
            // t -> t.getStart() == task.getStart()
            // &&
            // t.getEnd() == m)
            // .findFirst()
            // .orElse(null),
            r_child = task_map.get(key(m + 1, task.getEnd()));

            task.setChildren(r_child, l_child);

        }

    }

    private static void getTree(List<Task> tasks) {
        Collections.reverse(tasks);
        int left = 1, right = 2;
        Task l_child, r_child;
        for (Task t : tasks) {

            if (!t.isBase()) {
                l_child = tasks.get(left);
                r_child = tasks.get(right);
                t.setChildren(r_child, l_child);
            }
            left += 2;
            right += 2;
        }

    }

    /*
     * This function generates all the intervals for merge sort iteratively, given
     * the range of indices to sort. Algorithm runs in O(n).
     * 
     * Parameters:
     * start : int - start of range
     * end : int - end of range (inclusive)
     * 
     * Returns a list of Interval objects indicating the ranges for merge sort.
     */
    public static List<Interval> generate_intervals(int start, int end) {
        List<Interval> frontier = new ArrayList<>();
        frontier.add(new Interval(start, end));

        int i = 0;
        while (i < frontier.size()) {
            int s = frontier.get(i).getStart();
            int e = frontier.get(i).getEnd();

            i++;

            // if base case
            if (s == e) {
                continue;
            }

            // compute midpoint
            int m = s + (e - s) / 2;

            // add prerequisite intervals
            frontier.add(new Interval(m + 1, e));
            frontier.add(new Interval(s, m));
        }

        List<Interval> retval = new ArrayList<>();
        for (i = frontier.size() - 1; i >= 0; i--) {
            retval.add(frontier.get(i));
        }

        return retval;
    }

    /*
     * This function performs the merge operation of merge sort.
     * 
     * Parameters:
     * array : vector<int> - array to sort
     * s : int - start index of merge
     * e : int - end index (inclusive) of merge
     */
    public static void merge(int[] array, int s, int e) {
        int m = s + (e - s) / 2;
        int[] left = new int[m - s + 1];
        int[] right = new int[e - m];
        int l_ptr = 0, r_ptr = 0;
        for (int i = s; i <= e; i++) {
            if (i <= m) {
                left[l_ptr++] = array[i];
            } else {
                right[r_ptr++] = array[i];
            }
        }
        l_ptr = r_ptr = 0;

        for (int i = s; i <= e; i++) {
            // no more elements on left half
            if (l_ptr == m - s + 1) {
                array[i] = right[r_ptr];
                r_ptr++;

                // no more elements on right half or left element comes first
            } else if (r_ptr == e - m || left[l_ptr] <= right[r_ptr]) {
                array[i] = left[l_ptr];
                l_ptr++;
            } else {
                array[i] = right[r_ptr];
                r_ptr++;
            }
        }
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
        return this.start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return this.end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}

// Prefer composition over inheritance.
class Task implements Callable<Interval> {
    private Interval interval;
    private boolean done = false;
    private int[] array;
    private boolean base;
    private Task l_child, r_child;

    public Interval getInterval() {
        return interval;
    }

    public Task getR_child() {
        return r_child;
    }

    public Task getL_child() {
        return l_child;
    }

    public boolean isBase() {
        return base;
    }

    public boolean isDone() {
        return done;
    }

    public int getStart() {
        return this.interval.getStart();
    }

    public int getEnd() {
        return this.interval.getEnd();
    }

    @Override
    public Interval call() {

        boolean left = (base) ? true : l_child.isDone(),
                right = (base) ? true : r_child.isDone();
        /*
         * TODO: Something like this->
         * while (!left.isDone || !right.isDone){
         * left.wait();
         * right.wait();
         * }
         * 
         */
        if (done || !left || !right) {
            return this.interval;
        }
        Main.merge(array, interval.getStart(), interval.getEnd());
        done = true;
        return this.interval;
    }

    public void setChildren(Task l, Task r) {
        this.l_child = l;
        this.r_child = r;
    }

    // Prefer composition over inheritance
    Task(Interval i, int[] arr) {
        this.interval = i;
        array = arr;
        l_child = r_child = null;
        base = done = interval.getEnd() == interval.getStart();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.format("%d %d", interval.getStart(), interval.getEnd());
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return super.equals(obj)
                || (((Task) obj).getStart() == this.getStart()
                        && ((Task) obj).getEnd() == this.getEnd());
    }

}
// TODO: Reimplement?
/*
 * class TreeMaker implements Callable<Task> {
 * private int l, r;
 * Task t;
 * List<Task> tasks;
 * 
 * TreeMaker(Task t, int left, int right, List<Task> tasks) {
 * this.t = t;
 * this.l = left;
 * this.r = right;
 * this.tasks = tasks;
 * }
 * 
 * @Override
 * public Task call() throws Exception {
 * if (t.isBase())
 * return t;
 * t.setChildren(tasks.get(l), tasks.get(r));
 * return t;
 * }
 * }/*
 */

/*
 * class MapFinder implements Callable<Task> {
 * private Task t;
 * private HashMap<Integer, Task> tasks;
 * 
 * public MapFinder(Task t, HashMap<Integer, Task> tasks) {
 * this.t = t;
 * this.tasks = tasks;
 * }
 * 
 * public Task call() {
 * TODO Auto-generated method
 * if (t.isBase())
 * return t;
 * final int m = t.getStart() + ((t.getEnd() - t.getStart()) >> 1);
 * Task l_child = tasks.get(Main.key(t.getStart(), m)),
 * r_child = tasks.get(Main.key(m + 1, t.getEnd()));
 * 
 * // System.out.println(t + " " + l_child + " " + r_child);
 * t.setChildren(r_child, l_child);
 * return t;
 * }
 * }
 */
