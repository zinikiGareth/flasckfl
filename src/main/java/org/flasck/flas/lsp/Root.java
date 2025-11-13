package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.util.TreeSet;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.CardDataListener;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.MetaEntry;
import org.ziniki.splitter.SplitMetaData;
import org.zinutils.hfs.HFSFile;
import org.zinutils.hfs.HFSFolder;
import org.zinutils.hfs.HierarchicalFileSystem;
import org.zinutils.utils.FileUtils;

import com.google.gson.JsonObject;

public class Root implements CardDataListener {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final FLASLanguageClient client;
	private final ErrorReporter errors;
	private final TaskQueue taskQ;
	private final URI uri;
	private final HFSFolder root;
	private final TreeSet<HFSFile> files = new TreeSet<HFSFile>(new WorkspaceFileNameComparator());
	private FLASCompiler compiler;

	public Root(FLASLanguageClient client, ErrorReporter errors, TaskQueue taskQ, HierarchicalFileSystem hfs, URI uri) {
		this.client = client;
		this.errors = errors;
		this.taskQ = taskQ;
		this.uri = uri;
		this.root = hfs.root(uri);
	}
	
	public String getPath() {
		return uri.getPath();
	}

	public void configure(HierarchicalFileSystem hfs) {
		logger.info("configuring " + root + " with hfs " + hfs);
		Configuration config = new Configuration(errors, new String[] {});
//		config.projectDir = this.root;
//		config.includeFrom.add(new File(flasHome, "flim"));
//		config.includeFrom.add(new File(flasHome, "userflim"));
		Repository repository = new Repository();
		compiler = new FLASCompiler(config, errors, repository, this);
		compiler.taskQueue(taskQ);
		taskQ.loadFLIM(uri, compiler);
	}

	public void setCardsFolder(String cardsFolder) {
		/*
		if (cardsFolder == null)
			compiler.setCardsFolder(null);
		else
			compiler.setCardsFolder(new File(root, cardsFolder));
*/
		logger.error("SETTING CARDS FOLDER - deprecated?");
		// and force a rebuild of Stage 2
		taskQ.readyWhenYouAre(uri, compiler);
	}

	public void gatherFiles() {
		files.clear();
		for (HFSFile f : root.findFilesUnderMatching("*")) {
			if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((f.getName())))) {
				files.add(f);
			}
		}
		for (HFSFile f : files) {
			errors.logMessage("gathered " + f.getName());
		}
	}

	public void compileAll() {
		for (HFSFile f : files) {
			taskQ.submit(new CompileTask(errors, compiler, uri.resolve(f.getPath()), null));
		}
	}

	public void dispatch(URI uri, String text) {
		if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath())))) {
			logger.info("Submitting file for compilation for " + uri);
			taskQ.submit(new CompileTask(errors, compiler, uri, text));
		}
	}

	@Override
	public void provideWebData(SplitMetaData md) {
		JsonObject cards = new JsonObject();
		for (String card : md) {
			JsonObject fields = new JsonObject();
			CardData cd = md.forCard(card);
			for (MetaEntry me : cd) {
				fields.addProperty(me.key(), me.value().toString());
			}
			cards.add(card, fields);
		}
		JsonObject send = new JsonObject();
		send.addProperty("uri", uri.toString().replaceAll("/*$", ""));
		send.add("cards", cards);
		client.sendCardInfo(send);
	}

	public ContentObject fileCO(URI uri) {
		String path = uri.getPath();
		path = path.replace(this.uri.getPath(), "");
		File f = new File(path);
		return new FileContentObject(root.getFileContents(f));
	}
}
