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
		byte[] b = new byte[1000];
		SysLib.int2bytes(diskBlocks,b,0);
		SysLib.rawwrite(0,b);
		return (superblock.format(files) == 0) ? true : false;
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
	
	/*
	public synchronized int write(FileTableEntry fte, byte[] buffer){
		if(fte == null) return -1;
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
			System.err.println("current is "+ current);
			fte.seekPtr += write;
			if(fte.seekPtr > fte.inode.length) fte.inode.length = fte.seekPtr;
			SysLib.rawread(current, oBlock);
			System.arraycopy(buffer, i, oBlock, offset, write);
			i += write;
			SysLib.rawwrite(current,oBlock);
		} while(i != buffer.length);
		return i;
	}
<<<<<<< HEAD
=======
	*/

	public int write(FileTableEntry var1, byte[] var2) {
        if(var1.mode == "r") {
            return -1;
        } else {
            synchronized(var1) {
                int var4 = 0;
                int var5 = var2.length;
                while(var5 > 0) {
                    int var6 = var1.inode.findTargetBlock(var1.seekPtr);
                    if(var6 == -1) {
                        short var7 = (short)this.superblock.getFreeBlock();
                        switch(var1.inode.setTargetBlock(var1.seekPtr, var7)) {
                        case -3:
                            short var8 = (short)this.superblock.getFreeBlock();
                            if(!var1.inode.setIndexBlock(var8)) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }
                            if(var1.inode.setTargetBlock(var1.seekPtr, var7) != 0) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }
                        case 0:
                        default:
                            var6 = var7;
                            break;
                        case -2:
                        case -1:
                            SysLib.cerr("ThreadOS: filesystem panic on write\n");
                            return -1;
                        }
                    }
                    byte[] var13 = new byte[512];
                    if(SysLib.rawread(var6, var13) == -1) {
                        System.exit(2);
                    }
                    int var14 = var1.seekPtr % 512;
                    int var9 = 512 - var14;
                    int var10 = Math.min(var9, var5);
                    System.arraycopy(var2, var4, var13, var14, var10);
                    SysLib.rawwrite(var6, var13);
                    var1.seekPtr += var10;
                    var4 += var10;
                    var5 -= var10;
                    if(var1.seekPtr > var1.inode.length) {
                        var1.inode.length = var1.seekPtr;
                    }
                }
                var1.inode.toDisk(var1.iNumber);
                return var4;
            }
        }
    }	
>>>>>>> origin/master

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