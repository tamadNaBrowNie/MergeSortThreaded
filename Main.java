import java.util.ArrayList;
import java.util.Collections;
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
        System.out.print("Enter array size N: ");
        Scanner scanner = new Scanner(System.in);
        int[] arr = new int[scanner.nextInt()];
        System.out.println("Shuffled array:");
        for (int i = 0; i < arr.length; i++) {
            arr[i] = rand.nextInt(arr.length);
            // TODO:BUFFER THIS
            // System.out.println(arr[i]);
        }

        System.out.println();
        System.out.println();

        int threads = scanner.nextInt();
        scanner.close();
        List<Interval> intervals = generate_intervals(0, arr.length - 1);
        // intervals.forEach((c) -> System.out.printf("\n%d %d\n", c.getStart(),
        // c.getEnd()));
        if (threads == 1)
            intervals.forEach((c) -> merge(arr, c.getStart(), c.getEnd()));
        else {
            List<Task> tasks = new ArrayList<Task>();
            intervals.stream().map(c -> new Task(c, arr)).forEach(d -> tasks.add(d));
            Collections.reverse(tasks);
            // The commented code snippet you provided is a loop that iterates over the list
            // of tasks
            // and sets the children for each task. Here's a breakdown of what each part of
            // the code is
            // doing:
            tasks.forEach(t -> System.out.println(t.toString()));
            System.out.println();
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                System.out.println(i);
                System.out.println(tasks.get(i).toString());
                if (task.isBase())
                    continue;
                int left = i * 2 + 1, right = i * 2 + 2;
                // if (left < tasks.size() && right < tasks.size())
                if (tasks.size() > left || tasks.size() > right)
                    task.setChildren(tasks.get(left), tasks.get(right));
                // else
                // tasks.get(i).setChildren(tasks.get(i + 1), tasks.get(i + 2));
                try {
                    System.out.println(tasks.get(i).getL().toString());
                    System.out.println(tasks.get(i).getR().toString());
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            try {

                ExecutorService pool = Executors.newFixedThreadPool(threads);

                // new Task(new Interval(0, arr.length - 1), tasks, arr);
                // tasks = generate_tasks(0, arr.length - 1, arr);
                while (tasks.stream().anyMatch(task -> task.isDone() == false))
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