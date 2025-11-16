package org.flasck.flas.lsp;

public class FLASFileNameComparator extends WorkspaceFileNameComparator {
	private final static String[] extensions = new String[] { ".fl", ".ut", ".st", ".fa" };
	
	public FLASFileNameComparator() {
		super(extensions);
	}
	
	public static boolean isValidExtension(String extension) {
		return find(extensions, extension) != -1;
	}
}
