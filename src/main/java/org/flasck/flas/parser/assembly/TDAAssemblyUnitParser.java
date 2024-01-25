package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.LibraryAssembly;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAAssemblyUnitParser extends BlockLocationTracker implements TDAParsing {
	private final TopLevelNamer namer;
	private final AssemblyDefinitionConsumer adc;

	public TDAAssemblyUnitParser(ErrorReporter errors, TopLevelNamer namer, AssemblyDefinitionConsumer adc, LocationTracker parentTracker) {
		super(errors,parentTracker);
		this.namer = namer;
		this.adc = adc;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "expected 'application', 'card' or 'library'");
			return new IgnoreNestedParser(errors);
		}
		
		switch (kw.text) {
		case "application": {
			ApplicationAssembly consumer = new ApplicationAssembly(kw.location, namer.assemblyName(null), adc);
			adc.assembly(consumer);
			errors.logReduction("fa-application-intro", kw.location, kw.location);
			super.tellParent(kw.location);
			return new TDAParsingWithAction(new ApplicationElementParser(errors, kw.location, namer, consumer, this), reduction(kw.location, "fa-application"));
		}
		case "card": {
			throw new NotImplementedException();
		}
		case "library": {
			LibraryAssembly consumer = new LibraryAssembly(kw.location, namer.assemblyName(null));
			adc.assembly(consumer);
			errors.logReduction("fa-library", kw.location, kw.location);
			super.tellParent(kw.location);
			return new NoNestingParser(errors);
		}
		default: {
			errors.message(toks, "expected 'application', 'card' or 'library'");
			return new IgnoreNestedParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
