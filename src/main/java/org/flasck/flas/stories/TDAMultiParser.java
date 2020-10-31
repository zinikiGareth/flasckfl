package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.ParserModule;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.FunctionAssembler;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionScopeNamer;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
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
import org.flasck.flas.parser.st.SystemTestStepParser;
import org.flasck.flas.parser.st.TDASystemTestParser;
import org.flasck.flas.parser.ut.TDAUnitTestParser;
import org.flasck.flas.parser.ut.TestStepNamer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMultiParser implements TDAParsing {
	private final ErrorReporter errors;
	private final List<TDAParsing> parsers = new ArrayList<>();

	@SafeVarargs
	public TDAMultiParser(ErrorReporter errors, TDAParserConstructor... klz) {
		this.errors = errors;
		for (TDAParserConstructor k : klz)
			parsers.add(k.construct(errors));
	}

	// needs to return the parser that will do the children
	public TDAParsing tryParsing(Tokenizable toks) {
		ErrorMark mark = errors.mark();
		for (TDAParsing p : parsers) {
			toks.reset(0);
			final TDAParsing nested = p.tryParsing(toks);
			if (nested != null)
				return nested;
		}
		toks.reset(0);
		if (!mark.hasMoreNow())
			errors.message(toks, "syntax error");
		return new IgnoreNestedParser();
	}

	public static TDAParsing topLevelUnit(ErrorReporter errors, TopLevelNamer namer, TopLevelDefinitionConsumer sb, Iterable<ParserModule> modules) {
		FunctionIntroConsumer assembler = new FunctionAssembler(errors, sb, null);
		TDAMultiParser ret = new TDAMultiParser(errors, TDAIntroParser.constructor(namer, sb), TDAFunctionParser.constructor(namer, (pos, x, cn) -> namer.functionCase(pos, x, cn), assembler, sb, null), TDATupleDeclarationParser.constructor(namer, sb, null));
		for (ParserModule m : modules) {
			TDAParsing r = m.introParser(errors, namer, sb);
			if (r != null)
				ret.parsers.add(r);
		}
		return ret;
	}
	
	public static TDAParsing unitTestUnit(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer utdc) {
		return new TDAUnitTestParser(errors, namer, utdc, utdc);
	}

	public static TDAParsing systemTestUnit(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer stdc, TopLevelDefinitionConsumer tldc, Iterable<ParserModule> modules) {
		TDAMultiParser ret = new TDAMultiParser(errors);
		ret.parsers.add(new TDASystemTestParser(errors, namer, stdc, tldc, modules));
		for (ParserModule m : modules) {
			TDAParsing r = m.systemTestParser(errors, namer, stdc, tldc);
			if (r != null)
				ret.parsers.add(r);
		}
		return ret;
	}

	public static TDAParsing systemTestStep(ErrorReporter errors, TestStepNamer namer, SystemTestStage stg, TopLevelDefinitionConsumer topLevel, Iterable<ParserModule> modules) {
		TDAMultiParser ret = new TDAMultiParser(errors);
		ret.parsers.add(new SystemTestStepParser(errors, namer, stg, topLevel));
		for (ParserModule m : modules) {
			TDAParsing r = m.systemTestStepParser(errors, namer, stg, topLevel);
			if (r != null)
				ret.parsers.add(r);
		}
		return ret;
	}

	public static TDAParsing assemblyUnit(ErrorReporter errors, TopLevelNamer namer, AssemblyDefinitionConsumer adc) {
		return new TDAAssemblyUnitParser(errors, namer, adc);
	}
	
	public static TDAParsing functionScopeUnit(ErrorReporter errors, FunctionScopeNamer namer, FunctionIntroConsumer sb, FunctionScopeUnitConsumer topLevel, StateHolder holder) {
		return new TDAMultiParser(errors, TDAHandlerParser.constructor(null, namer, topLevel, holder), TDAMethodParser.constructor(namer, sb, topLevel, holder), TDAFunctionParser.constructor(namer, (pos, x, cn) -> namer.functionCase(pos, x, cn), sb, topLevel, holder), TDATupleDeclarationParser.constructor(namer, topLevel, holder));
	}

	// I added this method for testing purposes
	public boolean contains(Class<?> cls) {
		for (TDAParsing p : parsers)
			if (cls.isInstance(p))
				return true;
		return false;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		for (TDAParsing p : parsers)
			p.scopeComplete(location);
	}
}
