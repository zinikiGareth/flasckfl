package org.flasck.flas.htmlzip;

import java.io.File;

public interface Sink {
	public void zipLocation(File fromZip);
	public void beginFile(String file);
	public void card(String tag, int from, int to);
	public void holeid(String holeName, int from, int to);
	public void hole(String called, int from, int to);
	public void identityAttr(String called, int from, int to);
	public void dodgyAttr(int from, int to);
	public void fileEnd();
}
