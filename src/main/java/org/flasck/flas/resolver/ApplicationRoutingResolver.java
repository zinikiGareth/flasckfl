package org.flasck.flas.resolver;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;

public class ApplicationRoutingResolver extends LeafAdapter {
	public class ParameterRepositoryEntry implements RepositoryEntry {
		private final InputPosition pos;
		private final String param;

		public ParameterRepositoryEntry(InputPosition pos, String param) {
			this.pos = pos;
			this.param = param;
		}

		@Override
		public NameOfThing name() {
			return new VarName(pos, e.name(), param);
		}

		@Override
		public InputPosition location() {
			return pos;
		}

		@Override
		public void dumpTo(PrintWriter pw) {
			pw.println("parameter " + param);
		}
	}

	public class Scope {
		private Map<String, RepositoryEntry> defns = new TreeMap<>();
	}

	private final ApplicationRouting e;
	private final List<Scope> scopes = new ArrayList<>();

	public ApplicationRoutingResolver(ApplicationRouting e) {
		this.e = e;
		scopes.add(0, new Scope());
	}

	public void nest() {
		scopes.add(0, new Scope());
	}
	
	public void parameter(InputPosition pos, String param) {
		scopes.get(0).defns.put(param, new ParameterRepositoryEntry(pos, param));
	}
	
	@Override
	public void leaveCardAssignment(CardBinding card) {
		RepositoryEntry defn = (RepositoryEntry) card.cardType.defn();
		if (defn == null) // the card class could not be found, bind to something else
			defn = new ParameterRepositoryEntry(card.location(), card.var.var); // this is obviously wrong, but there will be a type resolution error ...
		scopes.get(0).defns.put(card.var.var, defn);
	}

	public void unnest() {
		scopes.remove(0);
	}

	public RepositoryEntry resolve(RepositoryResolver resolver, UnresolvedVar var) {
		for (Scope s : scopes) {
			if (s.defns.containsKey(var.var))
				return s.defns.get(var.var);
		}
		return null;
	}

}
