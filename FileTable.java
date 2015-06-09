import java.util.*;

public class FileTable{
	private Vector<FileTableEntry> table;
	private Directory dir;
	//private Vector<Inode> inodes;
	
	public FileTable(Directory directory, int numInodes){
		table = new Vector<FileTableEntry>();
		dir = directory;
		//inodes = new Vector<Inode>();
		//for(short i = 0; i < numInodes; i++){
		//	inodes.add( new Inode(i));
		//}
	}
	
	 public synchronized FileTableEntry falloc( String fname, String mode ) {
             	  
    	short iNumber = -1;											//Initialize inumber/inode to empty values
      	Inode inode = null;
      	
      	while(true){
      		iNumber = fname.equals("/") ? 0 : dir.namei(fname);		//set inumber to either 0 or the namei of filename
      		if(iNumber >= 0){										//As long as iNumber isn't less than zero, continue
      			inode = new Inode(iNumber);							//create a new Inode using out zero or higher number
      			if(mode.compareTo("r") == 0){						//Check to see if we are flagged for read
      				if (inode.flag > 0) {							//If the flag is in use, wait, otherwise break out of loop
      					try {
							wait();
						} catch (InterruptedException e) { }
      				}
      				break;
      			} else if(mode.compareTo("w") == 0){				//Check to see if the flag is set to write
      				if (inode.flag > 0) {							//If in use, wait, otherwise set flag to used and break
      					try {										//so that other writers can't enter
      						wait();
      					} catch (InterruptedException e) { }
      				}
      				inode.flag=1;
      				break;
      			} else if (mode.compareTo("w+") == 0) {				//Check to see if the flag is set to read/write
      				if (inode.flag > 0) {							//If in use, wait, otherwise set flag to used and break
      					try {										//so that other writers can't enter
      						wait();
      					} catch (InterruptedException e) { }
      				}
      				inode.flag=1;
      				break;
      			} else if (mode.compareTo("a") == 0) {				//Check to see if the flag is set to append
      				if (inode.flag > 0) {							//If in use, wait, otherwise set flag to used and break
      					try {										//so that other writers can't enter
      						wait();
      					} catch (InterruptedException e) { }
      				}
      				inode.flag=1;
      				break;
      			} else {
      				throw new IllegalArgumentException();
      			}
      		}
      	}
      	inode.count++;												//Increment the inode count
      	inode.toDisk(iNumber);										//Use the toDisk command to copy the inode to disk
      	FileTableEntry e = new FileTableEntry(inode, iNumber, mode);//Create a new Filetableentry
      	if (mode.equals("a")) {										//If set to a, we need to set the seek pointer as well
      		e.seekPtr = inode.length;
      	}
      	table.addElement(e);										//Add this new entry to the table
      	return e;
      }
	
	/*public synchronized FileTableEntry falloc(String filename, String mode){
		Inode tempNode = null;
		short iNum = -1;
		boolean running = true;
		while(running){
			iNum = filename.equals("/") ? 0 : dir.namei(filename);
			if(iNum >= 0){
				tempNode = inodes.get(iNum);
				/*if(filename.equals("/")){
					iNum = 0;
				}else{
					iNum = dir.namei(filename);
				}
				if(iNum < 0){
					if(mode.equals("r"))return null;
					iNum = dir.ialloc(filename);
					if(iNum < 0) return null;
					tempNode = new Inode();
					break;
				}
				//tempNode = new Inode(iNum);
				switch(mode){
				//READ
				case "r":
					if(tempNode.flag > 0){
						try{
							wait();
						}catch(InterruptedException e){}
					}
					break;	
				//WRITE	
				case "w":
					if(tempNode.flag > 0){
						try{
							wait();
						}catch(InterruptedException e){}
					}
					tempNode.flag = 1;
					break;
				//READ WRITE	
				case "w+":
					if(tempNode.flag > 0){
						try{
							wait();
						}catch(InterruptedException e){}
					}
					tempNode.flag = 1;
					break;
				//APPEND
				case "a":
					if(tempNode.flag > 0){
						try{
							wait();
						}catch(InterruptedException e){}
					}
					tempNode.flag = 1;
					break;
				default: 
					System.out.println("Unhandled Mode");
					throw new IllegalArgumentException();
				}
			}else{
				running = false;
			}
		}
		if(running == false) return null;
		tempNode.count++;
		tempNode.toDisk(iNum);
		FileTableEntry fte = new FileTableEntry(tempNode, iNum, mode);
		if(mode.equals("a")){
			fte.seekPtr = tempNode.length;
		}
		table.add(fte);
		return fte;
	}*/
	
	
	public synchronized boolean ffree(FileTableEntry e){
		boolean retVal = table.removeElement(e);
		if(retVal == true){
			e.inode.flag = 0;
			if(e.inode.flag != 0){
				e.inode.count--;
			}
			e.inode.toDisk(e.iNumber);
			e = null;
			notifyAll();
			return true;
		}else{
			System.out.println("No such file table entry");
			return false;
		}
	}
	
	public synchronized boolean fempty(){
		return table.isEmpty();
	}
}