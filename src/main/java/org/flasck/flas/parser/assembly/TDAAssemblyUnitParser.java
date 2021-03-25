package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.LibraryAssembly;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAAssemblyUnitParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TopLevelNamer namer;
	private final AssemblyDefinitionConsumer adc;

	public TDAAssemblyUnitParser(ErrorReporter errors, TopLevelNamer namer, AssemblyDefinitionConsumer adc) {
		this.errors = errors;
		this.namer = namer;
		this.adc = adc;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			errors.message(toks, "expected 'application', 'card' or 'library'");
			return new IgnoreNestedParser();
		}
		
		switch (kw.text) {
		case "application": {
			ApplicationAssembly consumer = new ApplicationAssembly(kw.location, namer.assemblyName(null));
			adc.assembly(consumer);
			return new ApplicationElementParser(errors, kw.location, consumer);
		}
		case "card": {
			throw new NotImplementedException();
		}
		case "library": {
			LibraryAssembly consumer = new LibraryAssembly(kw.location, namer.assemblyName(null));
			adc.assembly(consumer);
			return new NoNestingParser(errors);
		}
		default: {
			errors.message(toks, "expected 'application', 'card' or 'library'");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
