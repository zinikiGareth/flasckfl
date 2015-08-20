package org.flasck.flas.rewriter;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class ResolutionException extends RuntimeException {
	public final String name;
	public final InputPosition location;

	public ResolutionException(InputPosition location, String name) {
		super("Could not resolve name " + name);
		this.location = location;
		this.name = name;
	}
}
