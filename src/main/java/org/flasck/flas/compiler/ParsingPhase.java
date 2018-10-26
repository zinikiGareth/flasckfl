package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ScopeBuilder;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.stories.TDAMultiParser;

public class ParsingPhase implements ParserScanner {
	private final ErrorReporter errors;
	private final Blocker blocker;

	public ParsingPhase(ErrorReporter errors, Phase2Processor p2) {
		this.errors = errors;
		// new Detoxer(errors, p2);
		ScopeBuilder sb = new ScopeBuilder(errors/*, detoxer */);
		TDANester story = new TDANester(new TDAMultiParser(errors, sb, TDAIntroParser.class /*, FunctionParser.class, TupleDeclarationParser.class */));
//		LineParser parser = new LineParser(errors/*, detoxer*/);
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
		}
	}

}
