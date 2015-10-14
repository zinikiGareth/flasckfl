package org.flasck.flas.rewriter;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class ResolutionException extends RuntimeException {
	public final String name;
	public final InputPosition location;

	public ResolutionException(InputPosition location, String name) {
		super("could not resolve name " + name);
		this.location = location;
		this.name = name;
	}

	public ResolutionException(InputPosition location, int k, String name) {
		super("could not find package " + name);
		this.location = location;
		this.name = name;
	}
}
