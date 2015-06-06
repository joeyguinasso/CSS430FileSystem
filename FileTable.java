import java.util.*;

public class FileTable{
	private Vector<FileTableEntry> table;
	private Directory dir;
	
	public FileTable(Directory directory){
		table = new Vector<FileTableEntry>();
		dir = directory;
	}
	
	public synchronized FileTableEntry falloc(String filename, String mode){
		Inode tempNode = null;
		short iNum = -1;
		
		while(true){
			if(filename.equals("/")){
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
			tempNode = new Inode(iNum);
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
		}
		tempNode.count++;
		tempNode.toDisk(iNum);
		FileTableEntry fte = new FileTableEntry(tempNode, iNum, mode);
		if(mode.equals("a")){
			fte.seekPtr = tempNode.length;
		}
		table.add(fte);
		return fte;
	}
	
	
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