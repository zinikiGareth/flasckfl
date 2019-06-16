package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.TemplateName;

public interface TemplateNamer extends FunctionScopeNamer, VarNamer {

	TemplateName template(String text);

	FunctionName ctor(InputPosition location, String text);

	FunctionName method(InputPosition loc, String text);

}
