package org.flasck.flas.htmlzip;

public interface Sink {
	public void block(String tag, int from, int to);
	public void hole(int from, int to);
	public void dodgyAttr(String attr, int from, int to);
}
