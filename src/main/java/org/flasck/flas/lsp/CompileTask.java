package org.flasck.flas.lsp;

import java.net.URI;

import org.flasck.flas.compiler.CompileUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: extract most of this somewhere else (say "FLASCompiler")
// "run" here should invoke that with the minimal amount of extra information
// get it so that we do the remaining steps
public class CompileTask implements Runnable {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final CompileUnit compiler;
	private final URI uri;
	private final String text;

	public CompileTask(CompileUnit compiler, URI uri, String text) {
		this.compiler = compiler;
		this.uri = uri;
		this.text = text;
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof CompileTask && uri.equals(((CompileTask)obj).uri);
	}

	@Override
	public void run() {
		try {
			compiler.parse(uri, text);
		} catch (Throwable t) {
			logger.error("Exception processing " + uri, t);
		}
	}
	
	@Override
	public String toString() {
		return "CompileTask[" + uri + "]";
	}
}
