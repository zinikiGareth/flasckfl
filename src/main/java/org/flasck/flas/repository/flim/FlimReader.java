package org.flasck.flas.repository.flim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.Repository;
import org.zinutils.utils.FileUtils;

public class FlimReader {
	private final ErrorResult errors;
	private final Repository repository;

	public FlimReader(ErrorResult errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	public void read(File flimdir, List<File> butNot) {
		List<String> ignore = new ArrayList<>();
		for (File i : butNot)
			ignore.add(i.getName());
		FileUtils.assertDirectory(flimdir);
		for (File f : FileUtils.findFilesMatching(flimdir, "*")) {
			if (!ignore.contains(f.getName()))
				importFlim(f);
		}
	}

	private void importFlim(File f) {
		Blocker blocker = new Blocker(errors, new TDANester(new FlimTop(errors, repository, f.getName())));
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(f))) {
			String s;
			try {
				blocker.newFile();
				while ((s = lnr.readLine()) != null)
					blocker.present(f.getName(), lnr.getLineNumber(), s);
				blocker.flush();
			} catch (IOException ex) {
				errors.message(new InputPosition(f.getName(), lnr.getLineNumber(), -1, null), ex.toString());
				return;
			}
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null), "file does not exist");
		} catch (IOException ex) {
			errors.message(new InputPosition(f.getName(), -1, -1, null), ex.toString());
		} catch (Throwable t) {
			errors.reportException(t);
		}
	}
}
