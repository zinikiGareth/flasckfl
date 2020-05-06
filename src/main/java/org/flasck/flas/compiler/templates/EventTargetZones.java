package org.flasck.flas.compiler.templates;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.templates.EventPlacement.HandlerInfo;
import org.flasck.flas.compiler.templates.EventPlacement.TemplateTarget;
import org.flasck.flas.parsedForm.TemplateBinding;

public interface EventTargetZones {
	void handler(String eventType, FunctionName handlerName);
	void binding(String id, TemplateBinding currentBinding, String handler);
	Iterable<String> templateNames();
	Iterable<String> handledEvents();
	Iterable<FunctionName> handling(String ev);
	Iterable<TemplateTarget> targets(String t);
	HandlerInfo getHandler(String handler);
}
