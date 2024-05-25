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
            System.out.println(arr[i]);
        }
        System.out.println();
        // TODO: Generate a random array of given 
        // TODO: generate_intervals but threadedp
        List<Interval> intervals = generate_intervals(0, arr.length-1);
        ArrayList<Task> tasks = new ArrayList<Task>();
        int threads = scanner.nextInt() ;
        if(threads== 1)
            intervals.forEach((c)->merge(arr,c.getStart(),c.getEnd()));
        else
            
            new Task(intervals.get(intervals.size()-1),intervals,tasks);
        System.out.println();
        for(int i:arr){
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
    public static List<Interval> generate_intervals(int start, int end) {
        List<Interval> frontier = new ArrayList<>();
        frontier.add(new Interval(start, end));

        int i = 0;
        for (;i < frontier.size();i++) {
            int s = frontier.get(i).getStart();
            int e = frontier.get(i).getEnd();

            // i++;

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

    public int getStart () {return this.start;}

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd(){ return this.end;}

    public void setEnd(int end) {
        this.end = end;
    }
}

// Prefer composition over inheritance.
class Task implements Runnable{
    private Interval interval;
    private Task l_child, r_child;
    private boolean done=false;
    public void run(){

    }
    Task(Interval interval,Task child1,Task child2,ArrayList<Task> tasks) {
        this.interval = interval;
        this.l_child = child1;
        this.r_child = child2;
        tasks.add(this);

    }
    // TODO:RECURSIVE STATE CONSTRUCTOR THAT LOOKS FOR ITS CHILDREN. hell make it create Tasks
    Task(Interval i, List<Interval> list, List<Task> tasks ){
        interval = i;
        if(i.getStart() == i.getEnd())
        {l_child = null;
        r_child = null;}
        else{
            Interval left =list.stream()
                            .filter(
                                child->child.getStart() == i.getStart()
                                 && child.getEnd() 
                                 == (i.getStart()+i.getEnd())>>1)
                                 .findFirst()
                                 .orElse(null);
            Interval right =list.stream()
                            .filter(child->child.getEnd() == i.getEnd()
                             && child.getStart() 
                             == (
                                (i.getStart()+i.getEnd())>>1)+1)
                                .findFirst()
                                .orElse(null);
            l_child=new Task(left,list,tasks);
            r_child=new Task(right,list,tasks);

        }
        tasks.add(this);

    }
// TODO: K.I.S.S. let the number of threads be on the thread pool executor.
// class Task implements Runnable{
    
//     private State state;
//     public void run(){

//     }
    
// }
    
}