package org.flasck.flas.htmlzip;

public class StdoutSink implements Sink {
	private String file;

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
	public void hole(String called, int from, int to) {
		System.out.println("    hole " + called + " from " + from + " to " + to);
	}

	@Override
	public void identifyElement(String called, int from, int to) {
		System.out.println("    element " + called + " at " + from + "-" + to);
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
