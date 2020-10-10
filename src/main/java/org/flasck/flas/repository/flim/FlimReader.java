package org.flasck.flas.repository.flim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.utils.FileUtils;

public class FlimReader {
	private final ErrorReporter errors;
	private final Repository repository;

	public FlimReader(ErrorReporter errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	public void read(DirectedAcyclicGraph<String> pkgs, File flimdir, List<File> butNot) {
		if (!flimdir.exists())
			return;
		
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
		for (File f : FileUtils.findFilesMatching(flimdir, "*")) {
			String name = f.getName();
			if (!ignore.contains(name)) {
				FlimTop importer = importFlim(f);
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
			ft.resolve();
		}
	}

	private FlimTop importFlim(File f) {
		System.out.println("importing flim file " + f.getName());
		FlimTop ret = new FlimTop(errors, repository, f.getName());
		Blocker blocker = new Blocker(errors, new TDANester(ret));
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(f))) {
			String s;
			try {
				blocker.newFile();
				while ((s = lnr.readLine()) != null)
					blocker.present(f.getName(), lnr.getLineNumber(), s);
				blocker.flush();
			} catch (IOException ex) {
				errors.message(new InputPosition(f.getName(), lnr.getLineNumber(), -1, null, null), ex.toString());
			}
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null, null), "file does not exist");
		} catch (IOException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null, null), ex.toString());
		} catch (Throwable t) {
			errors.reportException(t);
		}
		return ret;
	}
}
