package org.flasck.flas.compiler;

import java.net.URI;

public interface TaskQueue {
	void submit(Runnable task);
	void readyWhenYouAre(URI uri, CompileUnit stage2);
	void loadFLIM(URI uri, CompileUnit compiler);
	void waitToDrain() throws InterruptedException;
}
