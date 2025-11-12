package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.LSPTaskQueue;
import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.errors.ErrorReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPCore {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final ErrorReporter errors;
	private final File flasHome;
	private final TaskQueue taskQ;
	private final Map<String, Root> roots = new TreeMap<>();
	private String cardsFolder;
	private boolean readyToNotify;

	public LSPCore(ErrorReporter errors, File flasHome) {
		this.errors = errors;
		this.flasHome = flasHome;
		this.taskQ = new LSPTaskQueue();
	}

	public void addRoot(FLASLanguageClient client, String rootUri) {
		logger.info("opening root " + rootUri);
		try {
			synchronized (roots) {
				URI uri = new URI(rootUri + "/");
				Root root = new Root(client, errors, taskQ, uri);
				if (roots.containsKey(root.root.getPath())) {
					logger.warn("ignoring duplicate project " + root.root.getPath());
					return;
				}
				errors.logMessage("opening root " + root.root);
				roots.put(root.root.getPath(), root);
				root.configure(flasHome);
				root.setCardsFolder(cardsFolder);
			}
		} catch (URISyntaxException ex) {
			errors.logMessage("could not open " + rootUri);
		}
	}

	public void doParsing(boolean readyForNotifications) {
		this.readyToNotify |= readyForNotifications;
		if (!this.readyToNotify)
			return;
		synchronized (roots) {
			for (Root root : roots.values()) {
				root.gatherFiles();
				root.compileAll();
			}
		}
	}

	public void dispatch(URI uri, String text) {
		if (!this.readyToNotify)
			return;
		Root r = findRoot(uri);
		r.dispatch(uri, text);
	}
	
	private Root findRoot(URI uri) {
		synchronized (roots) {
			String path = uri.getPath();
			for (Entry<String, Root> e : roots.entrySet()) {
				if (path.startsWith(e.getKey()))
					return e.getValue();
			}
			return null;
		}
	}
	
	public void setCardsFolder(String cardsFolder) {
		logger.info("setting cards folder to " + cardsFolder);
		this.cardsFolder = cardsFolder;
		synchronized (roots) {
			for (Root r : roots.values())
				r.setCardsFolder(cardsFolder);
		}
	}

	public ErrorReporter errors() {
		return errors;
	}

	public void waitForTaskQueueToDrain() throws InterruptedException {
		taskQ.waitToDrain();
	}
}
