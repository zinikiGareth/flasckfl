package org.flasck.flas.parsedForm.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.parser.assembly.MainRoutingGroupConsumer;
import org.flasck.flas.parser.assembly.RoutingGroupConsumer;

public class SubRouting implements RoutingGroupConsumer {
	protected final ErrorReporter errors;
	protected final MainRoutingGroupConsumer main;
	public final String path;
	private String title;
	public RoutingActions enter;
	public RoutingActions exit;
	public final List<SubRouting> routes = new ArrayList<>();
	public final List<CardBinding> assignments = new ArrayList<>();

	public SubRouting(ErrorReporter errors, String path, RoutingGroupConsumer main) {
		this.errors = errors;
		this.path = path;
		if (main == null)
			this.main = (MainRoutingGroupConsumer) this;
		else if (main instanceof MainRoutingGroupConsumer)
			this.main = (MainRoutingGroupConsumer) main;
		else
			this.main = ((SubRouting)main).main;
	}

	@Override
	public void assignCard(UnresolvedVar var, TypeReference cardType) {
		CardBinding cb = main.nameCard(var, cardType);
		assignments.add(cb);
	}

	@Override
	public void title(InputPosition pos, String s) {
		if (this.title != null) {
			errors.message(pos, "cannot set title more than once");
			return;
		}
		this.title = s;
	}

	@Override
	public void enter(RoutingActions actions) {
		if (this.enter != null) {
			errors.message(actions.location(), "duplicate specification of enter");
			return;
		}
		this.enter = actions;
	}

	@Override
	public void exit(RoutingActions actions) {
		if (this.exit != null) {
			errors.message(actions.location(), "duplicate specification of exit");
			return;
		}
		this.exit = actions;
	}

	@Override
	public void route(RoutingGroupConsumer group) {
		routes.add((SubRouting)group);
	}

	public boolean hasTitle() {
		return title != null;
	}

	public String getTitle() {
		return title;
	}
}
