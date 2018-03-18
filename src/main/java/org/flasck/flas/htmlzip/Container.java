package org.flasck.flas.htmlzip;

public class Container {

	public final String fromTable;
	public final String template;
	public final int maxrows;
	public final String rowvar;

	public Container(String fromTable, String template, int maxrows, String rowvar) {
		this.fromTable = fromTable;
		this.template = template;
		this.maxrows = maxrows;
		this.rowvar = rowvar;
	}

}
