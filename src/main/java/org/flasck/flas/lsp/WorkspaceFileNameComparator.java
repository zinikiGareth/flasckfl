package org.flasck.flas.lsp;

import java.io.File;

import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class WorkspaceFileNameComparator extends FileNameComparator {
	private final static String[] extensions = new String[] { ".fl", ".ut", ".st", ".fa" };
	
	@Override
	public int compare(File o1, File o2) {
		int e1 = find(FileUtils.extension(o1.getName()));
		int e2 = find(FileUtils.extension(o2.getName()));
		return Integer.compare(e1, e2);
	}

	public static int find(String extension) {
		for (int i=0;i<extensions.length;i++)
			if (extensions[i].equals(extension))
				return i;
		return -1;
	}

}
