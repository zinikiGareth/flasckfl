package org.flasck.flas;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.flasck.flas.compiler.CompileUnit;
import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.lsp.CompileTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPTaskQueue implements TaskQueue {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
	private final ExecutorService exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, tasks);
	private final Set<CompileUnit> units = new HashSet<>();

	public LSPTaskQueue() {
	}
	
	@Override
	public void loadFLIM(URI uri, CompileUnit compiler) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				compiler.lspLoadFLIM(uri);
			}
		});
	}
	
	@Override
	public void submit(Runnable ct) {
		if (ct != null) {
			exec.execute(ct);
		}
	}

	@Override
	public synchronized void readyWhenYouAre(URI uri, CompileUnit stage2) {
		units.add(stage2);
		if (tasks.isEmpty()) {
			logger.info("Adding stage2 to workflow for " + uri);
			Set<CompileUnit> ready = new HashSet<>(units);
			units.clear();
			exec.execute(new Runnable() {
				@Override
				public void run() {
					logger.info("Attempting rest for " + ready);
					for (CompileUnit unit : ready) {
						unit.attemptRest(uri);
					}
				}
			});
		}
	}

	@Override
	public void waitToDrain() throws InterruptedException {
		exec.shutdown();
		exec.awaitTermination(5, TimeUnit.SECONDS);
	}
}
