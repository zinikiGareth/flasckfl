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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.MetaEntry;
import org.ziniki.splitter.SplitMetaData;
import org.zinutils.utils.FileUtils;

import com.google.gson.JsonObject;

public class Root implements CardDataListener {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final FLASLanguageClient client;
	private final ErrorReporter errors;
	private final TaskQueue taskQ;
	public final URI uri;
	public final File root;
	private final TreeSet<File> files = new TreeSet<File>(new WorkspaceFileNameComparator());
	private FLASCompiler compiler;

	public Root(FLASLanguageClient client, ErrorReporter errors, TaskQueue taskQ, URI uri) {
		this.client = client;
		this.errors = errors;
		this.taskQ = taskQ;
		this.uri = uri;
		this.root = new File(uri.getPath());
	}

	public void configure(File flasHome) {
		logger.info("configuring " + root + " with flas home " + flasHome);
		Configuration config = new Configuration(errors, new String[] {});
		config.projectDir = this.root;
		config.includeFrom.add(new File(flasHome, "flim"));
		config.includeFrom.add(new File(flasHome, "userflim"));
		Repository repository = new Repository();
		compiler = new FLASCompiler(config, errors, repository, this);
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

	public void compileAll() {
		for (File f : files) {
			taskQ.submit(new CompileTask(compiler, uri.resolve(f.getPath()), null));
		}
	}

	public void dispatch(URI uri, String text) {
		if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath())))) {
			logger.info("Submitting file for compilation for " + uri);
			taskQ.submit(new CompileTask(compiler, uri, text));
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
}
