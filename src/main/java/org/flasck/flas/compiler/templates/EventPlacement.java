package org.flasck.flas.compiler.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.zinutils.collections.ListMap;

public class EventPlacement implements EventTargetZones {
	public class HandlerInfo {
		public final String event;
		public final FunctionName name;

		public HandlerInfo(String event, FunctionName name) {
			this.event = event;
			this.name = name;
		}
	}
	
	public class TemplateTarget {
		public final String slot;
		public final String type;
		public final int option;
		public final String handler;

		public TemplateTarget(String slot, String type, int option, String handler) {
			this.slot = slot;
			this.type = type;
			this.option = option;
			this.handler = handler;
		}
	}
	
	private Map<String, HandlerInfo> handlers = new TreeMap<>();
	//     a map of template names to
	//       a list of 'handlers' for that template, each handler being:
	//         an area type (content or style)
	//         an area name (eg message)
	//         an FLEventType (eg ClickEvent)
	//         -- and I think I want the handler here too ...
	private ListMap<String, TemplateTarget> templates = new ListMap<>(); 
	
	@Override
	public void handler(String eventType, FunctionName handlerName) {
		handlers.put(handlerName.name, new HandlerInfo(eventType, handlerName));
	}

	@Override
	public void binding(String id, TemplateBinding currentBinding, int option, String handler) {
		if (currentBinding == null)
			templates.add(id, new TemplateTarget(null, null, option, handler));
		else
			templates.add(id, new TemplateTarget(currentBinding.assignsTo.text, currentBinding.assignsTo.type().toString().toLowerCase(), option, handler));
	}

	@Override
	public Iterable<String> templateNames() {
		return templates.keySet();
	}

	@Override
	public Iterable<TemplateTarget> targets(String t) {
		return templates.get(t);
	}

	@Override
	public HandlerInfo getHandler(String handler) {
		return handlers.get(handler);
	}

	@Override
	public List<HandlerInfo> unboundHandlers() {
		List<HandlerInfo> ret = new ArrayList<>();
		outer:
		for (Entry<String, HandlerInfo> e : handlers.entrySet()) {
			for (TemplateTarget tt : templates.values()) {
				if (tt.handler.equals(e.getKey()))
					continue outer;
			}
			ret.add(e.getValue());
		}
		return ret;
	}
}
