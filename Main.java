import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        int seed = 0;
        // TODO: Seed your randomizer
        Random rand = new Random(seed);
        // TODO: Get array size and thread count from user'
        Scanner scanner = new Scanner(System.in);
        int[] arr = new int[scanner.nextInt()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = rand.nextInt(arr.length);
            // TODO:BUFFER THIS
            System.out.println(arr[i]);
        }
        System.out.println();
        List<Task> tasks = new ArrayList<Task>();
        int threads = scanner.nextInt();
        if (threads == 1)
            generate_intervals(0, arr.length - 1).forEach((c) -> merge(arr, c.getStart(), c.getEnd()));
        else {

            try {

                ExecutorService pool = Executors.newFixedThreadPool(threads);
                // new Task(new Interval(0, arr.length - 1), tasks, arr);
                tasks = generate_tasks(0, arr.length - 1, arr);
                while (tasks.stream().anyMatch(task -> task.isDone() == false))
                    // TODO: actually filter out unfinished tasks
                    tasks.forEach(i -> pool.execute(i));
                pool.shutdown();

                while (!pool.awaitTermination(0, TimeUnit.MICROSECONDS))
                    // wait for pool to dry
                    ;
            } catch (InterruptedException e) {
                System.err.println("Exec interrupted");
            }
        }
        System.out.println();
        for (int i : arr) {
            System.out.println(i);
        }
        scanner.close();
        // TODO: Call the generate_intervals method to generate the merge
        // sequence
        // TODO: Call merge on each interval in sequence

        // Once you get the single-threaded version to work, it's time to
        // implement the concurrent version. Good luck :)

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
    // TODO: COPY PASTE THIS THEN MODIFY TO WORK WITH Task
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

    public static List<Task> generate_tasks(int start, int end, int[] arr) {
        List<Task> frontier = new ArrayList<>();
        Task head = new Task(new Interval(start, end), arr);
        frontier.add(head);

        int i = 0;
        while (i < frontier.size()) {
            head = frontier.get(i);
            int s = head.getStart();
            int e = head.getEnd();

            i++;

            // if base case
            if (s == e) {
                continue;
            }

            // compute midpoint
            int m = s + ((e - s) >> 1);
            Task left = new Task(new Interval(s, m), arr),
                    right = new Task(new Interval(m + 1, e), arr);
            head.setChildren(left, right);
            // add prerequisite intervals
            frontier.add(left);
            frontier.add(right);
        }

        return frontier;
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

    public boolean isDone() {
        return done;
    }

    public int getStart() {
        return this.interval.getStart();
    }

    public int getEnd() {
        return this.interval.getEnd();
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
    }

    Task(Interval i, List<Task> tasks, int[] arr) {
        interval = i;
        this.array = arr;
        tasks.add(this);
        int s = i.getStart(),
                e = i.getEnd();
        if (s == e) {
            r_child = l_child = null;
            return;
        }
        int num = s + e;
        Interval left = new Interval(s, num >> 1);
        // OLD code for searching intervals
        /*
         * list.stream()
         * .filter(
         * child -> child.getStart() == i.getStart()
         * && child.getEnd() == (i.getStart() + i.getEnd()) >> 1)
         * .findFirst()
         * .orElse(null);
         */
        Interval right = new Interval((num + 2) >> 1, e);
        /*
         * list.stream()
         * .filter(child -> child.getEnd() == i.getEnd()
         * && child.getStart() == ((i.getStart() + i.getEnd()) >> 1) + 1)
         * .findFirst()
         * .orElse(null);
         */
        l_child = new Task(left, tasks, arr);
        r_child = new Task(right, tasks, arr);

    }

}