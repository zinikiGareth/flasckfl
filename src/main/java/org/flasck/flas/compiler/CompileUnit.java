package org.flasck.flas.compiler;

import java.net.URI;

public interface CompileUnit {
	public void parse(URI uri, String string);
	public void attemptRest(URI uri);
}
