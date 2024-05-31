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
import java.util.function.Function;
import java.util.stream.IntStream;

public class Main {

    public static void main(String[] args) throws IOException {
        // TODO: Seed your randomizer
        Random rand = new Random(1);
        // TODO: Get array size and thread count from user'
        int[] cores = { 0, 1, 2, 4, 6, 8, 10 },

                data = { 1 << 14, (1 << 12) - 2000, (1 << 12) + 3000, (1 << 16) - 1, (1 << 16) + 1, (1 << 17) + 1,
                        1 << 23 };

        Scanner scanner = new Scanner(System.in);
        long startTime = 0, elapsedTime = 0;
        while (true) {
            System.out.print(" test mode [y/n]? ");
            String a = scanner.nextLine().toLowerCase();
            switch (a) {
                case "y":
                    break;

                case "n":

                    demo(rand, scanner);
                    scanner.close();
                    return;
                default:
                    continue;
            }
            break;
        }

        System.out.println("Write where:");
        // scanner.nextLine();
        String locale = scanner.nextLine();
        BufferedWriter writer = new BufferedWriter(new FileWriter(locale), 131072);
        // Test area
        scanner.close();
        int siz;
        for (int h = 1; h < 3; h++) {
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
                        String msg = String.format("\nTest %d size = %d  threads= %d took %d ms sorted? %b",
                                k, dat, siz, elapsedTime, isSorted(arr));
                        writer.write(msg);
                        // System.out.println(msg);
                    }
                    writer.write("\n Mean:" + (float) avg / 3 + " ms");
                    writer.flush();
                }

            }
        }
        writer.flush();
        writer.close();
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
        Collections.reverse(tasks);
        return tasks;
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
            p = scanner.nextInt();
        }

        startTime = System.currentTimeMillis();
        int[] arr = new int[n];
        doTasks(1 << p, rand, arr);

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
        // List<Interval> intervals = generate_intervals(0, arr.length - 1);
        // if (threads == 1) {
        // generate_intervals(0, arr.length - 1).forEach((c) -> merge(arr, c.getStart(),
        // c.getEnd()));
        // return;
        // }
        // uses generated intervals if n is power of 2 because you can easily rebuild
        // the splitting "tree"
        // performance is comparable to recursive version though
        // same case for n <32768
        // reason why we reimplemented the old reursive code was its speed and stability
        // for too large n that are none powers of 2
        // adding tings to hashmap takes too long because the hash function cannot
        // handle n > 323767

        try {
            // Slow? yes. Why? finding dependencies is O(n log n) which is the same O(n) as
            // unthreaded
            // Using Executor service basically makes this push based.

            // new Task(new Interval(0, arr.length - 1), arr, tasks);
            BufferedWriter writer = new BufferedWriter(new FileWriter("exec.txt", true), 8192 << 2);
            ThreadFactory ThreadFactory = Executors.defaultThreadFactory();

            ExecutorService pool = Executors.newFixedThreadPool(threads, ThreadFactory);
            long start = System.currentTimeMillis();
            if (threads > 1) {
                List<Task> tasks =
                        // (arr.length & -arr.length) == arr.length || arr.length < 32768
                        // // using the generated intervals is gucci for powers of 2
                        // // can someone pls parallelize tis
                        // ? threaded(arr, generate_intervals(0, arr.length - 1), pool)
                        // :
                        generateTasks(0, arr.length - 1, arr);
                Task root = tasks.get(tasks.size() - 1);
                writer.write("Dep check ");
                writer.write(System.currentTimeMillis() - start + " ms n = " + arr.length + " threads = " + threads);
                start = System.currentTimeMillis();
                tasks.removeIf(task -> task.isBase());
                tasks.forEach(t -> pool.execute(t));
                writer.write(" Exec ");

                writer.write(System.currentTimeMillis() - start + " ms");
                root.waitTask(root);
                pool.shutdownNow();
            } else {
                boolean hack = false;
                // if we want to be honest
                if (hack)
                    generate_intervals(0, arr.length - 1).forEach(t -> pool.submit(new Runnable() {
                        public void run() {
                            // TODO Auto-generated method stub
                            merge(arr, t.getStart(), t.getEnd());
                        }
                    }));
                else
                    // this is way faster. my guess is its because we only loop once and there's no
                    // task queuing overhead
                    generate_intervals(0, arr.length - 1).forEach(t -> merge(arr, t.getStart(), t.getEnd()));

                pool.shutdown();
                // wait for pool to dry.
                // yes its a spinlock. a lot of waits in java are impatient.
                // stability and speed are enemies apparently
                // learned this trick from 'ere: https://stackoverflow.com/a/1250655
                // they mentioned that malarkey happens if its not a spin lock
                // i have experienced that malarkey both in primes and here
                // this is also why a lot of examples of notify and wait in java use while loops
                // its too prevent the thread from getting impatient whether
                // 1. it took too long
                // 2. something went wrong (spurious wake up)
                // (http://opensourceforgeeks.blogspot.com/2014/08/spurious-wakeups-in-java-and-how-to.html)

                while (!pool.awaitTermination(0, TimeUnit.NANOSECONDS))
                    ;

            }
            // it is a pain to parallelize and when i did, it somehow got 5x slower
            // tldr; overhead for getting the dependency takes almost the same amount of
            // time as unthreaded mergesort. paralellizing the task is slow(much smarter to
            // probably distribute it myself but too late)
            // This is more true for non powers of 2 ergo adding the old recursive code

            // This spins my head right round...

            // while (tasks.stream().anyMatch(task -> task.isDone() == false))
            // int[] gaps = IntStream.range(1, arr.length).toArray();
            // for (int gap : gaps) {
            // tasks.stream()
            // .filter(task -> (task.getEnd() - task.getStart()) == gap)
            // .forEach(task -> pool.submit(task));

            // }
            // Spin lock or not, traversal will always have O (n log n)

            // .stream()
            // .filter(task -> !task.isDone())
            // .forEach(task -> pool.submit(task));

            // synchronized (root) {
            // while (!root.isDone())
            // root.waitTask(root);
            // }
            //
            writer.write('\n');

            writer.flush();
            writer.close();

        } catch (InterruptedException e) {
            System.err.println("Exec interrupted");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // private static ArrayList<Task> threaded(int[] arr) {
    // ArrayList<Task> tasks = new ArrayList<Task>();
    // new Task(new Interval(0, arr.length - 1), arr, tasks);
    // return tasks;
    // }

    // private static List<Task> threaded(int[] arr, List<Interval> intervals,
    // ExecutorService pool)
    // throws InterruptedException {
    // List<Task> tasks = Collections.synchronizedList(new ArrayList<Task>());
    // // the line below takes 1 second at 2^23
    // Collections.reverse(intervals);
    // intervals.forEach(i -> tasks.add(new Task(i, arr)));
    // // List<Callable<Task>> tree = new ArrayList<Callable<Task>>();
    // if ((arr.length & -arr.length) == arr.length)
    // Main.getTree(tasks, pool);
    // else
    // Main.mapTree(tasks, pool);

    // Collections.reverse(tasks);

    // return tasks;
    // }

    public static int key(int start, int end) {
        // from https://stackoverflow.com/a/13871379
        // also known as Szudzik's pairing function. starts colliding after n = (2^15)-1
        // hence the slow down around >2^15
        return (start < end ? start + end * end : start * start + start + end);
    }

    private static List<Callable<Task>> mapTree(List<Task> tasks, ExecutorService pool) {

        HashMap<Task, Task> task_map = new HashMap<Task, Task>();
        // tasks.forEach(task -> System.out.println(task));
        Task l_child, r_child;

        for (Task t : tasks) {
            // WHY IS THIS LOOP SO SLOW?
            // A:Collisions
            task_map.put(t, t);
            // System.out.println(System.currentTimeMillis() - startTime);
        }
        // System.out.println(System.currentTimeMillis() -
        // startTime);tem.currentTimeMillis() - startTime);

        for (Task task : tasks.stream().filter(task -> !task.isBase()).toList()) {
            // System.out.println(i);

            // if (task.isBase()) {
            // // System.out.println("Based\n");
            // continue;

            // }
            final int m = task.getStart() + ((task.getEnd() - task.getStart()) >> 1);
            // THANK FUCK FOR HASHMAPS
            l_child = task_map.get(new Task(new Interval(task.getStart(), m)));
            // tasks.stream()
            // .filter(
            // t -> t.getStart() == task.getStart()
            // &&
            // t.getEnd() == m)
            // .findFirst()
            // .orElse(null),
            r_child = task_map.get(new Task(new Interval(m + 1, task.getEnd())));

            task.setChildren(r_child, l_child);

        }
        return null;

    }

    private static void getTree(List<Task> tasks, ExecutorService pool) {
        // Collections.reverse(tasks);
        int left = 1, right = 2;
        // Task l_child, r_child;
        // List<Callable<Task>> tree = new ArrayList<Callable<Task>>();
        for (Task t : tasks) {

            if (!t.isBase()) {
                final Task l_child = tasks.get(left), r_child = tasks.get(right);
                t.setChildren(l_child, r_child);
                // pool.submit(new Runnable() {
                // @Override
                // public void run() {
                // // TODO Auto-generated method stub
                // t.setChildren(l_child, r_child);
                // }
                // });
            }
            left += 2;
            right += 2;
        }
        // Collections.reverse(tasks);
        // return tree;
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

    public void waitTask(Task task) throws InterruptedException {
        synchronized (task) {
            // spin locks are the easiest ways to beat spurious wake ups
            while (!task.isDone()) {
                task.wait(10);
            }
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            if (done) {
                this.notify();
                return;
                // this.interval;
            }
            /*
             * TODO: Something like this->
             * while (!left.isDone || !right.isDone){
             * left.wait();
             * right.wait();
             * }
             * why spin lock? java threads wait for no one. they can wake up early by
             * themselves
             */
            // if (!l_child.isDone() || !r_child.isDone()) {
            // return interval;
            // }
            try {
                waitTask(l_child);
                waitTask(r_child);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Main.merge(array, interval.getStart(), interval.getEnd());
            done = true;
            this.notify();
            return;
            // this.interval;
        }
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

    // Old Task implementation should reduce amount of traversals.
    Task(Interval i, int[] arr, ArrayList<Task> tasks) {
        this.interval = i;
        array = arr;
        base = done = interval.getEnd() == interval.getStart();
        if (interval.getEnd() == interval.getStart())
            l_child = r_child = null;
        else {
            int s = getStart(), e = getEnd(), m = s + (e - s) / 2;
            l_child = new Task(new Interval(getStart(), m), arr, tasks);
            r_child = new Task(new Interval(m + 1, getEnd()), arr, tasks);
            // tasks.add(l_child);
            // tasks.add(r_child);
        }
        tasks.add(this);
        // System.out.println(tasks);

    }

    public Task(Interval interval) {
        // TODO Auto-generated constructor stub
        this.interval = interval;
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

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return Main.key(getStart(), getEnd());
    }

}
// TODO: Reimplement?

class MapFinder implements Callable<Task> {
    private Task t;
    private HashMap<Integer, Task> tasks;

    public MapFinder(Task t, HashMap<Integer, Task> tasks) {
        this.t = t;
        this.tasks = tasks;
    }

    public Task call() {
        // TODO Auto-generated method
        if (t.isBase())
            return t;
        final int m = t.getStart() + ((t.getEnd() - t.getStart()) >> 1);
        Task l_child = tasks.get(Main.key(t.getStart(), m)),
                r_child = tasks.get(Main.key(m + 1, t.getEnd()));

        // System.out.println(t + " " + l_child + " " + r_child);
        t.setChildren(r_child, l_child);
        return t;
    }
}
