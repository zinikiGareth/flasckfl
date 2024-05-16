package org.flasck.flas.repository.flim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.utils.FileUtils;

public class FlimReader {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final ErrorReporter errors;
	private final Repository repository;

	public FlimReader(ErrorReporter errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	public void read(DirectedAcyclicGraph<String> pkgs, File flimdir, List<File> butNot) {
		if (!flimdir.exists()) {
			logger.warn("cannot read flim dir " + flimdir);
			return;
		}
		
		List<String> ignore = new ArrayList<>();
		for (File i : butNot)
			ignore.add(i.getName());

		// Don't read any flim files if we are building the stdlib
		if (ignore.contains("root.package"))
			return;
		
		// TODO: we also need a slightly more complicated check that we aren't reading something that depends on us
		// This requires us to actually read the file to get to the uses, but ideally not do ANYTHING with it
		// But certainly we want to avoid resolving it
		
		Set<FlimTop> importers = new HashSet<>();
		for (File f : FileUtils.findFilesMatching(flimdir, "*.flim")) {
			String name = FileUtils.dropExtension(f.getName());
			if (!ignore.contains(name) && !pkgs.hasNode(name)) {
				logger.info("reading flim file for " + name + " from " + f);
				repository.readingFLIM(name);
				FlimTop importer = importFlim(f, name);
				if (importer == null)
					continue;
				importers.add(importer);
				pkgs.ensure(name);
				for (String s : importer.uses) {
					pkgs.ensure(s);
					pkgs.ensureLink(name, s);
				}
			}
		}
		if (errors.hasErrors())
			return;
		
		for (FlimTop ft : importers) {
			repository.selectPackage(ft.pkgName());
			ft.resolve();
		}
	}


	public FlimTop read(DirectedAcyclicGraph<String> pkgs, String pkg, ContentObject co) {
		FlimTop importer = importFlim(new InputStreamReader(co.asStream()), pkg, pkg);
		pkgs.ensure(pkg);
		for (String s : importer.uses) {
			pkgs.ensure(s);
			pkgs.ensureLink(pkg, s);
		}
		return importer;
	}

	private FlimTop importFlim(File f, String name) {
		String fn = f.getName();
		try {
			FileReader r = new FileReader(f);
			return importFlim(r, name, fn);
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(fn, -1, -1, null, null), "file does not exist");
			return null;
		}
	}

	private FlimTop importFlim(Reader r, String name, String fn) {
		FlimTop ret = new FlimTop(errors, repository, name);
		Blocker blocker = new Blocker(errors, new TDANester(errors, ret));
		try (LineNumberReader lnr = new LineNumberReader(r)) {
			String s;
			try {
				blocker.newFile();
				while ((s = lnr.readLine()) != null)
					blocker.present(fn, lnr.getLineNumber(), s);
				blocker.flush();
				return ret;
			} catch (IOException ex) {
				errors.message(new InputPosition(fn, lnr.getLineNumber(), -1, null, null), ex.toString());
			}
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(fn, -1, -1, null, null), "file does not exist");
		} catch (IOException ex) {
			errors.message(new InputPosition(fn, -1, -1, null, null), ex.toString());
		} catch (Throwable t) {
			errors.reportException(t);
		}
		return null;
	}
}
