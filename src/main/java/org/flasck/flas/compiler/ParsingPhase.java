package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.modules.ParserModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.assembly.AssemblyDefinitionConsumer;
import org.flasck.flas.parser.st.SystemTestDefinitionConsumer;
import org.flasck.flas.parser.st.SystemTestPackageNamer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.jvm.ziniki.ContentObject;

public class ParsingPhase implements ParserScanner {
	private final ErrorReporter errors;
	private final Blocker blocker;

	public ParsingPhase(ErrorReporter errors, String inPkg, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.topLevelUnit(errors, new PackageNamer(inPkg), tldc, modules));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, UnitTestFileName fn, UnitTestDefinitionConsumer utdc) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.unitTestUnit(errors, new UnitTestPackageNamer(fn), utdc));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, UnitTestFileName fn, SystemTestDefinitionConsumer stdc, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.systemTestUnit(errors, new SystemTestPackageNamer(fn), stdc, tldc, modules));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, String inPkg, AssemblyDefinitionConsumer adc) {
		this.errors = errors;
		TDANester story = new TDANester(TDAMultiParser.assemblyUnit(errors, new PackageNamer(inPkg), adc));
		this.blocker = new Blocker(errors, story);
	}

	@Override
	public void process(ContentObject co) {
		String name = new File(co.key()).getName();
		try (LineNumberReader lnr = new LineNumberReader(new StringReader(co.asString()))) {
			String s;
			try {
				blocker.newFile();
				while ((s = lnr.readLine()) != null) {
					blocker.present(name, lnr.getLineNumber(), s);
				}
				blocker.flush();
			} catch (IOException ex) {
				errors.message(new InputPosition(name, lnr.getLineNumber(), -1, null, null), ex.toString());
				return;
			}
		} catch (FileNotFoundException ex) {
			errors.message(new InputPosition(name, -1, -1, null, null), "file does not exist");
		} catch (IOException ex) {
			errors.message(new InputPosition(name, -1, -1, null, null), ex.toString());
		} catch (Throwable t) {
			errors.reportException(t);
		}
	}
}
