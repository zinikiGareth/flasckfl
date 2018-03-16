package org.flasck.flas.htmlzip;

public class StdoutSink implements Sink {

	@Override
	public void block(String tag, int from, int to) {
		System.out.println("block called " + tag + " from " + from + " to " + to);
	}

	@Override
	public void hole(int from, int to) {
		System.out.println("hole from " + from + " to " + to);
	}

	@Override
	public void dodgyAttr(String attr, int from, int to) {
		System.out.println("attr " + attr + " identified from " + from + " to " + to);
	}

}
