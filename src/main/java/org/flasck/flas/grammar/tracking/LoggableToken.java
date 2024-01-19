package org.flasck.flas.grammar.tracking;

import org.flasck.flas.commonBase.Locatable;

public interface LoggableToken extends Locatable {
	public String type();
	public String text();
}
