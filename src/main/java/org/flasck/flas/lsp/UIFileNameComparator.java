package org.flasck.flas.lsp;

public class UIFileNameComparator extends WorkspaceFileNameComparator {
	private final static String[] ui = new String[] { ".html", ".css" };
	
	public UIFileNameComparator() {
		super(ui);
	}
	
	public static boolean isValidExtension(String extension) {
		return find(ui, extension) != -1;
	}
}
