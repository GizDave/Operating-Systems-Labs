import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

class Safer{
	//grant the release and check whether each release amount is smaller or equal to what the task is allocated
	protected static int[] release(int[] available_resource, int[] current_allocation, int[] release_these){
		for(int i=0; i<current_allocation.length; i++){
			if(release_these[i+1]<=current_allocation[i]){
				available_resource[i]+=release_these[i+1];
				current_allocation[i]-=release_these[i+1];
			}
			else
				System.out.println("	Task Releases More Resources Than It Owns "+release_these[i+1]+">"+current_allocation[i]);
		}
		return available_resource;
	}
	//check if initial claims are legal or there are enough resource for said claim
	protected static boolean isLegal(int[] res_avail, int[] claim){
		System.out.println("	isLegal: avail "+Arrays.toString(res_avail)+" vs claim "+Arrays.toString(claim));
		for(int i=1; i<claim.length; i++)
			if((res_avail[i-1]==0 && claim[i]!=0) || res_avail[i-1]<claim[i])
				return false;
		return true;
	}
	//check if granting said request would lead to an unsafe state. it also works to check if deadlock exists.
	//assume the new requests are now part of the allocations
	protected static boolean isSafe(int[] available, int[][] claims, int[][] allocations){
		//all three parameters don't have the extra element that indicates usage.
		System.out.println("		isSafe:\n 			avail: "+Arrays.toString(available)+"\n 			alloc: "+Arrays.deepToString(allocations)+"\n 			claims: "+Arrays.deepToString(claims));
		int i,j;
		boolean canTerminate=true;
		boolean[] zero_row=new boolean[allocations.length];
		for(i=0; i<allocations.length; i++){
			for(j=0; j<allocations[0].length && allocations[i][j]==0; j++){}
			if(j<allocations[0].length)
				canTerminate=false;
			else
				zero_row[i]=true;
		}
		if(canTerminate)
			return true;
		canTerminate=true;
		//System.out.println("	after 1st true");
		for(i=0; i<claims.length; i++){
			if(zero_row[i])
				continue;
			for(j=0; j<claims[0].length; j++)
				if(available[j]+allocations[i][j]<claims[i][j]){
					canTerminate=false;
					break;
				}
			if(canTerminate){
				int[] available_clone=available.clone(); 
				int[][] allocations_clone=new int[allocations.length][allocations[0].length];
				for(j=0; j<allocations.length; j++)
					allocations_clone[j]=allocations[j].clone();
				for(j=0; j<claims[0].length; j++){
					available_clone[j]+=allocations[i][j];
					allocations_clone[i][j]=0;
				}
				//System.out.println(String.format("task %d: (old) avail=%s, alloc=%s; (new) avail=%s, alloc=%s", i+1, Arrays.toString(available), Arrays.deepToString(allocations), Arrays.toString(available_clone), Arrays.deepToString(allocations_clone)));
				if(Safer.isSafe(available_clone, claims, allocations_clone))
					return true;
			}
			canTerminate=true;
		}
		return false;
	}
	//convert string claims to int claims, 
	protected static int[] loadRequest(String[] claims, int num_type){
		//claims: [instruction, task_num, res_type, res_num, res_type, res_num, ...]
		//            0             1        2         3        4         5
		//request:[                                 res_num,           res_num, ...]
		//                                             0                  1
		//System.out.println(Arrays.toString(claims));
		//System.out.println("loadRequest "+Arrays.toString(claims));
		int i=0, j=3;
		int[] request=new int[num_type];
		for(i=0; i<request.length && j<claims.length; i++){
			request[Integer.parseInt(claims[j-1])-1]=Integer.parseInt(claims[j]);
			//System.out.println("request i: "+request[i]);
			j+=2;
		}
		//System.out.println("	"+Arrays.toString(request));
		return request;
	}
	//grant request
	protected static void grant_request(int[] available_resource, int[] current_allocation, int[] request){
		for(int i=0; i<available_resource.length; i++){
			available_resource[i]-=request[i+1];
			current_allocation[i]+=request[i+1];
		}
	}
	//check if task terminated/aborted
	protected static boolean isComplete(LinkedList<String> input){
		return (input.peek()=="abort" || input.size()==0)? true:false;
	}
}

