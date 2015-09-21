package io.engelberth.http.fileserver;

import java.io.FileFilter;
import java.io.File;
import java.io.FileNotFoundException;
import org.json.*;

public class JSONFile {
	private String mRoot = null;
	private File mRootFile;
	private File[] mDirectories;
	private File[] mFiles;
	private JSONArray mRootArray = new JSONArray();
	
	public static void main(String[] args) throws Exception {
		JSONFile jsonExplorer = new JSONFile("./");
	}
	@Override
	public String toString() {
		return mRootArray.toString(4);
	}
	private JSONFile() { }
	
	public JSONFile(String root) throws FileNotFoundException {
		mRoot = root;
		mRootFile = new File(root);
		if (!mRootFile.exists()) throw new FileNotFoundException("Root: " + root + " does not exist");
		if (!mRootFile.isDirectory()) throw new FileNotFoundException("Root: " + root + " is not a directory");
		mDirectories = mRootFile.listFiles(new DirectoryFileFilter());
		mFiles = mRootFile.listFiles(new FileFileFilter());
	}
	
	private boolean addFile(File f) {
		if (!f.exists()) return false;
		if (f.isDirectory()) mRootArray.put(new JSONObject().put("name", f.getName()).put("type", "directory"));
		return true;
	}
	public static class DirectoryFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	}
	public static class FileFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile();
		}
	}
	
}
