package org.flasck.flas.htmlzip;

public class MultiSink implements Sink {
	private final Sink[] sinks;

	public MultiSink(Sink...sinks) {
		this.sinks = sinks;
	}

	@Override
	public void beginFile(String file) {
		for (Sink s : sinks)
			s.beginFile(file);
	}

	@Override
	public void card(String tag, int from, int to) {
		for (Sink s : sinks)
			s.card(tag, from, to);
	}

	@Override
	public void hole(String called, int from, int to) {
		for (Sink s : sinks)
			s.hole(called, from, to);
	}

	@Override
	public void identifyElement(String called, int from, int to) {
		for (Sink s : sinks)
			s.identifyElement(called, from, to);
	}

	@Override
	public void dodgyAttr(int from, int to) {
		for (Sink s : sinks)
			s.dodgyAttr(from, to);
	}

	@Override
	public void fileEnd() {
		for (Sink s : sinks)
			s.fileEnd();
	}

}
