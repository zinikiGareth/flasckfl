package org.flasck.flas.compiler;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.parser.st.SystemTestDefinitionConsumer;
import org.flasck.flas.parser.st.SystemTestNamer;
import org.flasck.flas.parser.ut.TestStepNamer;

public interface ParserModule {
	TDAParsing introParser(ErrorReporter errors, TopLevelNamer namer, TopLevelDefinitionConsumer consumer);
	TDAParsing systemTestParser(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer stdc,	TopLevelDefinitionConsumer tldc);
	TDAParsing systemTestStepParser(ErrorReporter errors, TestStepNamer namer, SystemTestStage stg, TopLevelDefinitionConsumer topLevel);
}
