class SuperBlock {
	public int totalBlocks; // the number of disk blocks
	public int totalInodes; // the number of inodes
	public int freeList;    // the block number of the free list's head
	private final int defaultInodeBlocks = 64;	// default number of inodes
	private int diskSize;
   
	public SuperBlock( int diskSize ) {
		//read superblock from disk
		this.diskSize = diskSize;
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.rawread(0, superBlock);
   
	public SuperBlock( int diskSize ) {
		//read superblock from disk
		byte[] superBlock = new byte[512];
		SysLib.rawread(0, buffer);
		this.totalBlocks = SysLib.bytes2int(superBlock, 0);
		this.totalInodes = SysLib.bytes2int(superBlock, 4);
		this.freeList = SysLib.bytes2int(superBlock, 8);
	
		//if correct info
		if(this.totalBlocks == diskSize && this.totalInodes > 0 && this.freeList >= 2){
			//disk contents are valid
			return;
		} else {
			//need to format disk
			totalBlocks = diskSize;
			SysLib.cerr("Default format( " + defaultInodeBlocks +" ) \n");
			format( defaultInodeBlocks );
		}
   	}

	public int format(){
    	return format(defaultInodeBlocks);									
	}
	
	public int format(int numInodes){
    	if (numInodes > diskSize - 1 - numInodes%16) {
    		  return -1;
    	}
    	byte[] buffer = new byte[Disk.blockSize];								
    	SysLib.rawread(0, buffer);											
    	totalInodes = numInodes;												
    	totalBlocks = SysLib.bytes2int(buffer, 0) ;							
    	freeList = 1;
    	for (short i = 1; i < totalBlocks; i++) {								
			if (i == totalBlocks - 1)											
				SysLib.short2bytes((short)-1, buffer, 0);
    		else																
				SysLib.short2bytes((short)(i + 1), buffer, 0);
				SysLib.rawwrite(i, buffer);										
		} 
    	int inodeSize = 0;  																		
    	for(short i = 0; i < numInodes; i++) {								
			inodeSize = new Inode().toDisk(i);							
		}
    	freeList += (inodeSize * numInodes)/Disk.blockSize + ((inodeSize * numInodes)%Disk.blockSize > 0 ? 1 : 0);
    	sync();															
    	return 0;
    }

	//save to superblock
	public void sync() {
		byte[] buffer = new byte[Disk.blockSize];
		SysLib.int2bytes(this.totalBlocks, buffer, 0);
	        SysLib.int2bytes(this.totalInodes, buffer, 4);
        	SysLib.int2bytes(this.freeList, buffer, 8);
	        SysLib.rawwrite(0, buffer);
        	SysLib.cout("Superblock synchronized\n");
	}

	//gets next free open block
	public int nextFreeBlock(){
		int freeBlock = this.freeList;
		if(freeBlock == -1){
			byte[] buffer = new byte[512];
		        SysLib.rawread(freeBlock, buffer);
		        this.freeList = SysLib.bytes2int(buffer, 0);
			SysLib.int2bytes(0, buffer, 0);
			SysLib.rawwrite(freeBlock, buffer);
		}
		return freeBlock;
	}
	
	//Herb added this cuz reasons
	public boolean retBlock(int block){
		if(block < 0 || block > totalBlocks) return false;
		byte[] retblk = new byte[Disk.blockSize];
		SysLib.int2bytes(freeList,retblk,0);
		SysLib.rawwrite(block, retblk);
		freeList = block;
		return true;
	}
}