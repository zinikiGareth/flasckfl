package org.flasck.flas.compiler;

import java.net.URI;

import org.flasck.flas.lsp.CompileTask;

public interface TaskQueue {
	void submit(CompileTask task);
	void readyWhenYouAre(URI uri, CompileUnit stage2);
	void loadFLIM(URI uri, CompileUnit compiler);
	void waitToDrain() throws InterruptedException;
}
