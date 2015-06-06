class Superblock {
	public int totalBlocks; // the number of disk blocks
	public int totalInodes; // the number of inodes
	public int freeList;    // the block number of the free list's head
	private final int defaultInodeValue = 64;	// default number of inodes
   
	public SuperBlock( int diskSize ) {
		byte[] buffer = new byte[512];
		SysLib.rawread(0, buffer);
		this.totalBlocks = SysLib.bytes2int(buffer, 0);
		this.totalInodes = SysLib.bytes2int(buffer, 4);
		this.freeList = SysLib.bytes2int(buffer, 8);
	
		//if correct info
		if(this.totalBlocks == diskSize && this.totalInodes > 0 && this.freeLIst >= 2){
			return
		} else {
			this.totalBlocks = diskSize;
			SysLib.cerr("Defaault format( " + defaultInodeValue +" ) \n");
			this.format();
		}
   	}

	//default with 64 value
	void format(){
		this.format(defaultInodeValue);
	}

	//overloaded format
	void format(int inodeValue){
		//loop through creating new inodes
		this.totalInodes = inodeValue;
		for(int i = 0; i < this.totalInodes; i++){
			Inode newInode = new Inode();
			newInode.flag = 0;
			newInode.toDisk(i);
		}

		//
		this.freeList = this.totalInodes * 32 / 512;
		for(int i = this.freeList; i < this.totalBlocks; i++){
			byte[] buffer = new byte[512];
			for(int j = 0; j < 512; j++){
				buffer[j] = 0;
			}
			SysLib.int2bytes(i + 1, buffer, 0);
			SysLib.rawwrite(i, buffer);
		}
		this.sync();
	}

	//save to superblock
	void sync() {
		byte[] buffer = new byte[512];
		SysLib.int2bytes(this.totalBlocks, var1, 0);
	        SysLib.int2bytes(this.inodeBlocks, var1, 4);
        	SysLib.int2bytes(this.freeList, var1, 8);
	        SysLib.rawwrite(0, var1);
        	SysLib.cout("Superblock synchronized\n");
	}

	//figure someone might need this
	//gets next free open block
	public int nextFreeBlock(){
		int freeBlock = this.freeList;
		if(freeBlock == -1){
			byte[] buffer = new byte[512];
		        SysLib.rawread(freeBlock, buffer);
		        this.freeList = SysLib.bytes2int(buffer, 0);
			SysLib.int2bytes(0, buffer 0);
			SysLib.rawwrite(freeBlock, buffer);
		}
		return freeBlock;
	}
	
	//Herb added this cuz reasons
	public boolean retBlock(int block){
		byte[] retblk;
		if(block < 0 || block > totalBlocks) return false;
		block = new byte[Disk.blockSize];
		SysLib.int2bytes(freeList,block,0);
		SysLib.rawwrite(block, retblk);
		freeList = block;
		return true;
	}
	
	
	
	
	
}