public class ResourceAllocation{
	//primary variables
	private int num_task, num_type, num_alive; //number of tasks in total, number of resource types, number of remaining tasks, respectively
	private int[] existing_resource; //an array of all existing resource types and their numbers. index=resource type/num
	private LinkedList<String>[] task_input;
	private data_wrap[] print_stack;
	//backup variable
	private String contents;
	private int[] backup;
	private LinkedList<String>[] backup_input;
	//auxillary variable
	private int i, j, k;
	//security variable
	private boolean lock=true;
	//enum for all possible activity states
	enum Act{
		initiate, request, compute, release, terminate, abort, delay;
	}
	//private inner class that keeps track of the times of a task
	private class data_wrap{
		protected int time_taken, waiting_time;
		public data_wrap(){
			time_taken=waiting_time=0;
		}
	}
	//constructor
	//assume correct input
	public ResourceAllocation(String contents){
		this.contents=contents;
		String temp, init_input=contents.substring(0, contents.indexOf("|"));
		//load numbers of tasks, variable types
		String[] divided=init_input.split(" ");
		num_task=num_alive=Integer.parseInt(divided[0]);
		num_type=Integer.parseInt(divided[1]);
		//initialize data structures
		print_stack=new data_wrap[num_task];
		task_input=new LinkedList[num_task];
		for(i=0; i<num_task; i++){
			print_stack[i]=new data_wrap();
			task_input[i]=new LinkedList<>();
		}
		//load initial number of available resources for each resource type
		existing_resource=new int[num_type];
		for(i=0; i<existing_resource.length; i++)
			existing_resource[i]=Integer.parseInt(divided[i+2]);
		//load the instructions into individual linkedlists, one for each task
		contents=contents.substring(init_input.length()+1);
		temp=contents;
		while(temp.length()>1){
			j=temp.indexOf("|");
			divided=temp.substring(0,j).split(" ");
			task_input[Integer.parseInt(divided[1])-1].add(temp.substring(0,j));
			temp=temp.substring(j+1);
		}
		//back up data
		backup_input=task_input.clone();
		backup=new int[2+existing_resource.length];
		backup[0]=num_task;
		backup[1]=num_type;
		System.arraycopy(existing_resource, 0, backup, 2, existing_resource.length);
		lock=false;
	}
	public LinkedList<String>[] get_TaskInput(){return task_input;}
	//reset the primary variables to allow another resource manager run
	private void reset(){
		num_task=num_alive=backup[0];
		num_type=backup[1];
		for(int i=0; i+2<existing_resource.length; i++)
			existing_resource[i]=backup[i+2];
		task_input=backup_input.clone();
		for(i=0; i<num_task; i++)
			print_stack[i]=new data_wrap();
	}
	//satisfy a request if possible. otherwise, make the task wait
	//when a release occurs, try to satisfy pending requests in a FIFO manner
	public void optimisticResourceManager(){
		if(lock)
			return;
		Act command;
		String original, temp;
		boolean still_dead=true;
		ListIterator<Integer> it;
		String[] input;
		int cycles=0, total_waiting_time=0, total_time_taken=0;
		int[] available_resource=existing_resource.clone(), request=new int[num_type];
		int[][] current_allocation=new int[num_task][num_type], all_requests=new int[num_task][num_type+1], all_releases=new int[num_task][num_type+1];
		System.arraycopy(existing_resource, 0, available_resource, 0, available_resource.length);
		LinkedList<Integer> pending_requests=new LinkedList<>(), remove_these=new LinkedList<>();
		//System.out.println("available resources "+Arrays.toString(available_resource));
		while(num_alive>0){
			//System.out.println(String.format("During cycle %d-%d", cycles, cycles+1));
			//check pending requests first
			if(pending_requests.size()>0){
				//System.out.println("	first check pending requests");
				it=pending_requests.listIterator(0);
				while(it.hasNext()){
					if(Safer.isLegal(available_resource, all_requests[(i=it.next())])){
						//System.out.println("		task "+(i+1)+"'s pending request is granted.");
						Safer.grant_request(available_resource, current_allocation[i], all_requests[i]);
						Arrays.fill(all_requests[i], 0);
						remove_these.add((Integer)i);
					}
					else{
						print_stack[i].waiting_time++;
						//System.out.println("		task "+(i+1)+"'s pending request is not granted. "+Arrays.toString(current_allocation[i]));
					}
				}
			}
			//perform designated task instructions
			for(i=0; i<task_input.length; i++){
				if(Safer.isComplete(task_input[i]) || (pending_requests.contains(i)))
					continue;
				original=task_input[i].pop();
				input=original.split(" ");
				command=Act.valueOf(input[0]);
				switch(command){
					case initiate:  //optimistic manager ignores initial claims
									//System.out.println("	task "+(i+1)+" initiates.");
									break;
					case request:	//copy request into the all_requests matrx
									request=Safer.loadRequest(input, num_type);
									all_requests[i][0]=1;
									for(j=0; j<request.length; j++)
										all_requests[i][j+1]+=request[j];
									//task_input[i].add(original);
									break;
					case compute:	//compute for one cycle
									if((j=Integer.parseInt(input[2]))>1){
										task_input[i].addFirst(String.format("compute %s %d 0", input[1], j-1));
										//System.out.println("	task "+(i+1)+" is computing.");
									}
									/*
									else
										System.out.println("	task "+(i+1)+" finishes computing.");
									*/
									break;
					case release:	//copy release into the all_releases matrix
									//System.out.println("	task "+(i+1)+" releases resources.");
									request=Safer.loadRequest(input, num_type);
									all_releases[i][0]=1;
									for(j=0; j<request.length; j++)
										all_releases[i][j+1]+=request[j];
									break;
					case terminate: //terminates task
									task_input[i].clear();
									print_stack[i].time_taken=cycles;
									num_alive--;
									break;
				}
			}
			//remove pending requests that were granted earlier in this cycle
			if(remove_these.size()>0){				
				pending_requests.removeAll(remove_these);
				remove_these.clear();
			}
			//grant requested resources if possible
			for(i=0; i<num_task; i++){
				if(all_requests[i][0]==1){
					if(Safer.isLegal(available_resource, all_requests[i])){
						//System.out.println("	task "+(i+1)+" completes its request. ");
						Safer.grant_request(available_resource, current_allocation[i], all_requests[i]);
						Arrays.fill(all_requests[i], 0);
					}
					else{						
						//System.out.println("	task "+(i+1)+"'s request cannot be granted. ");
						//add task to "blocked" list
						if(!pending_requests.contains(i)){
							print_stack[i].waiting_time++;
							pending_requests.addLast(i);
						}
					}
				}
			}
			//deadlock detection and recovery
			if(pending_requests.size()==num_alive){
				still_dead=true;
				i=num_task;
				while(still_dead && num_alive>0){
					//find a deadlocked nonterminated task
					it=pending_requests.listIterator(0);
					while(it.hasNext())
						if((j=it.next())<i)
							i=j;
					//abort the selected task
					all_releases[i][0]=0;
					for(j=1; j<all_releases[i].length; j++)
						all_releases[i][j]=current_allocation[i][j-1];
					Safer.release(available_resource, current_allocation[i], all_releases[i]);
					Arrays.fill(all_requests[i], 0);
					task_input[i].clear();
					pending_requests.remove((Integer)i);
					task_input[i].addFirst("abort");
					//System.out.println("	task "+(i+1)+" is aborted.");
					num_alive--;
					//check if deadlock remains
					it=pending_requests.listIterator(0);
					while(it.hasNext())
						if(!Safer.isComplete(task_input[(i=it.next())]) && Safer.isLegal(available_resource, all_requests[i])){
							still_dead=false;
							break;
						}
				}
			}
			//release resources
			for(i=0; i<num_task; i++){
				if(all_releases[i][0]==1){
					Safer.release(available_resource, current_allocation[i], all_releases[i]);
					Arrays.fill(all_releases[i], 0);
				}
			}
			//Collections.sort(pending_requests);
			//System.out.println("     END CYCLE "+cycles+" "+Arrays.toString(available_resource));
			cycles++;
		}
		//print task summaries and then the manager summary
		System.out.println();
		for(i=0; i<print_stack.length; i++){
			if(task_input[i].peek()=="abort")
				System.out.println(String.format("Task %d %s", i+1, "               aborted"));
			else{
				System.out.println(String.format("Task %d %5d %5d     %3.2f%%", i+1, print_stack[i].time_taken, print_stack[i].waiting_time, ((double)print_stack[i].waiting_time/print_stack[i].time_taken)*100));
				total_waiting_time+=print_stack[i].waiting_time;
				total_time_taken+=print_stack[i].time_taken;
			}
		}
		System.out.println(String.format("\nSummary:\n   total_time_taken            %5d\n   total_waiting_time          %5d\n   percentage_of_time_waiting %3.2f%%", total_time_taken, total_waiting_time, ((double)total_waiting_time/total_time_taken)*100));
		reset();
	}
	//allocate resources only when claims don't result in an unsafe state
	public void bankersAlgorithm(){
		/*Safer.isSafe(int[] available, int[][] claims, int[][] allocations)*/
		if(lock)
			return;
		Act command;
		String original;
		boolean exceed_claim=false;
		ListIterator<Integer> it;
		String[] input;
		int cycles=0, total_waiting_time=0, total_time_taken=0;
		int[] available_resource=existing_resource.clone(), request=new int[num_type], available_clone=new int[available_resource.length];
		int[][] current_allocation=new int[num_task][num_type], all_claims=new int[num_task][num_type], all_requests=new int[num_task][num_type+1], all_releases=new int[num_task][num_type+1], allocations_clone=new int[num_task][num_type];
		System.arraycopy(existing_resource, 0, available_resource, 0, available_resource.length);
		LinkedList<Integer> pending_requests=new LinkedList<>(), remove_these=new LinkedList<>();
		//System.out.println("available resources "+Arrays.toString(available_resource));
		while(num_alive>0){
			System.out.println(String.format("During cycle %d-%d", cycles, cycles+1));
			//check pending requests first
			if(pending_requests.size()>0){
				System.out.println("	first check pending requests");
				it=pending_requests.listIterator(0);
				while(it.hasNext()){
					i=it.next();
					request=Safer.loadRequest(task_input[i].peek().split(" "), num_type);
					System.out.print("	");
					System.arraycopy(request, 0, all_requests[i], 1, num_type);
					if(Safer.isLegal(available_resource, all_requests[i])){
						available_clone=available_resource.clone();
						for(j=0; j<current_allocation.length; j++)
							allocations_clone[j]=current_allocation[j].clone();
						for(j=0; j<available_clone.length; j++){
							available_clone[j]-=request[j];
							allocations_clone[i][j]+=request[j];
						}
						System.out.println("		pending safe test:\n 			avail: "+Arrays.toString(available_resource)+"\n 			alloc: "+Arrays.deepToString(current_allocation)+"\n 			requests: "+Arrays.deepToString(all_requests));
						if(Safer.isSafe(available_clone, all_claims, allocations_clone)){
							System.out.print("		task "+(i+1)+"'s pending request is granted (legal and safe). ");
							all_requests[i][0]=1;
							//System.arraycopy(request, 0, all_requests[i], 1, num_type); System.out.println(Arrays.toString(all_requests[i]));
							Safer.grant_request(available_resource, current_allocation[i], all_requests[i]);
							Arrays.fill(all_requests[i], 0);
							remove_these.add((Integer)i);
							task_input[i].pop();
						}
						else{
							System.out.println("		task "+(i+1)+"'s pending request is not granted (unsafe). ");
							print_stack[i].waiting_time++;
							Arrays.fill(all_requests[i], 0);
						}
					}
					else{
						System.out.println("		task "+(i+1)+"'s pending request is not granted (illegal). ");
						print_stack[i].waiting_time++;
						Arrays.fill(all_requests[i], 0);
					}
				}
			}
			//perform designated task instructions
			for(i=0; i<task_input.length; i++){
				if(Safer.isComplete(task_input[i]) || (pending_requests.contains(i)))
					continue;
				original=task_input[i].pop();
				input=original.split(" ");
				command=Act.valueOf(input[0]);
				switch(command){
					case initiate:  //System.out.println("	"+original+" initiate");
									request=Safer.loadRequest(input, num_type);
									System.arraycopy(request, 0, all_requests[i], 1, request.length);
									if(Safer.isLegal(existing_resource, all_requests[i])){
										System.out.println("	task "+(i+1)+" successfully initates. ");
										//System.arraycopy(request, 0, all_claims[i], 0, request.length);
										for(j=0; j<all_claims[0].length; j++)
											all_claims[i][j]+=request[j];
									}
									else{
										System.out.print("	task "+(i+1)+" fails to initiate. ");
										task_input[i].clear();
										task_input[i].addFirst("abort");
										num_alive--;
										Arrays.fill(all_claims[i],0);
										System.out.println("task "+(i+1)+" is aborted. ");
									}
									Arrays.fill(all_requests[i], 0);
									break;
					case request:	//System.out.println("	"+original+" request");
									request=Safer.loadRequest(input, num_type);
									System.arraycopy(request, 0, all_requests[i], 1, num_type);
									//check if task requests more than it claims
									for(j=0; j<all_claims[0].length; j++)
										if(request[j]+current_allocation[i][j]>all_claims[i][j]){
											System.out.print("	task "+(i+1)+"'s request is not granted (claim). ");
											all_releases[i][0]=1;
											for(j=1; j<all_releases[i].length; j++)
												all_releases[i][j]=current_allocation[i][j-1];
											//Safer.release(available_resource, current_allocation[i], all_releases[i]);
											Arrays.fill(all_requests[i], 0);
											task_input[i].clear();
											//pending_requests.remove((Integer)i);
											task_input[i].addFirst("abort");
											num_alive--;
											Arrays.fill(all_claims[i],0);
											Arrays.fill(all_requests[i],0);
											System.out.println("task "+(i+1)+" is aborted.");
											exceed_claim=true;
											break;
										}
									if(exceed_claim){
										Arrays.fill(all_requests[i],0);
										break;
									}
									//check if there are enough resources to meet the request
									else if(Safer.isLegal(available_resource, all_requests[i])){
										//System.out.println("		pre request:\n 			avail: "+Arrays.toString(available_resource)+"\n 			alloc: "+Arrays.deepToString(current_allocation)+"\n 			requests: "+Arrays.deepToString(all_requests));
										for(j=0; j<available_clone.length; j++)
											available_clone[j]=available_resource[j];
										for(j=0; j<current_allocation.length; j++)
											allocations_clone[j]=current_allocation[j].clone();
										for(j=0; j<available_clone.length; j++){
											available_clone[j]-=request[j];
											allocations_clone[i][j]+=request[j];
										}
										//System.out.println("		mid request:\n 			avail: "+Arrays.toString(available_resource)+"\n 			alloc: "+Arrays.deepToString(current_allocation)+"\n 			requests: "+Arrays.deepToString(all_requests));
										if(Safer.isSafe(available_clone, all_claims, allocations_clone)){
											System.out.println("	task "+(i+1)+"'s request is granted (legal and safe). ");
											all_requests[i][0]=1;
											//System.arraycopy(request, 0, all_requests[i], 1, num_type); System.out.println(Arrays.toString(all_requests[i]));
											//System.out.println("		granting request:\n 			avail: "+Arrays.toString(available_resource)+"\n 			alloc: "+Arrays.deepToString(current_allocation)+"\n 			requests: "+Arrays.deepToString(all_requests));
											Safer.grant_request(available_resource, current_allocation[i], all_requests[i]);
											Arrays.fill(all_requests[i], 0);
										}
										else{
											System.out.println("	task "+(i+1)+"'s request is not granted (unsafe). ");
											if(!pending_requests.contains(i)){
												print_stack[i].waiting_time++;
												pending_requests.addLast(i);
											}
											Arrays.fill(all_requests[i],0);
											task_input[i].addFirst(original);
										}
									}
									else{
										System.out.println("	task "+(i+1)+"'s request is not granted (illegal). ");
										if(!pending_requests.contains(i)){
											print_stack[i].waiting_time++;
											pending_requests.addLast(i);
										}
										task_input[i].addFirst(original);
										Arrays.fill(all_requests[i],0);
									}
									//System.out.println("		post request:\n 			avail: "+Arrays.toString(available_resource)+"\n 			alloc: "+Arrays.deepToString(current_allocation)+"\n 			requests: "+Arrays.deepToString(all_requests));
									break;
					case compute:	//System.out.println("	"+original+" compute");
									if((j=Integer.parseInt(input[2]))>1){
										task_input[i].addFirst(String.format("compute %s %d 0", input[1], j-1));
										System.out.println("	task "+(i+1)+" is computing.");
									}
									else
										System.out.println("	task "+(i+1)+" finishes computing.");
									break;
					case release:	//System.out.println("	"+original+" release");
									System.out.println("	task "+(i+1)+" releases resources.");
									request=Safer.loadRequest(input, num_type);
									all_releases[i][0]=1;
									for(j=0; j<request.length; j++)
										all_releases[i][j+1]+=request[j];
									break;
					case terminate: System.out.println("	"+original+" terminate");
									task_input[i].clear();
									print_stack[i].time_taken=cycles;
									num_alive--;
									break;
				}
				exceed_claim=false;
			}
			//remove pending requests that were granted earlier in this cycle
			if(remove_these.size()>0){				
				pending_requests.removeAll(remove_these);
				remove_these.clear();
			}
			//release resources
			for(i=0; i<num_task; i++){
				if(all_releases[i][0]==1){
					Safer.release(available_resource, current_allocation[i], all_releases[i]);
					Arrays.fill(all_releases[i], 0);
				}
			}
			System.out.println("     END CYCLE "+cycles+" with available: "+Arrays.toString(available_resource)+" and alloc:"+Arrays.deepToString(current_allocation));
			cycles++;
		}
		//print task summaries and then the manager summary
		System.out.println();
		for(i=0; i<print_stack.length; i++){
			if(task_input[i].peek()=="abort")
				System.out.println(String.format("Task %d %s", i+1, "               aborted"));
			else{
				System.out.println(String.format("Task %d %5d %5d     %3.2f%%", i+1, print_stack[i].time_taken, print_stack[i].waiting_time, ((double)print_stack[i].waiting_time/print_stack[i].time_taken)*100));
				total_waiting_time+=print_stack[i].waiting_time;
				total_time_taken+=print_stack[i].time_taken;
			}
		}
		System.out.println(String.format("\nSummary:\n   total_time_taken            %5d\n   total_waiting_time          %5d\n   percentage_of_time_waiting %3.2f%%", total_time_taken, total_waiting_time, ((double)total_waiting_time/total_time_taken)*100));
		reset();
	}

	public static void main(String[] args){
		if(args.length==0)
			return;
		try{
			String contents=new String(Files.readAllBytes(Paths.get(new File(args[0]).getAbsolutePath())),StandardCharsets.US_ASCII);
			contents=contents.replaceAll("\\n+", "|").replaceAll("\\s+"," ").trim(); //System.out.println("contents: "+contents);
			ResourceAllocation ra=new ResourceAllocation(contents);
			//System.out.println("optimisticResourceManager()");
			//ra.optimisticResourceManager();
			//System.out.println("------------------");
			System.out.println("bankersAlgorithm()");
			ra.bankersAlgorithm();
			//int[] available={2};
			//int[][] claims={{4}, {4}, {4}};
			//int[][] allocations={{0}, {1}, {1}};
			//System.out.println(Safer.isSafe(available, claims, allocations));
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}