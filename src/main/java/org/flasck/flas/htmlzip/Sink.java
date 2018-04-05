package org.flasck.flas.htmlzip;

public interface Sink {
	public void beginFile(String file);
	public void card(String tag, int from, int to);
	public void hole(String called, int from, int to);
	public void identifyElement(String called, int from, int to);
	public void dodgyAttr(int from, int to);
	public void fileEnd();
}
