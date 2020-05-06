package org.flasck.flas.web;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.web.EventPlacement.HandlerInfo;
import org.flasck.flas.web.EventPlacement.TemplateTarget;

public interface EventTargetZones {
	void handler(String eventType, FunctionName handlerName);
	void binding(String id, TemplateBinding currentBinding, String handler);
	Iterable<String> templateNames();
	Iterable<String> handledEvents();
	Iterable<FunctionName> handling(String ev);
	Iterable<TemplateTarget> targets(String t);
	HandlerInfo getHandler(String handler);
}
