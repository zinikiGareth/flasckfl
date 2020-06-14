package org.flasck.flas.compiler.templates;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.templates.EventPlacement.HandlerInfo;
import org.flasck.flas.compiler.templates.EventPlacement.TemplateTarget;
import org.flasck.flas.parsedForm.TemplateBinding;

public interface EventTargetZones {
	void handler(String eventType, FunctionName handlerName);
	void binding(String id, TemplateBinding currentBinding, int option, String handler, Integer cond);
	Iterable<String> templateNames();
	Iterable<TemplateTarget> targets(String t);
	HandlerInfo getHandler(String handler);
	List<HandlerInfo> unboundHandlers();
}
