package org.flasck.flas.parsedForm.assembly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.assembly.MainRoutingGroupConsumer;
import org.flasck.flas.parser.assembly.RoutingGroupConsumer;

public class SubRouting implements RoutingGroupConsumer, Locatable {
	protected final ErrorReporter errors;
	protected final MainRoutingGroupConsumer main;
	private final InputPosition pos;
	public final String path;
	private String title;
	public RoutingActions enter;
	public RoutingActions at;
	public RoutingActions exit;
	public boolean requiresSecurity;
	public final List<SubRouting> routes = new ArrayList<>();
	private final Map<VarName, CardBinding> cards = new HashMap<>();
	public final List<CardBinding> assignments = new ArrayList<>();
	private final NameOfThing routeName;

	public SubRouting(ErrorReporter errors, InputPosition pos, String path, RoutingGroupConsumer main, NameOfThing routeName) {
		this.errors = errors;
		this.pos = pos;
		this.path = path;
		this.routeName = routeName;
		if (main == null)
			this.main = (MainRoutingGroupConsumer) this;
		else if (main instanceof MainRoutingGroupConsumer)
			this.main = (MainRoutingGroupConsumer) main;
		else
			this.main = ((SubRouting)main).main;
	}

	public InputPosition location() {
		return pos;
	}

	@Override
	public NameOfThing name() {
		return routeName;
	}

	@Override
	public CardBinding nameCard(UnresolvedVar var, TypeReference cardType) {
		String s = var.var;
		NameOfThing foo = routeName;
		VarName asVar = new VarName(var.location(), foo, s);
		CardBinding ret = new CardBinding(routeName, asVar, cardType);
		if (cards.containsKey(asVar)) {
			errors.message(var.location(), "duplicate card binding of " + s);
			return ret;
		}
		cards.put(asVar, ret);
		return ret;
	}

	public CardBinding getCard(VarName var) {
		return cards.get(var);
	}

	@Override
	public void assignCard(UnresolvedVar var, TypeReference cardType) {
		CardBinding cb = nameCard(var, cardType);
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
	public void at(RoutingActions actions) {
		if (this.at != null) {
			errors.message(actions.location(), "duplicate specification of at");
			return;
		}
		this.at = actions;
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
	public void isSecure() {
		requiresSecurity = true;
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
