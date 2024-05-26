import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        // TODO: Seed your randomizer

        // TODO: Get array size and thread count from user'
        int[] cores = {}, data = {};
        Scanner scanner = new Scanner(System.in);
        System.out.println("Test mode? 0 is no else yes");
        if (0 == scanner.nextInt()) {
            System.out.print("\nEnter array size N: ");
            System.out.print("# of threads (its an exponent raising 2): ");
            doTasks(scanner.nextInt(), scanner.nextInt());
        } else {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < cores.length; j++) {
                    for (int k = 0; k < 4; k++) {
                        doTasks(i, j);
                    }
                }
            }

        }

        scanner.close();
        // TODO: Call the generate_intervals method to generate the merge
        // sequence
        // TODO: Call merge on each interval in sequence

        // Once you get the single-threaded version to work, it's time to
        // implement the concurrent version. Good luck :)

    }

    private static boolean isSorted(int[] arr) {
        for (int i : arr) {
            System.out.println(i);
        }
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
        System.out.println("Shuffled array:");
        if (arr.length < 1) {
            System.out.println("BYE");
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

        List<Interval> intervals = generate_intervals(0, arr.length - 1);
        List<Task> tasks = new ArrayList<Task>();
        switch (threads) {
            case 1:
                intervals.forEach((c) -> merge(arr, c.getStart(), c.getEnd()));
                System.out.println("\narray sorted? " + isSorted(arr));
            case 0:
                System.out.println("Bye");
                return;
            default:
                intervals.stream().map(c -> new Task(c, arr)).forEach(d -> tasks.add(d));
        }

        tasks.forEach(t -> System.out.println(t.toString()));
        System.out.println();
        if ((arr.length & -arr.length) == arr.length) {
            Collections.reverse(tasks);
            makeTree(tasks);
        } else
            find_kids(tasks);

        try {

            ExecutorService pool = Executors.newFixedThreadPool(threads);

            while (tasks.stream().anyMatch(task -> task.isDone() == false))
                tasks.forEach(i -> pool.execute(i));
            pool.shutdown();

            // wait for pool to dry
            while (!pool.awaitTermination(0, TimeUnit.MICROSECONDS))
                ;

        } catch (InterruptedException e) {
            System.err.println("Exec interrupted");
        }
        System.out.println("\narray sorted? " + isSorted(arr));

    }

    private static void find_kids(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {

            System.out.println();
            Task task = tasks.get(i);
            System.out.println(i);
            System.out.println(tasks.get(i).toString());
            if (task.isBase()) {
                System.out.println("Based\n");
                continue;
            }
            final int m = task.getStart() + ((task.getEnd() - task.getStart()) >> 1);
            // STUPID AHCK
            Task l_child = tasks.stream()
                    .filter(
                            t -> t.getStart() == task.getStart()
                                    &&
                                    t.getEnd() == m)
                    .findFirst()
                    .orElse(null),
                    r_child = tasks.stream()
                            .filter(
                                    t -> t.getStart() == m + 1
                                            &&
                                            t.getEnd() == task.getEnd())
                            .findFirst()
                            .orElse(null);
            System.out.println(tasks.indexOf(r_child));
            System.out.println(tasks.indexOf(l_child));
            System.out.println();
            task.setChildren(r_child, l_child);

        }
    }

    private static void makeTree(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isBase())
                continue;
            int left = i * 2 + 1, right = i * 2 + 2;
            Task l_child = tasks.get(left),
                    r_child = tasks.get(right);
            tasks.get(i).setChildren(r_child, l_child);
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

}