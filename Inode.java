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
	short getIndexBlockNumber(){
		return indirect;
	}

	//
	boolean setIndexBlock( short indexBlockNumber ){
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

	short findTargetBlock( int offset){
		int block = offest / 512;
		//directly
		if(block < 11){
			return direct[block];
		//no indirect
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
		int block = offset / 512;
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
				SysLIb.rawwrite(indirect, buffer);
				return true;
			}
		}

	}
}