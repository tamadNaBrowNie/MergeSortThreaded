import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        // TODO: Seed your randomizer

        // TODO: Get array size and thread count from user'
        int[] cores = { 0, 1, 2, 3, 4 }, data = { 8, 16, 27, 31, (1 << 12) - 2331, (1 << 14) - 1, 1 << 23 };
        Scanner scanner = new Scanner(System.in);
        System.out.println("Test mode? 0 is no else yes");
        if (0 == scanner.nextInt()) {
            System.out.print("\nEnter array size N and");
            System.out.print("# of threads (its an exponent raising 2): ");
            doTasks(scanner.nextInt(), scanner.nextInt());

            scanner.close();
        } else {
            scanner.close();

            for (int h = 0; h < 5; h++) {
                System.out.println();
                for (int i = 0; i < data.length; i++) {
                    System.out.println("\n n= " + data[i]);
                    for (int j = 0; j < cores.length; j++) {
                        System.out.println("threads= " + cores[j]);
                        for (int k = 0; k < 3; k++) {
                            System.out.println("Test " + k);
                            doTasks(data[i], cores[j]);
                        }
                        System.out.println();
                    }

                }
            }
        }

        // TODO: Call the generate_intervals method to generate the merge
        // sequence
        // TODO: Call merge on each interval in sequence

        // Once you get the single-threaded version to work, it's time to
        // implement the concurrent version. Good luck :)

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

    private static void doTasks(int n, int p) {
        int seed = 0;

        Random rand = new Random(seed);

        int[] arr = new int[n];
        // System.out.println("Array done");
        if (arr.length < 1) {
            return;
        }
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
        int threads = 1 << p;
        // System.out.println("Array shuffed");
        List<Interval> intervals = generate_intervals(0, arr.length - 1);
        List<Task> tasks = new ArrayList<Task>();
        switch (threads) {
            case 1:
                intervals.forEach((c) -> merge(arr, c.getStart(), c.getEnd()));
                System.out.println("\narray sorted? " + isSorted(arr));
            case 0:
                return;
            default:
                // System.out.println("Mapping tasks");
                intervals.forEach(c -> tasks.add(new Task(c, arr)));

        }

        if ((arr.length & -arr.length) == arr.length) {
            Collections.reverse(tasks);
            int left = 1, right = 2;
            for (Task t : tasks) {

                if (t.isBase())
                    continue;

                Task l_child = tasks.get(left),
                        r_child = tasks.get(right);
                t.setChildren(r_child, l_child);
                left += 2;
                right += 2;
            }
        } else
            find_kids(tasks);

        try {
            // Slow? yes. Stupid? its not stupid if it works.
            // Using Executor service basically makes this pull based.

            ExecutorService pool = Executors.newFixedThreadPool(threads);

            while (tasks.stream().anyMatch(task -> task.isDone() == false)) {
                System.out.println("IN");
                tasks.forEach(i -> pool.execute(i));
            }
            pool.shutdown();

            // wait for pool to dry
            while (!pool.awaitTermination(0, TimeUnit.MICROSECONDS))
                ;

        } catch (InterruptedException e) {
            System.err.println("Exec interrupted");
        }
        System.out.println("\narray sorted? " + isSorted(arr));

    }

    private static int key(int start, int end) {
        // from https://stackoverflow.com/a/13871379
        return start < end ? start + end * end : start * start + start + end;
    }

    private static void find_kids(List<Task> tasks) {

        HashMap<Integer, Task> task_map = new HashMap<Integer, Task>();
        // tasks.forEach(task -> System.out.println(task));
        for (Task t : tasks) {
            task_map.put(key(t.getStart(), t.getEnd()), t);
        }

        for (int i = 0; i < tasks.size(); i++) {
            // System.out.println(i);
            Task task = tasks.get(i);
            if (task.isBase()) {
                // System.out.println("Based\n");
                continue;
            }
            final int m = task.getStart() + ((task.getEnd() - task.getStart()) >> 1);
            // THANK FUCK FOR HASHMAPS
            Task l_child = task_map.get(key(task.getStart(), m)),
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
class Task implements Runnable {
    private Interval interval;

    public Interval getInterval() {
        return interval;
    }

    private Task l_child, r_child;
    private boolean done = false;
    private int[] array;
    private boolean base;

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

    public Task getR() {
        return r_child;
    }

    public Task getL() {
        return l_child;
    }

    public void run() {

        boolean left = (l_child == null) ? true : l_child.isDone(),
                right = (r_child == null) ? true : r_child.isDone();
        if (done || !left || !right)
            return;

        Main.merge(array, interval.getStart(), interval.getEnd());
        done = true;

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
        base = interval.getEnd() == interval.getStart();
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