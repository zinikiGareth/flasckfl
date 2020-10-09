package org.flasck.flas;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.lsp.CompileTask;

public class LSPTaskQueue implements TaskQueue {
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
	private final Executor exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, tasks);

	public LSPTaskQueue() {
	}
	
	@Override
	public boolean isReady() {
		return tasks.isEmpty();
	}

	@Override
	public void submit(CompileTask ct) {
		if (ct != null && !tasks.contains(ct)) {// don't add things multiple times
			exec.execute(ct);
		}
	}

}
