package org.flasck.flas.parsedForm.assembly;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;
import org.flasck.flas.parser.assembly.MainRoutingGroupConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ApplicationRouting extends SubRouting implements MainRoutingGroupConsumer, NamedType {
	public class CardBinding implements RepositoryEntry {
		public final UnresolvedVar var;
		public final TypeReference cardType;
		private final VarName myname;

		public CardBinding(UnresolvedVar var, TypeReference cardType) {
			this.var = var;
			this.cardType = cardType;
			this.myname = new VarName(var.location, name, var.var);
		}

		@Override
		public NameOfThing name() {
			return myname;
		}

		@Override
		public InputPosition location() {
			return var.location;
		}

		@Override
		public void dumpTo(PrintWriter pw) {
			pw.println("card binding " + myname.uniqueName());
		}

		public StateHolder type() {
			return (StateHolder) cardType.defn();
		}
	}

	private final ErrorReporter errors;
	private final InputPosition location;
	private final NameOfThing packageName;
	private final NameOfThing name;
	private final ApplicationElementConsumer consumer;
	private final Map<String, CardBinding> cards = new HashMap<>();
	public boolean sawMainCard;
	public RoutingActions enter;
	public RoutingActions exit;

	public ApplicationRouting(ErrorReporter errors, InputPosition location, NameOfThing packageName, AssemblyName name, ApplicationElementConsumer consumer) {
		super(null);
		this.errors = errors;
		this.location = location;
		this.packageName = packageName;
		this.name = name;
		this.consumer = consumer;
	}

	public NameOfThing packageName() {
		return packageName;
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public void assignCard(UnresolvedVar var, TypeReference cardType) {
		main.nameCard(var, cardType);
	}

	@Override
	public void provideMainCard(TypeReference main) {
		if (sawMainCard) {
			errors.message(main.location(), "duplicate assignment to main card");
			return;
		}
		sawMainCard = true;
		consumer.mainCard(main.name());
	}

	@Override
	public void nameCard(UnresolvedVar var, TypeReference cardType) {
		String s = var.var;
		if (cards.containsKey(s)) {
			errors.message(var.location(), "duplicate card binding of " + s);
			return;
		}
		cards.put(s, new CardBinding(var, cardType));
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

	public CardBinding getCard(String var) {
		return cards.get(var);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("routing " + name);
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}

}
