import java.util.*;

public class FileTable{
	private Vector<FileTableEntry> table;
	private Directory dir;
	
	public FileTable(Directory directory, int numInodes){
		table = new Vector<FileTableEntry>();
		dir = directory;
	}
	
	 public synchronized FileTableEntry falloc( String fname, String mode ) {
             	  
    	short iNumber = -1;											
      	Inode inode = null;
      	
      	while(true){
      		iNumber = fname.equals("/") ? 0 : dir.namei(fname);
      		if(iNumber >= 0){							
      			inode = new Inode(iNumber);	
      			if(mode.compareTo("r") == 0){						
      				if (inode.flag > 0) {							
      					try {
							wait();
						} catch (InterruptedException e) { }
      				}
      				break;
      			} else if(mode.compareTo("w") == 0){			
      				if (inode.flag > 0) {						
      					try {									
      						wait();
      					} catch (InterruptedException e) { }
      				}
      				inode.flag=1;
      				break;
      			} else if (mode.compareTo("w+") == 0) {			
      				if (inode.flag > 0) {							
      					try {									
      						wait();
      					} catch (InterruptedException e) { }
      				}
      				inode.flag=1;
      				break;
      			} else if (mode.compareTo("a") == 0) {				
      				if (inode.flag > 0) {						
      					try {									
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
      	inode.count++;										
      	inode.toDisk(iNumber);										
      	FileTableEntry e = new FileTableEntry(inode, iNumber, mode);
      	if (mode.equals("a")) {								
      		e.seekPtr = inode.length;
      	}
      	table.addElement(e);
      	return e;
      }
	
	public synchronized boolean ffree(FileTableEntry e){
		boolean retVal = table.removeElement(e);
		if(retVal == true){
			e.inode.count--;
			if(e.inode.count == 0){
				e.inode.flag = 0;
			}
			e.inode.toDisk(e.iNumber);
			e = null;
			notifyAll();
			return true;
		}else{
			return false;
		}
	}
	
	public synchronized boolean fempty(){
		return table.isEmpty();
	}
}