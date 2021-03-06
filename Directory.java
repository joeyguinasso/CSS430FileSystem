import java.util.*;

public class Directory{
	private static int maxChars = 30;
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
	
	public int bytes2directory(byte data[]){
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
		return 0;
	}
	
	public byte[] directory2bytes() {
        byte[] var1 = new byte[this.fsize.length * 4 + this.fnames.length * maxChars * 2];
        int var2 = 0;
        int var3;
        for(var3 = 0; var3 < this.fsize.length; var2 += 4) {
            SysLib.int2bytes(this.fsize[var3], var1, var2);
            var3++;
        }
        for(var3 = 0; var3 < this.fnames.length; var2 += maxChars * 2) {
            String var4 = new String(this.fnames[var3], 0, this.fsize[var3]);
            byte[] var5 = var4.getBytes();
            System.arraycopy(var5, 0, var1, var2, var5.length);
            var3++;
        }
        return var1;
    }
	
	public short ialloc(String filename){
		if(filename.length() > maxChars){
			return -1; 
		}
		
		for(int i = 0; i < fsize.length; i++){
			if(fsize[i] == 0){
				fsize[i] = (byte) filename.length();
				filename.getChars(0,fsize[i],fnames[i],0);
				return (short) i;
			}	
		}
		return -1; 
	}
	
	public boolean ifree(short iNumber){
		if(fsize.length < iNumber || iNumber < 0){
			return false;
		}
		if(fsize[iNumber] == 0){
			return false;
		}
		fsize[iNumber] = 0; 
		return true;
	}
	
	public short namei(String filename){
		String name;
		int length = filename.length();
		for (int i = 0, l = fsize.length; i < l; i++) {
			if (fsize[i] == length) {
				name = new String(fnames[i], 0, fsize[i]);
				if (filename.compareTo(name) == 0) {
					return (short) i;
				}
			}
		}
		return -1;
	}
}