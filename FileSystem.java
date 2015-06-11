//-----------------------------------------------------------------------------
//Authors: Joey Guinasso, Drew Byland, Herb Traut
//Course:  CSS 430
//Subject  Final Project
//Date:    6/10/2015
//-----------------------------------------------------------------------------
import java.util.*;

public class FileSystem{
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	
	private int diskBlocks;
	
	public FileSystem(int diskBlocks){
		diskBlocks = diskBlocks;
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory, superblock.totalInodes);
		
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if(dirSize > 0) {
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}
	
	public void sync(){
		FileTableEntry dirEnt = open("/", "w");
		byte[] dirData = directory.directory2bytes();
		write(dirEnt, dirData);
		close(dirEnt);
		superblock.sync();
	}
	
	public boolean format(int files){
		if (!filetable.fempty()) {
			return false;
		}
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory, superblock.totalInodes);
		return true;
	}
	
	public synchronized FileTableEntry open(String filename, String mode){
		if(!filename.equals("/")){
			short inode = directory.namei(filename);
			if(inode == -1 && mode.equals("r")) return null;
			FileTableEntry fte;
			if(inode < 0) directory.ialloc(filename);
			fte = filetable.falloc(filename, mode);
			if(mode.equals("w")){
				deallocAllBlocks(fte);
			}else if(mode.equals("a")){
				fte.seekPtr = fte.inode.length;
			}else if(mode.equals("r") || mode.equals("w+")){
			}
			return fte;
		}
		return null;
	}
	
	private FileTableEntry filefree(FileTableEntry fte){
		filetable.ffree(fte);
		return null;
	}
	
	public synchronized boolean close(FileTableEntry fte){
			if(fte == null) return false;
			fte.count--;
			if(fte.count < 0) fte.count = 0;
			if(fte.count == 0){
				filetable.ffree(fte);
			}
		return true;
	}
	
	public int fsize(FileTableEntry fte){
		Inode inode;
		if(fte == null) return -1;
		inode = fte.inode;
		if(inode == null) return -1;
		return inode.length;
	}
	
	
	public int read(FileTableEntry fte, byte[] buffer) {
		int seekPtr, length, block, offset, available, remaining, rLength, index;
		Inode iNode;
		byte[] data;
		if (fte == null)
			return -1;
		
		if (fte.mode.equals("a"))
			return -1;
		
		if ((iNode = fte.inode) == null)
			return -1;
		
		length = buffer.length;

		
		synchronized (fte) {
			seekPtr = fte.seekPtr;
			data = new byte[Disk.blockSize];
			index = 0;
			while (index < length) {
				offset = seekPtr % Disk.blockSize;
				available = Disk.blockSize - offset;
				remaining = length - index;
				rLength = Math.min(available, remaining);
				block = iNode.findTargetBlock(offset, seekPtr);
				
				if (block == -1) {
					System.err.println("Failed to find target. block "+block);
					System.err.println("seekPtr is "+seekPtr);
					return -1;
				}

				if (block < 0 || block >= superblock.totalBlocks) {
					System.err.println("Block out of bounds"+block);
					break;
				}

				if (offset == 0) {
					data = new byte[Disk.blockSize];
				}

				SysLib.rawread(block, data);
				System.arraycopy(data, offset, buffer, index, rLength);
				index += rLength;
				seekPtr += rLength;
			}
			seek(fte, index, SEEK_CUR);
		}
		return index;
	}
	public int write(FileTableEntry fte, byte[] buffer){
		int seekPtr, length, offset, remaining, available, wLength, index;
		short block;
		Inode iNode;
		byte[] data;
		if (fte == null)
			return -1;
		if ((iNode = fte.inode) == null)
			return -1;
		if (fte.mode.equals("r") || fte.mode.equals("w"))
			return -1;
		length = buffer.length;
		synchronized (fte) {
			seekPtr = fte.mode.equals("a") ? seek(fte, 0, SEEK_END): fte.seekPtr;
			iNode.flag = 1; 
			index = 0;
			data = new byte[Disk.blockSize];
			while (index < length) {
				offset = seekPtr % Disk.blockSize;
				available = Disk.blockSize - offset;
				remaining = length - index;
				wLength = Math.min(available, remaining);
				block = iNode.findTargetBlock(offset, seekPtr);
				if (block == -1) {
					if ((block = (short) superblock.nextFreeBlock()) == -1) {
						System.err.println("Write failure: Out of memory!");
						iNode.flag = 0;
						break;
					}
					if (iNode.setTargetBlock(seekPtr, block) == false) {
						if (iNode.setIndexBlock(block) == false) {
							System.err.println("Write failure: Failed to set index block "+ block);
							iNode.flag = 0;
							break;
						}
						if ((block = (short) superblock.nextFreeBlock()) == -1) {
							System.err.println("Write failure: Out of memory!");
							iNode.flag = 0;
							break;
						}
						if (iNode.setTargetBlock(seekPtr, block) == false) {
							System.err.println("Write failure: Failed to set target block "+ block);
							iNode.flag = 0;
							break;
						}
					}
				}

				if (block >= superblock.totalBlocks) {
					iNode.flag = 0;
					break;
				}

				if (offset == 0) {
					data = new byte[Disk.blockSize];
				}

				SysLib.rawread(block, data);
				System.arraycopy(buffer, index, data, offset, wLength);
				SysLib.rawwrite(block, data);
				index += wLength;
				seekPtr += wLength;
			}
			if (seekPtr > iNode.length){
				iNode.length = seekPtr;
			}
			seek(fte, index, SEEK_CUR);
			if (iNode.flag == 0) {
				iNode.flag = 1;
			}
			iNode.toDisk(fte.iNumber);
		}
		return index;
	}
	
	public synchronized boolean delete(String filename){
		if(filename == null) return false;
		int iNum = directory.namei(filename);
		if(iNum == -1) return false;
		return directory.ifree((short)iNum);
	}
	
	public synchronized int seek(FileTableEntry fte, int offset, int whence){
			if(whence < 1){
				fte.seekPtr = 0 + offset;
			}else if(whence == 1){
				fte.seekPtr = fte.seekPtr + offset;
			}else if(whence == 2){
				fte.seekPtr = fte.inode.length + offset;
			}else{
				fte.seekPtr = fte.inode.length;
			}
			if(fte.seekPtr > fte.inode.length){
				fte.seekPtr = fte.inode.length;
			}
			if(fte.seekPtr < 0){
				fte.seekPtr = 0;
			}
			return fte.seekPtr;
	}
	
	private boolean deallocAllBlocks(FileTableEntry fte){
		Inode inode;
		byte[] data;
		int block;
		
		if(fte == null) return false;
		if((inode = fte.inode) == null) return false;
		if(inode.count > 1) return false;
		
		for(int i = 0; i < inode.length; i++ ){
			block = inode.findTargetBlock(i,i);
			if(block == -1) continue;
			if(superblock.retBlock(block)){
				if(inode.setTargetBlock(block, (short) -1) == false){
					System.out.println("Error when deallocating all blocks.");
					return false;
				}
			}
		}
		
		if(inode.indirect != -1){
			superblock.retBlock(inode.indirect);
		}
		inode.count = 0;
		inode.length = 0;
		inode.flag = -1;
		for(int i = 0; i < inode.direct.length; i++){
			inode.direct[i] = -1;
		}
		inode.toDisk(fte.iNumber);
		return true;
	}
	
	
	
	
	
}