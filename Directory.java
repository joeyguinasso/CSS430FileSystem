public class Directory{
	private static int maxChars = 30;
	
	//directory entries
	private int fsize[];
	private char fnames[][];
	
	public Directory(int maxInumber){
		fsize = new int [maxInumber];
		for(int i = 0; i < maxInumber; i++){
			fsize[i] = 0;
		}
		fnames = new char[maxInumber][maxChars];
		String root = "/";
		fsize[0] = root.length();
		root.getChars(0,fsize[0],fnames[0],0);
	}
	
	public void bytes2directory(byte data[]){
		int offset = 0;
		for(int i = 0; i < fsize.length; i++){
			fsize[i] = SysLib.bytes2int(data, offset);
			offset +=4;
		}
		for(int i = 0; i < fnames.length; i++){
			String name = new String(data, offset, maxChars*2);
			name.getChars(0,fsize[i],fnames[i],0);
			offset += maxChars*2;
		}
	}
	
	public byte[] directory2bytes(){
		byte[] directory;
		byte[] toWrite;
		directory = new byte[fsize.length * 4 + fnames.length * maxChars * 2];
		int offset = 0;
		
		for(int i = 0; i < fsize.length; i++){
			SysLib.int2bytes(fsize[i],directory,offset);
			offset += maxChars * 4;
		}
		
		for(int i = 0; i < fnames.length; i++){
			String name = new String(fnames[i], 0, fsize[i]);
			toWrite = name.getBytes();
			
			for (int j = 0; j < toWrite.length; j++) {
				directory[offset] = toWrite[j];
				offset++;
			}
			offset += maxChars * 2;
		}
		return directory;
	}
	
	public short ialloc(String filename){
		if(filename.length() > maxChars){
			//WARNING
			//check to cast to int where its called
			return -1; //filename too large
		}
		
		for(int i = 0; i < fsize.length; i++){
			if(fsize[i] == 0){
				fsize[i] = (byte) filename.length();
				filename.getChars(0,fsize[i],fnames[i],0);
				return (short) i;
			}	
		}
		return -1; //ran out of space
	}
	
	public boolean ifree(short iNumber){
		if(fsize.length < iNumber || iNumber < 0){
			return false;
		}
		if(fsize[iNumber] == 0){
			return false;
		}
		//WARNING name is not removed, should be overwritten
		fsize[iNumber] = 0; 
		return true;
	}
	
	public short namei(String filename){
		for(int i = 0; i < fsize.length; i++){
			//WARNING may be broken
			String temp = new String(fnames[i]);
			if(filename.equals(temp)){
				return (short) i;
			}
		}
		return -1;
	}
}