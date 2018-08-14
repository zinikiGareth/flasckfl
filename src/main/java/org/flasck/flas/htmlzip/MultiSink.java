package org.flasck.flas.htmlzip;

import java.io.File;

public class MultiSink implements Sink {
	private final Sink[] sinks;

	public MultiSink(Sink...sinks) {
		this.sinks = sinks;
	}

	@Override
	public void zipLocation(File fromZip) {
		for (Sink s : sinks)
			s.zipLocation(fromZip);
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
	public void holeid(String called, int from, int to) {
		for (Sink s : sinks)
			s.holeid(called, from, to);
	}

	@Override
	public void hole(int from, int to) {
		for (Sink s : sinks)
			s.hole(from, to);
	}

	@Override
	public void identityAttr(String called, int from, int to) {
		for (Sink s : sinks)
			s.identityAttr(called, from, to);
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
