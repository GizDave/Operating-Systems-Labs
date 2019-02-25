import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
class Module{
    protected int absolute_address;
    protected String[] use_list;
    protected String[] program_text;
    
    public Module(int absolute_address, String[] use, String[] text){
        this.absolute_address=absolute_address;
        this.use_list=use;
        this.program_text=text;
    }
}

public class TwoPassLinker{
    private Hashtable<String, Integer> symbol_table=new Hashtable<>();
    private LinkedList<Integer> memory_map=new LinkedList<>();
    private LinkedList<Module> mods=new LinkedList<>();
    private final int word_capacity=300;
    
    public TwoPassLinker(){
        Scanner scan=new Scanner(System.in);
        System.out.println("TwoPassLinker Java Edition\n\nEnter either a file path or a valid string (line breaks need to be replaced wth space). Program will self-check.");
        String input;
        try{
            while(true){
                input=scan.nextLine();
                if(Files.exists(Paths.get(input), LinkOption.NOFOLLOW_LINKS)){
                    System.out.println("WARNING: FILE DETECTED");
                    try{
                        String contents=new String(Files.readAllBytes(Paths.get(input)),StandardCharsets.US_ASCII);
                        //System.out.println(contents);
                        first_pass(contents.replaceAll("\n", " ").replaceAll("\\s+"," ").trim());
                    }catch(IOException e){
                        System.out.println("ERROR: FILE PATH ("+input+") CANNOT BE READ");
                    }
                }
                else{
                    System.out.println("WARNING: FILE CANNOT BE FOUND.");
                    first_pass(input.replaceAll("\\s+"," ").trim());
                }
                second_pass();
                return;
            }
        }catch(Exception e){
            scan.close();
            e.printStackTrace();
            return;
        }
    }
    
    /*
     * 1. find base address of each module
     * 2. find absolute address of external references
     * 3. store #2 in the symbol table
     */
    public void first_pass(String str){
        int curr_capacity=0;
        String[] divided=str.split(" ", 0), use={}, text={};
        String[][] definition;
        int relative_address=0, start=1, end, row, size, index, length, i=1, j;
        boolean alt, full=false;
        while(i<divided.length){
            start=i;
            //System.out.println(start+" "+divided[start-1]);
            if((length=Integer.parseInt(divided[start]))>0){
                //System.out.println("length: "+length);
                end=start+length*2+1;
                definition=new String[(end-start)/2][2];
                row=0;
                alt=false;
                //System.out.println("definition start: "+start+"; end: "+end);
                for(j=start+1; j<end; j++){
                    definition[row][(alt)?1:0]=divided[j];
                    if(alt){
                        if(symbol_table.containsKey(definition[row][0])){
                            System.out.println("ERROR: SYMBOL '"+definition[row][0]+"' IS MULTIPLY DEFINED");
                            symbol_table.remove(definition[row][0]);
                        }
                        row++;
                    }
                    alt=!alt;
                }
            }
            else{
                //System.out.println("definition skipped "+length);
                end=start+1;
                definition=null;
            }
            start=end;
            if((length=Integer.parseInt(divided[start]))>0){
                end++;
                use=new String[length];
                String temp="";
                index=0;
                //System.out.println("use");
                while(index<length){
                    if(divided[end].equals("-1")){
                        use[index]=temp;
                        //System.out.println("    Added '"+use[index]+"'");
                        temp="";
                        index++;
                    }
                    else
                        temp+=divided[end]+" ";
                    end++;
                }
                //System.out.println("start: "+start+"; end: "+end);
            }
            else{
                //System.out.println("use skipped "+length);
                end++;
                use=null;
            }
            start=end;
            if((length=Integer.parseInt(divided[start]))>0 && !full){
                text=new String[length];
                //System.out.println("text");
                start++;
                for(j=0; j<length; j++){
                    text[j]=divided[start+j];
                    //System.out.println("    Added "+divided[start+j]);
                    curr_capacity++;
                    if(curr_capacity>word_capacity){
                        System.out.println("ERROR: MACHINE EXCEEDED CAPACITY");
                        full=true;
                    }
                }
                end=start+j;
                //System.out.println("start: "+start+"; end: "+end);
            }
            else{
                //System.out.println("text skipped "+length);
                end++;
                text=null;
            }
            if(definition!=null && text!=null)
                for(i=0; i<definition.length; i++){
                    size=Integer.parseInt(definition[i][1]);
                    if(size>=text.length){
                        System.out.println("ERROR: DEFINITION OF '"+definition[i][0]+"' EXCEEDS MODULE SIZE; LAST ADDRESS IN MODULE IS USED.");
                        size=text.length-1+relative_address;
                    }
                    else
                        size+=relative_address;
                    if(size>299){
                        System.out.println("ERROR: ABSOLUTE ADDRESS OF '"+definition[i][0]+"' IS MORE THAN THE MAX 300 ADDRESS. USE 299 INSTEAD.");
                        size=299;
                    }
                    symbol_table.put(definition[i][0], size);
                }
            mods.addFirst(new Module(relative_address, use, text));
            relative_address+=text.length;
            //System.out.println("NEW RA="+relative_address);
            i=end;
            //System.out.println("->A module is read");
        }
        System.out.println("Symbol Table:\n"+symbol_table+"\n--------");
    }
    
