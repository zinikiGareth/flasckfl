package org.flasck.flas.grammar.tracking;

import org.flasck.flas.blockForm.InputPosition;

public interface LoggableToken {
	public InputPosition location();
	public String type();
	public String text();
}
