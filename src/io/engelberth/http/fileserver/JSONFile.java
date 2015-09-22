package io.engelberth.http.fileserver;

import java.io.FileFilter;
import java.io.File;
import java.io.FileNotFoundException;
import org.json.*;

public class JSONFile {
	private String mRoot = null;
	
	private JSONArray mRootArray;
	
	public static void main(String[] args) throws Exception {
		JSONFile jsonExplorer = new JSONFile("./");
		System.out.println(jsonExplorer);
	}
	@Override
	public String toString() {
		return mRootArray.toString(2);
	}
	private JSONFile() { }
	
	public JSONFile(String root) throws FileNotFoundException {
		mRoot = root;
		File rootFile = new File(root);
		if (!rootFile.exists()) throw new FileNotFoundException("Root: " + root + " does not exist");
		if (!rootFile.isDirectory()) throw new FileNotFoundException("Root: " + root + " is not a directory");
		
		File[] rootFiles = rootFile.listFiles();
		JSONObject[] jsonFiles = new JSONObject[rootFiles.length];
		
		for (int i = 0; i < jsonFiles.length; i++) {
			jsonFiles[i] = new JSONObject().put("name", rootFiles[i].getName());

			if (rootFiles[i].isDirectory()) jsonFiles[i].put("dir", true);
			if (rootFiles[i].isFile()) jsonFiles[i].put("file", true);

			jsonFiles[i].put("size", rootFiles[i].length());
		} 
		mRootArray = new JSONArray(jsonFiles);
		
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
