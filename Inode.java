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
	
	public int toDisk( short iNumber ) {                  			
    	  int blockNumber = 1 + iNumber / 16;						
    	  byte[] data = new byte[Disk.blockSize];					
    	  SysLib.rawread(blockNumber, data);						
    	  int offset = (iNumber % 16) * iNodeSize;					
    	  SysLib.int2bytes(length, data, offset);					
    	  offset += 4;	
    	  SysLib.short2bytes(count, data, offset);					
    	  offset += 2;
    	  SysLib.short2bytes(flag, data, offset);					
    	  offset += 2;
    	  
    	  for (int i = 0; i < directSize; i++) {					
    		  SysLib.short2bytes(direct[i], data, offset);
    		  offset += 2;
    	  }
    	  SysLib.short2bytes(indirect, data, offset);				
    	  
    	  SysLib.rawwrite(blockNumber, data);
    	  return iNodeSize;
      }
		
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

	public short findTargetBlock(int offset, int seekptr){
		int ptr = 0;
		//if (seekptr > length) return -1;
    	if((seekptr/Disk.blockSize) < 11) {
			ptr = offset/Disk.blockSize;
			return direct[ptr];
		}else{
			ptr = seekptr/Disk.blockSize;
    		byte[] data = new byte[Disk.blockSize];	
			getIndirectBlock(ptr);
			SysLib.rawread(indirect, data);	
			return (short) ptr;
		}
	}
	//This is now a boolean. 
	public boolean setTargetBlock(int offset, short indexBlockNumber){
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
	}
}