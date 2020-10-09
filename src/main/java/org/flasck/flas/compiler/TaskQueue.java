package org.flasck.flas.compiler;

import org.flasck.flas.lsp.CompileTask;

public interface TaskQueue {

	boolean isReady();

	void submit(CompileTask task);

}