    public void second_pass(){
        Iterator<Module> mod_it=mods.descendingIterator();
        ArrayList<String> keys=new ArrayList<>();
        Module mod;
        String temp="";
        int instruction=0, address_type, i, j, k, index, length=0;
        String[] texts, uses, divided_use;
        Hashtable<String, String> reverse_use;
        int module_num=1;
        char[] use_instructions;
        while(mod_it.hasNext()){
            System.out.println("Module #"+module_num);
            reverse_use=new Hashtable<>();
            mod=mod_it.next();
            texts=mod.program_text;
            uses=mod.use_list;
            if(uses!=null)
                for(i=0; i<uses.length; i++){
                    divided_use=uses[i].split(" ",0);
                    for(j=1; j<divided_use.length; j++){
                        length+=divided_use[j].length();
                        if(Integer.parseInt(divided_use[j])>=texts.length){
                            System.out.println("ERROR: ADDRESS IN USE LIST OF '"+uses[i].substring(0, uses[i].indexOf(" "))+"' EXCEEDS THE SIZE OF THE MODULE");
                            temp=" "+Integer.toString(texts.length-1);
                            if(j<divided_use.length-1){
                                temp+=" ";
                                index=uses[i].indexOf(temp);
                                uses[i]=uses[i].substring(0, index)+(texts.length-1)+uses[i].substring(index+(length+j-1));
                            }
                            else{
                                index=uses[i].indexOf(temp);
                                uses[i]=uses[i].substring(0, index)+(texts.length-1);
                            }
                        }
                    }
                    length=0;
                }
            if(texts!=null)
                for(i=0; i<texts.length; i++){
                    //System.out.println("Curr text: "+texts[i]);
                    instruction=Integer.parseInt(texts[i].substring(0,4));
                    address_type=Character.getNumericValue(texts[i].charAt(4));
                    //1-immediate", 2-absolute", 3-relative (relocate)", and 4-external (resolve)"
                    if(address_type==3)
                        instruction+=mod.absolute_address;
                    else if(address_type==4 && uses!=null){
                        use_instructions=new char[texts.length];
                        for(j=0; j<uses.length; j++){
                            if(uses[j].substring(1).indexOf(" "+Integer.toString(i))>=0){
                                divided_use=uses[j].split(" ", 0);
                                //System.out.println(uses[j]);
                                if(use_instructions[i]=='y')
                                    System.out.println("ERROR: MULTIPLE SYMBOLS USE THIS INSTRUCTION "+texts[i]);
                                if(!symbol_table.containsKey(divided_use[0])){
                                    System.out.println("ERROR: SYMBOL '"+divided_use[0]+"' IS USED BUT NOT DEFINED. A VALUE OF 111 IS GIVEN");
                                    symbol_table.put(divided_use[0], 111);
                                }
                                instruction=instruction-instruction%1000+symbol_table.get(divided_use[0]);
                                if(!keys.contains(divided_use[0]))
                                    keys.add(divided_use[0]);
                                use_instructions[i]='y';
                            }
                        }
                    }
                    if(instruction%1000>299 && address_type>1){
                        System.out.println("ERROR: ABSOLUTE ADDRESS EXCEEDS MACHINE SIZE. 299 IS USED AS THE LARGEST LEGAL VALUE.");
                        instruction=instruction/1000*1000+299;
                    }
                    System.out.println(instruction);
                    memory_map.addLast(instruction);
                }
            System.out.println();
            module_num++;
            temp="";
        }
        Object[] used_keys=keys.toArray();
        Object[] all_keys=symbol_table.keySet().toArray();
        if(all_keys.length!=used_keys.length){
            Arrays.sort(used_keys);
            Arrays.sort(all_keys);
            if(used_keys.length>0 && used_keys!=null)
                for(i=0; i<used_keys.length; i++)
                    if((index=Arrays.binarySearch(all_keys, used_keys[i]))>-1)
                        all_keys[index]=null;
            for(i=0; i<all_keys.length; i++)
                if(all_keys[i]!=null)
                    System.out.println("WARNING: '"+all_keys[i]+"' WAS DEFINED AND NEVER USED.");
        }
        System.out.println("Memory Map:\n"+memory_map);
    }
    
    public static void main(String[] args){
        TwoPassLinker linker=new TwoPassLinker();
    }
}