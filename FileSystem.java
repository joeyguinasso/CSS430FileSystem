import java.util.*;


public class FileSystem{
	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	
	public FileSystem(int diskBlocks){
		
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superblock.inodeBlocks);
		filetable = new FileTable(directory);
		
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if(dirSize > 0) {
			byte[] dirData = new byte[dirSize];
			read(dirent, dirData);
			directory.byte2directory(dirData);
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
		if(filetable.fempty()){
			System.out.println("Unable to format superblock. File is in use.");
			return false;
		}
		superblock.format(files);
		directory = new Directory(superblock.inodeBlocks);
		filetable = new FileTable(directory);
		return true;
	}
	
	public FileTableEntry open(String filename, String mode){
		if(!filename.equals("/")){
			FileTableEntry fte;
			Inode ind;
			if(filename == null || mode == null) return null;
			fte = filetable.falloc(filename,mode);
			ind = fte.inode;
			if(fte == null){
				filefree(fte);
			}else if(fte.mode == -1){
				filefree(fte);
			}else if(ind == null){
				filefree(fte);
			}else{
				synchronized (fte){
					if(fte.mode.equals("w") && ! deallocAllBlocks(fte)){
						
					}
				}
			}
			return fte;
		}
	}
	
	private FileTableEntry filefree(FileTableEntry fte){
		filetable.ffree(fte);
		return null;
	}
	
	public boolean close(FileTableEntry fte){
		
	}
	
	public int fsize(FileTableEntry fte){
		
	}
	
	public int read(FileTableEntry fte) {
		
	}
	
	public int write(FileTableEntry fte, byte[] buffer){
		
	}
	
	public boolean delete(String filename){
		
	}
	
	public int seek(FileTableEntry fte, int offset, int whence){
		
	}
	
	private boolean deallocAllBlocks(FileTableEntry fte){
		Inode inode;
		byte[] data;
		int block;
		
		if(fte == null) return false;
		if((inode = fte.inode) == null) return false;
		if(ind.count > 1) return false;
		
		for(int i = 0; i < inode.length; i++ ){
			block = inode.findTargetBlock(i);
			if(block == -1) continue;
			if(superblock.retBlock(block)){
				//need target block from inode
			}
		}
		
		
		
	}
	
	
	
	
	
}