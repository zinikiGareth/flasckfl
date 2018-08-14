package org.flasck.flas.htmlzip;

import java.io.File;

public class StdoutSink implements Sink {
	private String file;

	@Override
	public void zipLocation(File fromZip) {
		System.out.println("Zip file is " + fromZip);
	}

	@Override
	public void beginFile(String file) {
		this.file = file;
		System.out.println("Start of file " + file);
	}

	@Override
	public void card(String tag, int from, int to) {
		System.out.println("  card called " + tag + " from " + file + " pos " + from + " to " + to);
	}

	@Override
	public void holeid(String called, int from, int to) {
		System.out.println("    hole id " + called + " from " + from + " to " + to);
	}

	@Override
	public void hole(int from, int to) {
		System.out.println("    hole from " + from + " to " + to);
	}

	@Override
	public void identityAttr(String called, int from, int to) {
		System.out.println("    id attr '" + called + "' at " + from + "-" + to);
	}

	@Override
	public void dodgyAttr(int from, int to) {
		System.out.println("    attr removed from " + from + " to " + to);
	}

	@Override
	public void fileEnd() {
		System.out.println("----");
	}
}
