package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.modules.ParserModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.FunctionAssembler;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionScopeNamer;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAHandlerParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAMethodParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.parser.assembly.AssemblyDefinitionConsumer;
import org.flasck.flas.parser.assembly.TDAAssemblyUnitParser;
import org.flasck.flas.parser.st.SystemTestDefinitionConsumer;
import org.flasck.flas.parser.st.SystemTestNamer;
import org.flasck.flas.parser.st.SystemTestPackageNamer;
import org.flasck.flas.parser.st.SystemTestStepParser;
import org.flasck.flas.parser.st.TDASystemTestParser;
import org.flasck.flas.parser.st.TDASystemTestParser.OptionsRecorder;
import org.flasck.flas.parser.ut.TDAUnitTestParser;
import org.flasck.flas.parser.ut.TestStepNamer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.jvm.ziniki.ContentObject;

public class ParsingPhase implements ParserScanner {
	private final ErrorReporter errors;
	private final Blocker blocker;

	public ParsingPhase(ErrorReporter errors, String inPkg, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules) {
		this.errors = errors;
		TDANester story = new TDANester(errors, topLevelUnit(errors, new PackageNamer(inPkg), tldc, modules));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, UnitTestFileName fn, UnitTestDefinitionConsumer utdc) {
		this.errors = errors;
		TDANester story = new TDANester(errors, unitTestUnit(errors, new UnitTestPackageNamer(fn), utdc));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, UnitTestFileName fn, SystemTestDefinitionConsumer stdc, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules) {
		this.errors = errors;
		TDANester story = new TDANester(errors, systemTestUnit(errors, new SystemTestPackageNamer(fn), stdc, tldc, modules, null));
		this.blocker = new Blocker(errors, story);
	}

	public ParsingPhase(ErrorReporter errors, String inPkg, AssemblyDefinitionConsumer adc) {
		this.errors = errors;
		TDANester story = new TDANester(errors, assemblyUnit(errors, new PackageNamer(inPkg), adc));
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
	
	public static TDAParsing topLevelUnit(ErrorReporter errors, TopLevelNamer namer, TopLevelDefinitionConsumer sb, Iterable<ParserModule> modules) {
		FunctionAssembler assembler = new FunctionAssembler(errors, sb, null, null);
		TDAMultiParser ret = new TDAMultiParser(errors,
			TDAIntroParser.constructor(namer, sb),
			TDAFunctionParser.constructor(namer, (pos, x, cn) -> namer.functionCase(pos, x, cn), assembler, sb, null, assembler),
			TDATupleDeclarationParser.constructor(namer, sb, null, assembler));
		for (ParserModule m : modules) {
			TDAParsing r = m.introParser(errors, namer, sb);
			if (r != null) {
				// add it at the front because TDAFunctionParser swallows up everything
				ret.add(0, r);
			}
		}
		return ret;
	}
	
	public static TDAParsing unitTestUnit(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer utdc) {
		return new TDAUnitTestParser(errors, namer, utdc, utdc);
	}

	public static TDAParsing systemTestUnit(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer stdc, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules, LocationTracker locTracker) {
		TDAMultiParser ret = new TDAMultiParser(errors);
		OptionsRecorder rec = new OptionsRecorder();
		BlockLocationTracker blt = new BlockLocationTracker(errors, locTracker);
		ret.add(new TDASystemTestParser(errors, namer, stdc, tldc, modules, blt, rec));
		for (ParserModule m : modules) {
			TDAParsing r = m.systemTestParser(errors, namer, stdc, tldc);
			if (r != null)
				ret.add(r);
		}
		return new TDAParsingWithAction(ret, () -> {
			if (rec.firstLoc() != null)
				blt.reduce(rec.firstLoc().location(), rec.toString());
		});
	}

	public static TDAParsing systemTestStep(ErrorReporter errors, TestStepNamer namer, SystemTestStage stg, TopLevelDefinitionConsumer topLevel, Iterable<ParserModule> modules, LocationTracker locTracker) {
		TDAMultiParser ret = new TDAMultiParser(errors);
		ret.add(new SystemTestStepParser(errors, namer, stg, topLevel, locTracker));
		for (ParserModule m : modules) {
			TDAParsing r = m.systemTestStepParser(errors, namer, stg, topLevel, locTracker);
			if (r != null)
				ret.add(r);
		}
		return ret;
	}

	public static TDAParsing assemblyUnit(ErrorReporter errors, TopLevelNamer namer, AssemblyDefinitionConsumer adc) {
		return new TDAAssemblyUnitParser(errors, namer, adc, null);
	}
	
	public static TDAParsing functionScopeUnit(ErrorReporter errors, FunctionScopeNamer namer, FunctionIntroConsumer sb, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		return new TDAMultiParser(errors, TDAHandlerParser.constructor(null, namer, topLevel, holder, locTracker), TDAMethodParser.constructor(namer, sb, topLevel, holder, locTracker), TDAFunctionParser.constructor(namer, (pos, x, cn) -> namer.functionCase(pos, x, cn), sb, topLevel, holder, locTracker), TDATupleDeclarationParser.constructor(namer, topLevel, holder, locTracker));
	}
}
