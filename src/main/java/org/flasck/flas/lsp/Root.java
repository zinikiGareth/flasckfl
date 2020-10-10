package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.util.TreeSet;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.zinutils.utils.FileUtils;

public class Root {
	private final ErrorReporter errors;
	private final TaskQueue taskQ;
	public final URI uri;
	public final File root;
	private final TreeSet<File> files = new TreeSet<File>(new WorkspaceFileNameComparator());
	private FLASCompiler compiler;

	public Root(ErrorReporter errors, TaskQueue taskQ, URI uri) {
		this.errors = errors;
		this.taskQ = taskQ;
		this.uri = uri;
		this.root = new File(uri.getPath());
	}
	
	public void configure(File flasHome) {
		Configuration config = new Configuration(errors, new String[] {});
		config.includeFrom.add(new File(flasHome, "stdlib/flim"));
		config.includeFrom.add(new File(flasHome, "stdlib/jsout"));
		config.includeFrom.add(new File(flasHome, "stdlib/jvmout"));
        Repository repository = new Repository();
		compiler = new FLASCompiler(config, errors, repository);
		compiler.taskQueue(taskQ);
		taskQ.loadFLIM(uri, compiler);
	}

	public void setCardsFolder(String cardsFolder) {
		if (cardsFolder == null)
			compiler.setCardsFolder(null);
		else
			compiler.setCardsFolder(new File(root, cardsFolder));
		
		// and force a rebuild of Stage 2
		taskQ.readyWhenYouAre(uri, compiler);
	}

	public void gatherFiles() {
		files.clear();
		for (File f : FileUtils.findFilesUnderMatching(root, "*")) {
			if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((f.getName())))) {
				files.add(f);
			}
		}
		for (File f : files) {
			errors.logMessage("gathered " + f);
		}
	}

	public void compileAll(Root root) {
		for (File f : files) {
			taskQ.submit(new CompileTask(compiler, uri.resolve(f.getPath()), null));
		}
	}
	public void dispatch(URI uri, String text) {
    	if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath()))))
    		taskQ.submit(new CompileTask(compiler, uri, text));
	}
}
