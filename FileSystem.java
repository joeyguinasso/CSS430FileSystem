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
		/*byte[] b = new byte[1000];
		SysLib.int2bytes(diskBlocks,b,0);
		SysLib.rawwrite(0,b);
		boolean retVal = (superblock.format(files) == 0) ? true : false;
		System.err.println("SuperBlock totalBlocks is "+superblock.totalBlocks);
		return retVal;*/
	}
	
	public synchronized FileTableEntry open(String filename, String mode){
		if(!filename.equals("/")){
			short inode = directory.namei(filename);
			//System.err.println("inode is "+(int)inode);
			if(inode == -1 && mode.equals("r")) return null;
			FileTableEntry fte;
			if(inode < 0) directory.ialloc(filename);
			fte = filetable.falloc(filename, mode);
			if(mode.equals("w")){
				deallocAllBlocks(fte);
			}else if(mode.equals("a")){
				fte.seekPtr = fte.inode.length;
			}else if(mode.equals("r") || mode.equals("w+")){
				//nothing
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
	
	public synchronized int read(FileTableEntry fte, byte[] buffer) {
		if(fte == null) return -1;
		if(fte.mode == "w") return -1;
		if(fte.mode == "a") return -1;
		if(fte.inode == null) return -1;
		int i = 0;
			while(i != buffer.length && fte.seekPtr != fte.inode.length){
				byte[] iBlock = new byte[Disk.blockSize];
				int offset = fte.seekPtr % Disk.blockSize;
				int read = Math.min(Disk.blockSize - offset, buffer.length - i);
				int current = fte.inode.findTargetBlock(fte.seekPtr);
				SysLib.rawread(current, iBlock);
				System.arraycopy(iBlock, offset, buffer, i, read);
				i += read;
				fte.seekPtr += read;
			}
		return i;
	}
	
	public int write(FileTableEntry fte, byte[] buffer){
		int seekPtr, length, offset, remaining, available, wLength, index;
		short block;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null)
			return -1;
		// mode cannot be read only
		//if (fte.mode == FileTableEntry.READONLY)
			//return ERROR;
		// iNode cannot be null
		if ((iNode = fte.inode) == null)
			return -1;
		// iNode must not be in use
		if (fte.mode.equals("r") || fte.mode.equals("w"))
			return -1;
		// write up to buffer length
		length = buffer.length;
		// on error, set iNode flag to "to be deleted" because it's probably
		// garbage now
		// multiple threads cannot write at the same time
		synchronized (fte) {
			// start at position pointed to by inode's seek pointer
			// append should set seek pointer to EOF
			seekPtr = fte.mode.equals("a") ? seek(fte, 0, SEEK_END): fte.seekPtr;
			iNode.flag = 1; // set flag to write
			index = 0;
			data = new byte[Disk.blockSize];
			while (index < length) {

				// byte offset-- 0 is a new block
				offset = seekPtr % Disk.blockSize;
				// bytes available
				available = Disk.blockSize - offset;
				// bytes remaining
				remaining = length - index;
				// bytes to write-- cannot be greater than available
				wLength = Math.min(available, remaining);

				// get next block from iNode
				if ((block = iNode.findTargetBlock(offset)) == -1) {
					// if ERROR, file is out of memory, so get a new block
					if ((block = (short) superblock.nextFreeBlock()) == -1) {
						//Kernel.report("Write failure: Out of memory!");
						iNode.flag = 0;
						break;
						// return ERROR; // no more free blocks
					}
					// read the file to the block
					if (iNode.setTargetBlock(seekPtr, block) == false) {
						// out of bounds, try to get a new indirect block
						if (iNode.setIndexBlock(block) == false) {
							//Kernel.report("Write failure: Failed to set index block "
							//		+ block);
							iNode.flag = 0;
							break;
							// return ERROR;
						}
						// index block set, get a new block
						if ((block = (short) superblock.nextFreeBlock()) == -1) {
							//Kernel.report("Write failure: Out of memory!");
							iNode.flag = 0;
							break;
							// return ERROR; // no more free blocks
						}
						if (iNode.setTargetBlock(seekPtr, block) == false) {
							//Kernel.report("Write failure: Failed to set target block "
							//		+ block);
							iNode.flag = 0;
							break;
							// return ERROR;
						}
					}
				}

				if (block >= superblock.totalBlocks) {
					//Kernel.report("Write failure: Block" + block
					//		+ " out of range");
					iNode.flag = 0;
					break;
				}

				if (offset == 0) {
					data = new byte[Disk.blockSize];
				}

				SysLib.rawread(block, data);

				// copy data to buffer
				// source, source position, destination, destination position,
				// length to copy
				System.arraycopy(buffer, index, data, offset, wLength);
				// write data to disk

				SysLib.rawwrite(block, data);

				index += wLength;
				seekPtr += wLength;
			}
			// update iNode for append or w+
			if (seekPtr > iNode.length)
				iNode.length = seekPtr;
			// set new seek pointer
			seek(fte, index, SEEK_CUR);
			if (iNode.flag == 0) {
				// iNode is now USED
				iNode.flag = 1;
			}
			// save iNode to disk
			iNode.toDisk(fte.iNumber);
		}
		// if error was not returned, all bytes wrote successfully-- return
		// length
		return index;
		/*if(fte == null) return -1;
		if(fte.mode == "r") return -1;
		if(fte.inode == null) return -1;
		if(fte.inode.flag == 1) return -1; //if its in use do not write to it.
		int block = fte.seekPtr + buffer.length - fte.inode.length;
		while(block > 0) {
			block -= Disk.blockSize;
			addBlock(fte.inode);
		}
		int i = 0;
		do {
			byte[] oBlock = new byte[Disk.blockSize];
			int offset = fte.seekPtr%Disk.blockSize;
			int write = Math.min(Disk.blockSize - offset, buffer.length - i);
			int current = fte.inode.findTargetBlock(fte.seekPtr);
			//System.err.println("current is "+ current);
			fte.seekPtr += write;
			if(fte.seekPtr > fte.inode.length) fte.inode.length = fte.seekPtr;
			SysLib.rawread(current, oBlock);
			System.arraycopy(buffer, i, oBlock, offset, write);
			i += write;
			SysLib.rawwrite(current,oBlock);
		} while(i != buffer.length);
		return i;*/
	}

	private int addBlock(Inode inode){
		int nBlock = superblock.nextFreeBlock();
		int retVal = inode.addBlock(nBlock);
		if(retVal == -2){
			int nIndirectBlock = superblock.nextFreeBlock();
			inode.getIndirectBlock(nBlock);
			retVal =inode.direct[nBlock] = -1;
		}
		return retVal;
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
		
		//deallocate all direct blocks.
		for(int i = 0; i < inode.length; i++ ){
			block = inode.findTargetBlock(i);
			if(block == -1) continue;
			if(superblock.retBlock(block)){
				if(inode.setTargetBlock(block, (short) -1) == false){
					System.out.println("Error when deallocating all blocks.");
					return false;
				}
			}
		}
		
		//deallocate all indirect blocks.
		if(inode.indirect != -1){
			superblock.retBlock(inode.indirect);
		}
		inode.count = 0;
		inode.length = 0;
		inode.flag = -1;
		for(int i = 0; i < inode.direct.length; i++){
			inode.direct[i] = -1;
		}
		//May not need this.
		inode.toDisk(fte.iNumber);
		return true;
	}
	
	
	
	
	
}