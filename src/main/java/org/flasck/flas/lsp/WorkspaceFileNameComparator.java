package org.flasck.flas.lsp;

import java.util.Comparator;

import org.zinutils.hfs.HFSPath;
import org.zinutils.utils.FileUtils;

public class WorkspaceFileNameComparator implements Comparator<HFSPath> {
	private final String[] extensions;
	
	public WorkspaceFileNameComparator(String[] extensions) {
		this.extensions = extensions;
	}
	
	@Override
	public int compare(HFSPath o1, HFSPath o2) {
		int e1 = find(extensions, FileUtils.extension(o1.getName()));
		int e2 = find(extensions, FileUtils.extension(o2.getName()));
		int ret = Integer.compare(e1, e2);
		if (ret != 0)
			return ret;
		return o1.getName().compareTo(o2.getName());
	}
	
	protected static int find(String[] extensions, String extension) {
		for (int i=0;i<extensions.length;i++)
			if (extensions[i].equals(extension))
				return i;
		return -1;
	}

}
