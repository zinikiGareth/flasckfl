package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.stories.TDAMultiParser;

public class ParsingPhase implements ParserScanner {
	private final ErrorReporter errors;
	private final Blocker blocker;

	public ParsingPhase(ErrorReporter errors, String inPkg, TopLevelDefinitionConsumer sb) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.topLevelUnit(errors, new PackageNamer(inPkg), sb));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, UnitTestFileName fn, UnitTestDefinitionConsumer utdc) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.unitTestUnit(errors, new UnitTestPackageNamer(fn), utdc));
		this.blocker = new Blocker(errors, story);
	}

	@Override
	public void process(File f) {
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
