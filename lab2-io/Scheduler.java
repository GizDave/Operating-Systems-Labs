import java.util.*;
import java.io.*;
import java.math.BigDecimal;

public class Scheduler{
    private int total_finishing_time      =0; //when all the processes have finished
    private double total_cpu_utilization  =0; //percentage of time some job is running
    private double total_io_utilization   =0; //percentage of time some job is blocked
    private double total_throughput       =0; //processes completed per hundred time units
    private double average_turnaround_time=0;
    private double average_waiting_time   =0;
    private process[] procs;
    private int num_lines=0;
    private BufferedReader br;
    public Scheduler(){
        //constructor
        try{
            //assume "random numbers" is in the same directory
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
        }
    }
    /****************************************************************************************************/
    public void FCFS(boolean verbose){
        //first come first serve
        System.out.println("FCFS()");
        process[] dp_old=procs.clone(), dp=dp_old.clone();
        Arrays.sort(dp);
        int p_num=dp.length, timer=0, i, j;
        LinkedList<process> q=new LinkedList<>(), q_old=new LinkedList<>();
        boolean io_time=false;
        process p, runner=null;
        if(verbose){
            System.out.print("Original: "); print_procs_input(dp_old);
            System.out.print("Sorted  : "); print_procs_input(dp);
        }
        while(p_num>0){
            if(verbose){
                dp_old=dp.clone();
            }
            for(i=0; i<dp.length; i++){
                p=dp[i];
                if(p.stats==status.unstarted){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    if(p.a==timer){
                        p.stats=status.ready;
                        p.ready_time=timer;
                    }
                }
                else if(p.stats==status.blocked){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    p.io_burst--;
                    io_time=true;
                    if(p.io_burst==0){
                        p.stats=status.ready;
                        p.ready_time=timer;
                    }
                }
                else if(p.stats==status.ready){
                    if(!q.contains(p)){
                        q.add(p);
                    }
                    else{
                        p.waiting_time++;
                    }
                }
            }
            if(io_time){
                total_io_utilization++;
                io_time=false;
            }
            if(runner==null){
                if(verbose)
                    q_old=(LinkedList)q.clone();
                runner=q.peek();
            }
            if(runner!=null){
                if(runner.cpu_burst==0){
                    runner.stats=status.running;
                    runner.set_cpu_burst(randomOS(runner.b));
                    total_cpu_utilization+=runner.cpu_burst;
                    runner.cpu_time-=runner.cpu_burst;
                    num_lines++;
                }
                    if(verbose){
                        try{
                            for(j=0; j<dp_old.length; j++)
                                if(dp_old[j].id==runner.id){
                                    dp_old[j]=runner.clone();
                                    break;
                                }
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                runner.cpu_burst--;
                if(runner.cpu_burst==0){
                    if(runner.cpu_time==0){
                        runner.stats=status.terminated;
                        runner.finishing_time=timer;
                        average_waiting_time+=runner.waiting_time;
                        average_turnaround_time+=(timer-runner.a);
                        p_num--;
                    }
                    else{
                        runner.stats=status.blocked;
                        runner.set_io_burst();
                        runner.io_time+=runner.io_burst;
                    }
                    q.remove(runner);
                    runner=null;
                }
            }
            if(verbose){                
                System.out.print(String.format("Before cycle %5d: ",timer));
                for(i=0; i<dp_old.length; i++){
                    if(dp_old[i].stats==status.blocked)
                        j=dp_old[i].io_burst;
                    else if(dp_old[i].stats==status.running)
                        j=dp_old[i].cpu_burst;
                    else 
                        j=0;
                    System.out.print(String.format("%10s %3d. ", dp_old[i].stats, j));
                }
                System.out.println();
            }
            timer++;
        }
        timer--;
        total_finishing_time=timer;
        total_cpu_utilization/=total_finishing_time;
        total_io_utilization/=total_finishing_time;
        average_waiting_time/=dp.length;
        average_turnaround_time/=dp.length;
        total_throughput=((double)procs.length/total_finishing_time)*100;
        print_procs_status(dp);
        print_summary();
        reset_vars();
        reset_procs();
        num_lines=0;
        try{
            br.close();
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
        }
    }
    /****************************************************************************************************/
    public void RR(boolean verbose){
        //round robin
        //quantum = 2
        System.out.println("RR()");
        process[] dp_old=procs.clone(), dp=dp_old.clone();
        Arrays.sort(dp);
        int p_num=dp.length, timer=0, i, j, quantum=2, count=quantum;
        LinkedList<process> q=new LinkedList<>(), q_old=new LinkedList<>();
        boolean io_time=false;
        process p, runner=null;
        if(verbose){
            System.out.print("Original: "); print_procs_input(dp_old);
            System.out.print("Sorted  : "); print_procs_input(dp);
        }
        while(p_num>0){
            if(verbose){
                dp_old=dp.clone();
            }
            for(i=0; i<dp.length; i++){
                p=dp[i];
                if(p.stats==status.unstarted){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    if(p.a<=timer){
                        p.stats=status.ready;
                    }
                }
                else if(p.stats==status.blocked){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    p.io_burst--;
                    io_time=true;
                    if(p.io_burst==0){
                        p.stats=status.ready;
                    }
                }
                else if(p.stats==status.ready){
                    if(!q.contains(p)){
                        q.addLast(p);
                    }
                    else{
                        p.waiting_time++;
                    }
                }
            }
            if(io_time){
                total_io_utilization++;
                io_time=false;
            }
            if(runner==null && q.size()>0){
                if(verbose)
                    q_old=(LinkedList)q.clone();
                runner=q.poll();
                if(runner.cpu_burst==0){
                    runner.stats=status.running;
                    runner.set_cpu_burst(randomOS(runner.b));
                    total_cpu_utilization+=runner.cpu_burst;
                    runner.cpu_time-=runner.cpu_burst;
                    num_lines++;
                    count=quantum;
                }
            }
            if(runner!=null){
                    if(verbose){
                        try{
                            for(j=0; j<dp_old.length; j++)
                                if(dp_old[j].id==runner.id){
                                    dp_old[j]=runner.clone();
                                    break;
                                }
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                runner.cpu_burst--;
                count--;
                if(runner.cpu_burst==0){
                    if(runner.cpu_time==0){
                        runner.stats=status.terminated;
                        runner.finishing_time=timer;
                        average_waiting_time+=runner.waiting_time;
                        average_turnaround_time+=(timer-runner.a);
                        p_num--;
                    }
                    else{
                        runner.stats=status.blocked;
                        runner.set_io_burst();
                        runner.io_time+=runner.io_burst;
                    }
                    q.remove(runner);
                    count=quantum;
                    runner=null;
                }
                else if(count==0){
                    q.remove(runner);
                    runner.stats=status.ready;
                    count=quantum;
                    runner=null;
                }
            }
            if(verbose){                
                System.out.print(String.format("Before cycle %5d: ",timer));
                for(i=0; i<dp_old.length; i++){
                    if(dp_old[i].stats==status.blocked)
                        j=dp_old[i].io_burst;
                    else if(dp_old[i].stats==status.running)
                        j=dp_old[i].cpu_burst;
                    else 
                        j=0;
                    System.out.print(String.format("%10s %3d. ", dp_old[i].stats, j));
                }
                System.out.println();
            }
            timer++;
        }
        timer--;
        total_finishing_time=timer;
        total_cpu_utilization/=total_finishing_time;
        total_io_utilization/=total_finishing_time;
        average_waiting_time/=dp.length;
        average_turnaround_time/=dp.length;
        total_throughput=((double)procs.length/total_finishing_time)*100;
        print_procs_status(dp);
        print_summary();
        reset_vars();
        reset_procs();
        num_lines=0;
        try{
            br.close();
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
        }
    }
    /****************************************************************************************************/
    public void LCFS(boolean verbose){
        //last come first serve
        System.out.println("LCFS()");
        process[] dp_old=procs.clone(), dp=dp_old.clone();
        Arrays.sort(dp);
        int p_num=dp.length, timer=0, i, j;
        LinkedList<process> q=new LinkedList<>(), q_old=new LinkedList<>();
        boolean io_time=false;
        process p, runner=null;
        if(verbose){
            System.out.print("Original: "); print_procs_input(dp_old);
            System.out.print("Sorted  : "); print_procs_input(dp);
        }
        while(p_num>0){
            if(verbose){
                dp_old=dp.clone();
            }
            for(i=0; i<dp.length; i++){
                p=dp[i];
                if(p.stats==status.unstarted){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    if(p.a==timer){
                        p.stats=status.ready;
                        p.ready_time=timer;
                    }
                }
                else if(p.stats==status.blocked){
                    if(verbose){
                        try{
                            dp_old[i]=p.clone();
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                    p.io_burst--;
                    io_time=true;
                    if(p.io_burst==0){
                        p.stats=status.ready;
                        p.ready_time=timer;
                    }
                }
                else if(p.stats==status.ready){
                    if(!q.contains(p)){
                        q.add(p);
                    }
                    else{
                        p.waiting_time++;
                    }
                }
            }
            if(io_time){
                total_io_utilization++;
                io_time=false;
            }
            if(runner==null){
                if(verbose)
                    q_old=(LinkedList)q.clone();
                Collections.sort(q);
                runner=q.peek();
            }
            if(runner!=null){
                if(runner.cpu_burst==0){
                    runner.stats=status.running;
                    runner.set_cpu_burst(randomOS(runner.b));
                    total_cpu_utilization+=runner.cpu_burst;
                    runner.cpu_time-=runner.cpu_burst;
                    num_lines++;
                }
                    if(verbose){
                        try{
                            if(verbose){
                                for(j=0; j<dp_old.length; j++)
                                    if(dp_old[j].id==runner.id){
                                        dp_old[j]=runner.clone();
                                        break;
                                    }
                            }
                        }catch(CloneNotSupportedException e){
                            e.printStackTrace();
                        }
                    }
                runner.cpu_burst--;
                if(runner.cpu_burst==0){
                    if(runner.cpu_time==0){
                        runner.stats=status.terminated;
                        runner.finishing_time=timer;
                        average_waiting_time+=runner.waiting_time;
                        average_turnaround_time+=(timer-runner.a);
                        p_num--;
                    }
                    else{
                        runner.stats=status.blocked;
                        runner.set_io_burst();
                        runner.io_time+=runner.io_burst;
                    }
                    q.remove(runner);
                    runner=null;
                }
            }
            if(verbose){                
                System.out.print(String.format("Before cycle %5d: ",timer));
                for(i=0; i<dp_old.length; i++){
                    if(dp_old[i].stats==status.blocked)
                        j=dp_old[i].io_burst;
                    else if(dp_old[i].stats==status.running)
                        j=dp_old[i].cpu_burst;
                    else 
                        j=0;
                    System.out.print(String.format("%10s %3d. ", dp_old[i].stats, j));
                }
                System.out.println();
            }
            timer++;
        }
        timer--;
        total_finishing_time=timer;
        total_cpu_utilization/=total_finishing_time;
        total_io_utilization/=total_finishing_time;
        average_waiting_time/=dp.length;
        average_turnaround_time/=dp.length;
        total_throughput=((double)procs.length/total_finishing_time)*100;
        print_procs_status(dp);
        print_summary();
        reset_vars();
        reset_procs();
        num_lines=0;
        try{
            br.close();
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
        }
    }
    /****************************************************************************************************/
    public void HPRN(boolean verbose){
        //highest penalty ratio next
        //define the denominator to be at least 1
        //HPRN is non-preemptive
        class penalty_comp implements Comparator<process> {
            //ascending, hopefully
            public int compare(process p1, process p2){
                if(p1.penalty_ratio>p2.penalty_ratio){ //this, o
                    return -1;
                }
                else if(p1.penalty_ratio==p2.penalty_ratio){
                    if(p1.a>p2.a){ //o, this
                        return 1;
                    }
                    else if(p1.a==p2.a){
                        if(p1.id>p2.id){ //o, this
                            return 1;
                        }
                        else if(p1.id==p2.id){ //p1, p2
                            return 0;
                        }
                        else //this, o
                            return -1;
                    }
                    else
                        return -1;
                }
                else{
                    return 1;
                }
            }

            public boolean equals(Object obj){
                return this==obj;
            }
        }
        System.out.println("HPRN()");
        process[] dp_old=procs.clone(), dp=dp_old.clone(), temp;
        Arrays.sort(dp);
        int p_num=dp.length, timer=0, i, j;
        PriorityQueue<process> q=new PriorityQueue<>(new penalty_comp());
        boolean io_time=false;
        process p, runner=null;
        Iterator<process> it;
        if(verbose){
            System.out.print("Original: "); print_procs_input(dp_old);
            System.out.print("Sorted  : "); print_procs_input(dp);
        }
        while(p_num>0){
            if(verbose){
                dp_old=dp.clone();
            }
            if(verbose){
                dp_old=dp.clone();
            }
            for(i=0; i<dp.length; i++){
                p=dp[i];
                if(p.stats==status.unstarted){
                    try{
                        if(verbose)
                            dp_old[i]=p.clone();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    if(p.a<=timer){
                        p.stats=status.ready;
                    }
                }
                else if(p.stats==status.blocked){
                    try{
                        if(verbose)
                            dp_old[i]=p.clone();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    p.io_burst--;
                    io_time=true;
                    if(p.io_burst==0){
                        p.stats=status.ready;
                    }
                }
                else if(p.stats==status.ready){
                    float T = (float) (timer - p.a-1);
                    int acquired_cpu = p.c - p.cpu_time;
                    float t = (float) Integer.max(1, acquired_cpu);
                    p.penalty_ratio= (float)((T)/(t));
                    if(!q.contains(p)){
                        q.add(p);
                    }
                    else{
                        q.remove(p);
                        q.add(p);
                        p.waiting_time++;
                    }
                }
            }
            if(io_time){
                total_io_utilization++;
                io_time=false;
            }
            if(runner==null){
                runner=q.poll();
            }
            if(runner!=null){
                if(runner.cpu_burst==0){
                    runner.stats=status.running;
                    runner.set_cpu_burst(randomOS(runner.b));
                    total_cpu_utilization+=runner.cpu_burst;
                    runner.cpu_time-=runner.cpu_burst;
                    num_lines++;
                }
                    try{
                        if(verbose){
                            for(j=0; j<dp_old.length; j++)
                                if(dp_old[j].id==runner.id){
                                    dp_old[j]=runner.clone();
                                    break;
                                }
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                runner.cpu_burst--;
                if(runner.cpu_burst==0){
                    if(runner.cpu_time==0){
                        runner.stats=status.terminated;
                        runner.finishing_time=timer;
                        average_waiting_time+=runner.waiting_time;
                        average_turnaround_time+=(timer-runner.a);
                        p_num--;
                    }
                    else{
                        runner.stats=status.blocked;
                        runner.set_io_burst();
                        runner.io_time+=runner.io_burst;
                    }
                    runner=null;
                }
            }
            if(verbose){                
                System.out.print(String.format("Before cycle %5d: ",timer));
                for(i=0; i<dp_old.length; i++){
                    if(dp_old[i].stats==status.blocked)
                        j=dp_old[i].io_burst;
                    else if(dp_old[i].stats==status.running)
                        j=dp_old[i].cpu_burst;
                    else 
                        j=0;
                    System.out.print(String.format("%10s %3d. ", dp_old[i].stats, j));
                }
                System.out.println();
            }
            timer++;
        }
        timer--;
        total_finishing_time=timer;
        total_cpu_utilization/=total_finishing_time;
        total_io_utilization/=total_finishing_time;
        average_waiting_time/=dp.length;
        average_turnaround_time/=dp.length;
        total_throughput=((double)procs.length/total_finishing_time)*100;
        print_procs_status(dp);
        print_summary();
        reset_vars();
        reset_procs();
        num_lines=0;
        try{
            br.close();
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
        }
    }
    /****************************************************************************************************/
    public void set_procs(process[] procs){
        this.procs=procs;
    }

    public int randomOS(int u){
        int ret;
        try{
            String line=br.readLine();
            if(u==0)
                u++;
            ret=1+(Integer.parseInt(line)%u);
        }catch(Exception e){
            System.out.println("ERROR: UNABLE TO READ 'random numbers' FILE");
            e.printStackTrace();
            return -1;
        }
        return ret;
    }

    private void reset_vars(){
        total_finishing_time   =0;
        total_cpu_utilization  =0;
        total_io_utilization   =0;
        total_throughput       =0;
        average_turnaround_time=0;
        average_waiting_time   =0;
    }

    private void reset_procs(){
        for(int i=0; i<procs.length; i++)
            procs[i].reset();
    }

    private void print_procs_input(process[] procs){
        for(int i=0; i<procs.length; i++)
            System.out.print(String.format("(%d, %d, %d, %d)",procs[i].a, procs[i].b, procs[i].c, procs[i].m));
        System.out.println();
    }

    private void print_procs_status(process[] procs){
        for(int i=0; i<procs.length; i++)
            procs[i].print_process_status();
    }

    private void print_summary(){
        System.out.println(String.format("***Summary***\n     Finishing_time=%d\n     CPU_utilization=%f\n     IO_utilization=%f\n     Throughput=%f processes per hundred cycles\n     Average_turaround_time=%f\n     Average_waiting_time=%f", total_finishing_time, total_cpu_utilization, total_io_utilization, total_throughput, average_turnaround_time, average_waiting_time));
    }

    public static void main(String[] args){
        if(args.length==0)
            return;
        Scheduler sdl=new Scheduler();
        int i, j, start=0, index=0, process_num=0;
        String temp, section;
        String[] divided;
        process[] arr;
        boolean verbose=false;
        if(args[start].equals("--verbose")){
            start++;
            verbose=true;
        }
        for(i=start; i<args.length; i++){
            try{
                Scanner file=new Scanner(new File(args[i]));
                temp=file.nextLine();
                arr=new process[Character.getNumericValue(temp.charAt(0))];
                for(j=0; j<arr.length; j++){
                    section=temp.substring(temp.indexOf("(")+1,temp.indexOf(")"));
                    temp=temp.substring(temp.indexOf(")")+1);
                    divided=section.split(" ");
                    arr[index]=new process(process_num, 
                                           Integer.parseInt(divided[0]), //a
                                           Integer.parseInt(divided[1]), //b
                                           Integer.parseInt(divided[2]), //c
                                           Integer.parseInt(divided[3]), //m
                                           section);
                    index++;
                    process_num++;
                }
                sdl.set_procs(arr);
                sdl.FCFS(verbose); //works
                System.out.println("---------------------------------------------------------");
                sdl.RR(verbose); //works
                System.out.println("---------------------------------------------------------");
                sdl.LCFS(verbose); //works
                System.out.println("---------------------------------------------------------");
                sdl.HPRN(verbose); //works
                file.close();
                index=0;
                process_num=0;
            }catch(IOException e){
                System.out.println("ERROR: UNABLE TO READ FILE '"+args[i]+"'");
                e.printStackTrace();
            }
        }
    }
}

enum status{unstarted, ready, running, blocked, terminated, preempted}

class process implements Comparable<process>, Cloneable{
    //inputs
    protected final int id, a, b, c, m;
    protected String input;
    //secondary variables
    protected int io_time, io_burst, cpu_time, cpu_burst, prev_cpu_burst=0, finishing_time, waiting_time, ready_time;
    protected float penalty_ratio=0;
    protected status stats=status.unstarted;

    //constructor
    public process(int id, int a, int b, int c, int m, String input){
        this.id=id;
        this.a=a;
        this.b=b;
        this.c=c;
        this.m=m;
        this.input=input;
        this.cpu_time=c;
    }

    //primary funcs
    protected void set_cpu_burst(int r_os){
        //assume CPU burst time is uniformly distributed random integers (UDRI)
        //the next cpu burst is randomOS(B)
        if(r_os>cpu_time)
            cpu_burst=cpu_time;
        else
            cpu_burst=r_os;
        prev_cpu_burst=cpu_burst;
    }
    protected void set_io_burst(){ 
        io_burst=prev_cpu_burst*m;
    }

    //secondary funcs
    public int compareTo(process o) {
        if(this.ready_time>o.ready_time) //this, o 
            return -1;
        else if(this.ready_time==o.ready_time){
            if(this.penalty_ratio>o.penalty_ratio){ //this, o
                return -1;
            }
            else if(this.penalty_ratio==o.penalty_ratio){
                if(this.a>o.a){ //o, this
                    return 1;
                }
                else if(this.a==o.a){
                    if(this.id>o.id){ //o, this
                        return 1;
                    }
                    else if(this.id==o.id){
                        return 0;
                    }
                    else //this, o
                        return -1;
                }
                else
                    return -1;
            }
            else{
                return 1;
            }
        }
        else //o, this
            return 1;
    }
    protected void print_process_status(){
        System.out.println(String.format("Process %d\n   (A, B, C, M) = (%d, %d, %d, %d)\n   Finishing time: %d\n   Turnaround time: %d\n   I/O time: %d\n   waiting time: %d", id, a, b, c, m, finishing_time, (finishing_time-a), io_time, waiting_time));
    }
    protected void reset(){
        io_time=io_burst=cpu_burst=prev_cpu_burst=finishing_time=waiting_time=0;
        cpu_time=c;

        penalty_ratio=0;
        stats=status.unstarted;
        ready_time=0;
    }

    protected process clone() throws CloneNotSupportedException{
        return (process)super.clone();
    }
}
