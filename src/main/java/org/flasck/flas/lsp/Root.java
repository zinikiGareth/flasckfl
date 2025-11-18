package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.util.TreeSet;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.CardDataListener;
import org.flasck.flas.compiler.CompileUnit;
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
	private final TreeSet<HFSFile> flasfiles = new TreeSet<>(new FLASFileNameComparator());
	private final TreeSet<HFSFile> uifiles = new TreeSet<>(new UIFileNameComparator());
	private final TreeSet<HFSFolder> flasfolders = new TreeSet<>(new FLASFileNameComparator());
	private HFSFolder uifolder = null;
	private CompileUnit compiler;

	public Root(FLASLanguageClient client, ErrorReporter errors, TaskQueue taskQ, HierarchicalFileSystem hfs, URI uri) {
		this.client = client;
		this.errors = errors;
		this.taskQ = taskQ;
		this.uri = uri;
		this.root = hfs.root(uri);
	}
	
	public void useCompiler(CompileUnit c) {
		this.compiler = c;
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

	public void gatherFiles() {
		flasfiles.clear();
		for (HFSFile f : root.findFilesUnderMatching("*")) {
			if (FLASFileNameComparator.isValidExtension(FileUtils.extension((f.getName())))) {
				flasfiles.add(f);
			}
			if (UIFileNameComparator.isValidExtension(FileUtils.extension((f.getName())))) {
				uifiles.add(f);
			}
		}
		for (HFSFile f : flasfiles) {
			errors.logMessage("gathered " + f.getName());
		}
		for (HFSFile f : uifiles) {
			errors.logMessage("gathered " + f.getName());
		}
		figureFolders();
		for (HFSFolder f : flasfolders) {
			logger.info("determined flas dir " + f.getPath());
		}
		if (uifolder != null) {
			logger.info("determined ui folder to be " + uifolder.getPath());
			compiler.setCardsFolder(uifolder);
		}
	}

	private void figureFolders() {
		flasfolders.clear();
		for (HFSFile f : flasfiles) {
			logger.info("have file " + f.getPath());
			flasfolders.add(f.getFolder());
		}
		uifolder = null;
		for (HFSFile f : uifiles) {
			logger.info("have file " + f.getPath());
			HFSFolder tmp = f.getFolder();
			if (uifolder == null) {
				uifolder = tmp;
			} else if (uifolder.equals(tmp)) {
				;
			} else {
				logger.info("huh");
				while (!uifolder.equals(tmp)) {
					if (uifolder.getPath().getPath().length() > tmp.getPath().getPath().length()) {
						uifolder = uifolder.getFolder();
					} else {
						tmp = tmp.getFolder();
					}
				}
			}
		}
	}
	
	public void compileAll() {
		for (HFSFile f : flasfiles) {
			taskQ.submit(new CompileTask(errors, compiler, uri.resolve(f.getPath()), null));
		}
	}

	public void dispatch(URI uri, String text) {
		if (FLASFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath())))) {
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
		return new FileContentObject(uri.getPath(), f.getName(), root.getFileContents(f));
	}
}
