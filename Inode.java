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
		
		//for(int i = 0; i < 10; i++){
		//	System.err.println("In INODE.constructor data["+i+"] is " + data[i]);
		//}
		
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
	
	public int toDisk(short iNumber) {
		// initialize the Inode to be added back to Disk
		int offset, block;
		byte[] data;
		if (iNumber < 0)
			return -1;
		block = getOffset(iNumber);
		// get the Disk block at the iNumber-th place
		offset = (iNumber % 16) * iNodeSize;
		data = new byte[Disk.blockSize];
		// same flow as constructor
		SysLib.int2bytes(length, data, offset);
		offset += 4;
		SysLib.short2bytes(count, data, offset);
		offset += 2;
		SysLib.short2bytes(flag, data, offset);
		offset += 2;

		for (int i = 0; i < directSize; i++, offset += 2) {
			SysLib.short2bytes(direct[i], data, offset);
		}

		SysLib.short2bytes(indirect, data, offset);
		offset += 2;

		// write back to Disk
		SysLib.rawwrite(block, data);
		return 0;
	}
	
	public int getOffset(int iNumber) {
		return 1 + iNumber / 16;
	}
	
	/*public int toDisk(short var1) {
        byte[] var2 = new byte[32];
        byte var3 = 0;
        SysLib.int2bytes(this.length, var2, var3);
        int var6 = var3 + 4;
        SysLib.short2bytes(this.count, var2, var6);
        var6 += 2;
        SysLib.short2bytes(this.flag, var2, var6);
        var6 += 2;
        int var4;
        for(var4 = 0; var4 < 11; ++var4) {
            SysLib.short2bytes(this.direct[var4], var2, var6);
            var6 += 2;
        }
        SysLib.short2bytes(this.indirect, var2, var6);
        var6 += 2;
        var4 = 1 + var1 / 16;
        byte[] var5 = new byte[512];
        SysLib.rawread(var4, var5);
        var6 = var1 % 16 * 32;
        System.arraycopy(var2, 0, var5, var6, 32);
        SysLib.rawwrite(var4, var5);
		return 0;
    }*/
	
	/*public int toDisk(short iNumber){
		int blockNum = 1 + iNumber / 16;
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(blockNum, data);
		
		//for(int i = 0; i < 10; i++){
		//	System.err.println("In INODE.toDisk data["+i+"] is " + data[i]);
		//}
		
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
	}*/

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

	public short findTargetBlock(int seekptr){
		if (seekptr > length) return -1;
    	int ptr = seekptr/Disk.blockSize;
    	if (ptr < 11) {
			//System.err.println("Returning direct");
			return direct[ptr];
		}else{
			//System.err.println("Returning indirect");
			ptr -= 11;											
    		byte[] data = new byte[Disk.blockSize];					
			SysLib.rawread(indirect, data);							
			short[] ptrs = new short[Disk.blockSize/2];				
			for (int i = 0; i < data.length; i+=2) {					
				ptrs[i/2] = SysLib.bytes2short(data, i);							
			}
			return ptrs[ptr];
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