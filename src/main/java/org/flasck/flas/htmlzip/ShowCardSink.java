package org.flasck.flas.htmlzip;

import java.io.File;

public class ShowCardSink implements Sink {
	private String currentFile;

	@Override
	public void zipLocation(File fromZip) {
	}

	@Override
	public void beginFile(String file) {
		this.currentFile = file;
	}

	@Override
	public void card(String tag, int from, int to) {
		System.out.println("Recovered webzip card " + tag + " from " + currentFile);
	}

	@Override
	public void holeid(String holeName, int from, int to) {
	}

	@Override
	public void hole(int from, int to) {
	}

	@Override
	public void identityAttr(String called, int from, int to) {
	}

	@Override
	public void dodgyAttr(int from, int to) {
	}

	@Override
	public void fileEnd() {
	}
}