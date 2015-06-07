public class Inode{
	private final static int iNodeSize = 32;
	private final static int directSize = 11;

	public int length;
	public short count;
	public short flag;
	public short direct[] = new short [directSize];
	public short indirect;
		
	public Inode(){
		length = 0;
		count = 0;
		flag = -1;
		for(int i = 0; i < directSize; i++){
			direct[i] = -1;
		}
		indirect = -1;
	}
	
	public Inode(short iNumber){
		int blockNum = 1 + iNumber / 16;
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(blockNum, data);
		int offset = (iNumber % 16) * iNodeSize;
		length = SysLib.bytes2int(data, offset);
		offset +=4;
		count = SysLib.bytes2short(data, offset);
		offset += 2;
		flag = SysLib.bytes2short(data, offset);
		offset += 2;
		
		for(int i = 0; i < directSize; i ++){
			direct[i] = SysLib.bytes2short(data,offset);
			offset += 2;
		}
		indirect = SysLib.bytes2short(data, offset);
	}
	
	public int toDisk(short iNumber){
		int blockNum = 1 + iNumber / 16;
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(blockNum, data);
		int offset = (iNumber % 16) * iNodeSize;
		length = SysLib.bytes2int(data, offset);
		offset +=4;
		count = SysLib.bytes2short(data, offset);
		offset += 2;
		flag = SysLib.bytes2short(data, offset);
		offset += 2;
		
		for(int i = 0; i < directSize; i ++){
			direct[i] = SysLib.bytes2short(data,offset);
			offset += 2;
		}
		
		SysLib.short2bytes(indirect, data, offset);
		SysLib.rawwrite(blockNum, data);
		return iNodeSize;
	}

	//i think itll just be the indirect???

	public short getIndexBlockNumber(){
		return indirect;
	}

	//
	public boolean setIndexBlock( short indexBlockNumber ){
		for(int i = 0; i < 11; i ++){
			if(direct[i] == -1){
				return false;
			}
		}
		
		if(indirect != 1){
			return false;
		} else {
			indirect = indexBlockNumber;
			byte[] buffer = new byte[512];
			for(int i = 0; i < 256; ++i){
				SysLib.short2bytes((short)-1, buffer, i*2);
			}
			SysLib.rawwrite(indexBlockNumber, buffer);
			return true;
		}
	}

	public int findTargetBlock(int var1) {
	        int var2 = var1 / 512;
        	if(var2 < 11) {
           	 return this.direct[var2];
	        } else if(this.indirect < 0) {
        	    return -1;
	        } else {
        	    byte[] var3 = new byte[512];
	            SysLib.rawread(this.indirect, var3);
	            int var4 = var2 - 11;
	            return SysLib.bytes2short(var3, var4 * 2);
        	}
	}

	/*
	public short findTargetBlock( int offset){
		int block = offset / Disk.blockSize;
		//directly
		if(block < 11){
			return direct[block];
		//no indirect
		}else{
			block -= 11;
			byte[] data = new byte[Disk.blockSize];
			SysLib.rawread(indirect, data);
			short[] ptr = new short[Disk.blockSize/2];
			for(int i = 0; i < data.length; i+=2){
				ptr[i/2] = SysLib.bytes2short(data,i);
			}
			return ptr[block];
		}
	}
	*/

	//This is now a boolean. 
	public boolean setTargetBlock(int offset, short indexBlockNumber){
		} else if (indirect < 0 ) {
			return -1;
		//indirect
		} else {
			byte buffer = new byte[512];
			SysLib.rawread(indirect, buffer);
			int blockIndex = block - 11;		//subtract 11 to recieve correct bytes
			return SysLib.bytes2short(buffer, blockIndex*2);
		}
	}

	//should this be boolean? or sumfin else... like return -1 instead
	boolean setTargetBlock(int offset, short indexBlockNumber){
		int block = offset / Disk.blockSize;
		if (block < 11){
			direct[block] = indexBlockNumber;
			return true;
		} else if (indirect < 0) {
			return false;
		} else {
			byte[] buffer = new byte[512];
			SysLib.rawread(indirect, buffer);
			int blockIndex = block - 11;
			if(SysLib.bytes2short(buffer, blockIndex*2) > 0){
				return false;
			} else {
				SysLib.short2bytes(indexBlockNumber, buffer, blockIndex*2);
				SysLib.rawwrite(indirect, buffer);
				SysLIb.rawwrite(indirect, buffer);
				return true;
			}
		}

	}
	
	public int addBlock(int nBlock){
		//direct
		for(int i = 0; i < direct.length; i++){
			if(direct[i] == -1){
				direct[i] = (short) nBlock;
				return nBlock;
			}
		}
		//indirect
		if(nBlock == -1){
			return nBlock = -2;
		}
		//get indirect block
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		short[] indirects = new short[Disk.blockSize/2];
		for(int i = 0; i < data.length; i+=2){
			indirects[i/2] = SysLib.bytes2short(data,i);
		}
		
		for(int i = 0; i < indirects.length; i++){
			if(indirects[i] == -1){
				indirects[i] = (short) nBlock;
				byte[] b = new byte[Disk.blockSize];
				for(int j = 0, k = 0; j < b.length; k++, j+=2){
					SysLib.short2bytes(indirects[k],b,j);
				}
				SysLib.rawwrite(indirect,b);
				return nBlock;
			}
		}
		return -1;
	}
	
	public void getIndirectBlock(int nBlock){
		indirect = (short) nBlock;
		//get indirect block
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		short[] indirects = new short[Disk.blockSize/2];
		for(int i = 0; i < data.length; i+=2){
			indirects[i/2] = SysLib.bytes2short(data,i);
		}
		
		for(int i = 0; i < indirects.length; i++){
			indirects[i] = -1;
		}
		byte[] b = new byte[Disk.blockSize];
		for(int j = 0, k = 0; j < b.length; k++, j+=2){
			SysLib.short2bytes(indirects[k],b,j);
		}
		SysLib.rawwrite(indirect,b);
	}
}