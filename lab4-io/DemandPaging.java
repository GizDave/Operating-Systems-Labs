import java.util.*;
import java.io.*;

class Process{
	protected final double a, b, c, d;
	protected double average_residency=0;
	protected int eviction=0, page_fault=0, w=0;
	public Process(double a, double b, double c){
		this.a=a;
		this.b=b;
		this.c=c;
		this.d=1-a-b-c;
	}
}
/*no reusability*/
public class DemandPaging{
	//primary variables
	private final int machine_size, page_size, process_size, job_mix, reference_pp, quantum=3;
	private final String algo;
	//secondary variables
	private int[][] frame_table;
	private Process[] procs;
	private int p_num, timer=0, runner=1;
	private BufferedReader br;
	private Random rand=new Random();
	//constructor
	public DemandPaging(int m, int p, int s, int job, int n, String r){
		//m, p, s, j, n, r
        try{
            br = new BufferedReader(new FileReader(new File("random numbers")));
        }catch(IOException e){
            System.out.println("ERROR: UNABLE TO FIND 'random numbers' FILE IN THE CURRENT DIRECTORY");
            e.printStackTrace();
        }
        //initialize variables
		this.machine_size=m;
		this.page_size=p;
		this.process_size=s;
		this.job_mix=job;
		this.reference_pp=n;
		this.algo=r;
		frame_table=new int[machine_size/page_size][5];
		//initialize procs based on job mix number
		switch(job_mix){
			case 1: procs=new Process[]{new Process(1, 0, 0)};
					procs[0].w=(111*(1)+process_size)%process_size;
					break;
			case 2:	procs=new Process[]{new Process(1, 0, 0), new Process(1, 0, 0), new Process(1, 0, 0), new Process(1, 0, 0)};
					procs[0].w=(111*(1)+process_size)%process_size;
					procs[1].w=(111*(2)+process_size)%process_size;
					procs[2].w=(111*(3)+process_size)%process_size;
					procs[3].w=(111*(4)+process_size)%process_size;
					break;
			case 3: procs=new Process[]{new Process(0, 0, 0), new Process(0, 0, 0), new Process(0, 0, 0), new Process(0, 0, 0)};
					procs[0].w=(111*(1)+process_size)%process_size;
					procs[1].w=(111*(2)+process_size)%process_size;
					procs[2].w=(111*(3)+process_size)%process_size;
					procs[3].w=(111*(4)+process_size)%process_size;
					break;
			case 4: procs=new Process[]{new Process(0.75, 0.25, 0), new Process(0.75, 0, 0.25), new Process(0.75, 0.125, 0.125), new Process(0.5, 0.125, 0.125)};
					procs[0].w=(111*(1)+process_size)%process_size;
					procs[1].w=(111*(2)+process_size)%process_size;
					procs[2].w=(111*(3)+process_size)%process_size;
					procs[3].w=(111*(4)+process_size)%process_size;
					break;
		}
		//mark all page frames as available
		for(int i=0; i<frame_table.length; i++)
			frame_table[i][0]=1;
		//run
		System.out.println(String.format("The machine size is %d.\nThe page size is %d.\nThe process size is %d.\nThe job mix number is %d.\nThe number of references per process is %d.\nThe replacement algorithm is %s.\n", machine_size, page_size, process_size, job_mix, reference_pp, algo));
		try{
			driver();
		}catch(IOException e){
			System.out.println("Driver() IOException");
			e.printStackTrace();
		}
	}
	//generate memory references with lab 2 "random number" to generate random numbers
	private void driver() throws IOException{
		int i=0, j, k, l, reference, final_fault=0, final_eviction=0;
		double y=0, final_average_residency=0;
		while(i<reference_pp){
			l=Math.min(quantum, reference_pp-i);
			for(j=0; j<procs.length; j++){
				for(k=0; k<l; k++){
					//simulate current reference for (j+1)th process
					//System.out.print(String.format("%d references word %d ", j+1, procs[j].w));
					pager(j, procs[j].w/page_size);
					//caclulate the next reference for this process
					y=(Integer.parseInt(br.readLine()))/(Integer.MAX_VALUE+1d);
					if(y<procs[j].a)
						//case1: references are to the address one higher than the current (representing a sequential memory reference)
						reference=(procs[j].w+1+process_size)%process_size;
					else if(y<(procs[j].a+procs[j].b))
						//case2: nearby lower address (representing a backward branch)
						reference=(procs[j].w-5+process_size)%process_size;
					else if(y<(procs[j].a+procs[j].b+procs[j].c))
						//case3: nearby higher address (representing a jump around a ‘‘then’’ or ‘‘else’’ block)
						reference=(procs[j].w+4+process_size)%process_size;
					else
						//case4: random addresses
						reference=(Integer.parseInt(br.readLine())+process_size)%process_size;
					procs[j].w=reference;
					timer++;
				}
				//System.out.println();
			}
			//System.out.println("----------Cycle Ends----------");
			i+=l;
		}
		//print process and overall summaries
		//System.out.println();
		for(i=0; i<procs.length; i++){
			final_fault+=procs[i].page_fault;
			final_eviction+=procs[i].eviction;
			final_average_residency+=procs[i].average_residency;
			System.out.println(String.format("Process %d has %d faults and %s average_residency.", i+1, procs[i].page_fault, (procs[i].eviction>0)?Double.toString(procs[i].average_residency/procs[i].eviction):"undefined"));
		}
		System.out.println(String.format("\nThe total number of faults is %d and the overall average residency is %s.", final_fault, (final_eviction>0)?Double.toString(final_average_residency/final_eviction):"undefined"));
	}
	/*frame table entry*/ //(0) available_bit, (1) task_id, (2) page_id, (3)load_time, (4) last_reference_time;
	//page word if possible. else, call the algorithms
	public void pager(int task, int page_num){
		//System.out.print(String.format("(page %d) at time %d: ", page_num, timer+1));
		int hand;
		//check if the page is in frame table
		for(hand=0; hand<frame_table.length; hand++){
			if(frame_table[hand][0]==0 && (frame_table[hand][1]==task && frame_table[hand][2]==page_num)){
				//System.out.println(String.format("Hit in frame %d", hand));
				frame_table[hand][4]=timer;
				return;
			}
		}
		procs[task].page_fault++;
		//check if there is a free frame
		//System.out.print("Fault, ");
		for(hand=frame_table.length-1; hand>-1; hand--)
			if(frame_table[hand][0]==1){
				//System.out.println(String.format("using free frame %d.", hand));
				frame_table[hand]=new int[]{0, task, page_num, timer, timer};
				return;
			}
		//use a page replacement algorithm
		switch(algo){
			case "lru":		lru_algorithm(task, page_num);
							break;
			case "lifo":	lifo_algorithm(task, page_num);
							break;
			case "random":	random_algorithm(task, page_num);
							break;
		}
	}
	/*
		Page Replacement Algorithms
	*/
	/*frame table entry*/ //(0) available_bit, (1) task_id, (2) page_id, (3)load_time, (4) last_reference_time;
	//use a linkedlist to keep track of the pages. the head is least recent and the tail is most recent. remove tail at page fault
	public void lifo_algorithm(int task, int page_num){
		//search
		int mr_index=0;
		for(int x=frame_table.length-1; x>-1; x--)
			if(frame_table[x][3]>frame_table[mr_index][3])
				mr_index=x;
		//replacement
		//System.out.println(String.format("evicting page %d of %d from frame %d.", frame_table[mr_index][2], frame_table[mr_index][1]+1, mr_index));
		procs[frame_table[mr_index][1]].eviction++;
		procs[frame_table[mr_index][1]].average_residency+=(timer-frame_table[mr_index][3]);
		frame_table[mr_index]=new int[]{0, task, page_num, timer, timer};
	}
	/*frame table entry*/ //(0) available_bit, (1) task_id, (2) page_id, (3)load_time, (4) last_reference_time;
	//evict a random page
	//problems: average residency calculation
	public void random_algorithm(int task, int page_num){
		//search
		int mr_index=0;
		try{
			mr_index=(Integer.parseInt(br.readLine())+frame_table.length)%frame_table.length; 
		}catch(IOException e){
			e.printStackTrace();
		}
		//replacement
		//System.out.println(String.format("evicting page %d of %d from frame %d.", frame_table[mr_index][2], frame_table[mr_index][1]+1, mr_index));
		procs[frame_table[mr_index][1]].eviction++;
		procs[frame_table[mr_index][1]].average_residency+=(timer-frame_table[mr_index][3]);
		frame_table[mr_index]=new int[]{0, task, page_num, timer, timer};
	}
	/*frame table entry*/ //(0) available_bit, (1) task_id, (2) page_id, (3)load_time, (4) last_reference_time;
	//evict the page that has been unused for the longest time.
	//problems: average residency calculation
	public void lru_algorithm(int task, int page_num){
		//search
		int mr_index=0; 
		for(int x=frame_table.length-1; x>-1; x--)
			if((timer-frame_table[x][4])>(timer-frame_table[mr_index][4]))
				mr_index=x;
		//replacement
		//System.out.println(String.format("evicting page %d of %d from frame %d.", frame_table[mr_index][2], frame_table[mr_index][1]+1, mr_index));
		procs[frame_table[mr_index][1]].eviction++;
		procs[frame_table[mr_index][1]].average_residency+=(timer-frame_table[mr_index][3]);
		//System.out.println("	evicted page residency is now "+procs[frame_table[mr_index][1]].average_residency);
		frame_table[mr_index]=new int[]{0, task, page_num, timer, timer};
	}
	//auxillary function for testing
	public static String[] get_input(int i){
		switch(i){
			case 1:  return new String[]{"10",   "10", "20",  "1", "10",   "lru"   };
			case 2:  return new String[]{"10",   "10", "10",  "1", "100",  "lru"   };
			case 3:  return new String[]{"10",   "10", "10",  "2", "10",   "lru"   };
			case 4:  return new String[]{"20",   "10", "10",  "2", "10",   "lru"   };
			case 5:  return new String[]{"20",   "10", "10",  "2", "10",   "random"};
			case 6:  return new String[]{"20",   "10", "10",  "2", "10",   "lifo"  };
			case 7:  return new String[]{"20",   "10", "10",  "3", "10",   "lru"   };
			case 8:  return new String[]{"20",   "10", "10",  "3", "10",   "lifo"  };
			case 9:  return new String[]{"20",   "10", "10",  "4", "10",   "lru"   };
			case 10: return new String[]{"20",   "10", "10",  "4", "10",   "random"};
			case 11: return new String[]{"90",   "10", "40",  "4", "100",  "lru"   };
			case 12: return new String[]{"40",   "10", "90",  "1", "100",  "lru"   };
			case 13: return new String[]{"40",   "10", "90",  "1", "100",  "lifo"  }; 
			case 14: return new String[]{"800",  "40", "400", "4", "5000", "lru"   }; 
			case 15: return new String[]{"10",   "5",  "30",  "4", "3",    "random"}; 
			case 16: return new String[]{"1000", "40", "400", "4", "5000", "lifo"  }; 
		}
		return null;
	}
	//main function
	public static void main(String[] args){
		System.out.println();
		DemandPaging dp;
		if(args.length==1){
			int[] collection={};
			if(args[0].equals("lru"))
				collection=new int[]{1,2,3,4,7,9,11,12,14};
			else if(args[0].equals("random"))
				collection=new int[]{5,10,15};
			else if(args[0].equals("lifo"))
				collection=new int[]{6,8,13,16};
			for(int t=0; t<collection.length; t++){
				System.out.println(String.format("run-%d-debug", collection[t]));
				args=DemandPaging.get_input(collection[t]);
				dp=new DemandPaging(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
				System.out.println("---------------------------------------------------------------------------------");
			}
		}
		else if(args.length==6)
			dp=new DemandPaging(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
	}
}