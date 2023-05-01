package org.flasck.flas.compiler;

import java.net.URI;

public interface CompileUnit {
	public void parse(URI uri, String text);
	public void attemptRest(URI uri);
}
