package org.flasck.flas.lsp;

import java.io.File;

import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class WorkspaceFileNameComparator extends FileNameComparator {
	private final static String[] extensions = new String[] { ".fl", ".ut", ".st", ".fa", ".html" };
	
	@Override
	public int compare(File o1, File o2) {
		int e1 = find(FileUtils.extension(o1.getName()));
		int e2 = find(FileUtils.extension(o2.getName()));
		int ret = Integer.compare(e1, e2);
		if (ret != 0)
			return ret;
		return o1.getName().compareTo(o2.getName());
	}

	public static boolean isValidExtension(String extension) {
		return find(extension) != -1;
	}
	
	private static int find(String extension) {
		for (int i=0;i<extensions.length;i++)
			if (extensions[i].equals(extension))
				return i;
		return -1;
	}

}
