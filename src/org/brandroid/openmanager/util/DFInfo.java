package org.brandroid.openmanager.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Hashtable;

import org.brandroid.utils.Logger;

import android.os.StatFs;

public class DFInfo
{
	private String mPath;
	private int mSize, mUsed, mFree, mBlocksize;
	
	public String getPath() { return mPath; }
	public int getSize() { return mSize; }
	public int getUsed() { return mUsed; }
	public int getFree() { return mFree; }
	public int getBlockSize() { return mBlocksize; }
	
	public class CannotReadException extends Exception {
		public CannotReadException(String string) {
			super(string);
		} }
	public DFInfo(String path, int size, int used, int free, int blocksize) throws CannotReadException
	{
		mPath = path;
		if(!new File(path).canRead()) throw new CannotReadException("Cannot read " + path);
		mSize = size;
		mUsed = used;
		mFree = free;
		mBlocksize = blocksize;
	}
	public static int getSize(String s)
	{
		int level = 0;
		if(s.endsWith("B"))
			level = 0;
		else if(s.endsWith("K"))
			level = 1;
		else if(s.endsWith("M"))
			level = 2;
		else if(s.endsWith("G"))
			level = 3;
		else if(s.endsWith("T"))
			level = 4;
		else level = 1;
		try {
			double sz = Double.parseDouble(s.replaceAll("[^0-9\\.]", ""));
			sz *= (1024 ^ level);
			return (int)Math.floor(sz);
		} catch(Exception e) { Logger.LogError("Unable to get size from [" + s + "]", e); return -1; }
	}
	public static Hashtable<String, DFInfo> LoadDF()
	{
		//if(mDefault != null) return mDefault;
		Process dfProc = null;
		DataInputStream is = null;
		Hashtable<String, DFInfo> ret = new Hashtable<String, DFInfo>();
		try {
			Boolean handled = false;
			/*
			try {
				dfProc = Runtime.getRuntime().exec("busybox df -h\n");
				handled = true;
			} catch(IOException ex) {
				Logger.LogWarning("busybox failed");
			}
			*/
			if(!handled)
			{
				dfProc = Runtime.getRuntime().exec("df");
				handled = true;
			}
			if(!handled) return null;
			is = new DataInputStream(dfProc.getInputStream());
			String sl;
			while((sl = is.readLine()) != null)
			{
				sl = sl.replaceAll("  *", " ");
				//Logger.LogDebug("DF: " + sl);
				if(!sl.startsWith("/")) continue;
				if(!((sl.indexOf("/mnt") > -1 || sl.toLowerCase().startsWith("/remov"))&&(sl.indexOf("/",1) > -1))) continue;
				if(sl.indexOf("/asec") > -1) continue;
				if(sl.indexOf("/obb") > -1) continue;
				try {
					String[] slParts = sl.split(" ");
					DFInfo item = new DFInfo(slParts[0], getSize(slParts[1]), getSize(slParts[2]), getSize(slParts[3]), getSize(slParts[4]));
					//Logger.LogDebug("DF: Added " + item.getPath() + " - " + item.getFree() + "/" + item.getSize());
					ret.put(slParts[0], item);
				} catch(ArrayIndexOutOfBoundsException e) { Logger.LogWarning("DF: Unable to add " + sl); }
				catch(CannotReadException e) { } 
			}
		} catch (IOException e) {
			Logger.LogError("DF: Couldn't get Drive sizes.", e);
		} finally {
			try {
				if(is != null)
					is.close();
			} catch (IOException e) {
				Logger.LogWarning("DF: Couldn't close drive size input stream.", e);
			}
		}
		return ret;
	}
	
}