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
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.Repository;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.utils.FileUtils;

public class FlimReader {
	private final ErrorResult errors;
	private final Repository repository;

	public FlimReader(ErrorResult errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	public void read(DirectedAcyclicGraph<String> pkgs, File flimdir, List<File> butNot) {
		List<String> ignore = new ArrayList<>();
		for (File i : butNot)
			ignore.add(i.getName());
		FileUtils.assertDirectory(flimdir);
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
		for (FlimTop ft : importers) {
			ft.resolve();
		}
	}

	private FlimTop importFlim(File f) {
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
				errors.message(new InputPosition(f.getName(), lnr.getLineNumber(), -1, null), ex.toString());
			}
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null), "file does not exist");
		} catch (IOException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null), ex.toString());
		} catch (Throwable t) {
			errors.reportException(t);
		}
		return ret;
	}
}
