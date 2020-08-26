package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.HandlerImplements.AsNested;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class HandlerImplements extends Implements implements RepositoryEntry, NamedType, WithTypeSignature, StateHolder {
	public class AsNested implements NestedVarReader {
		@Override
		public int size() {
			int ret = 0;
			for (HandlerLambda hl : boundVars)
				if (hl.isNested)
					ret++;
			return ret;
		}

		@Override
		public Collection<HSIOptions> all() {
			throw new NotImplementedException();
		}

		@Override
		public List<UnresolvedVar> vars() {
			List<UnresolvedVar> ret = new ArrayList<>();
			for (HandlerLambda hl : boundVars)
				if (hl.isNested) {
					TypedPattern tp = (TypedPattern)hl.patt;
					UnresolvedVar uv = new UnresolvedVar(hl.location(), tp.var.var);
					uv.bind(tp);
					ret.add(uv);
				}
			return ret;
		}

		@Override
		public List<Pattern> patterns() {
			throw new NotImplementedException();
		}

		@Override
		public boolean containsReferencesNotIn(Set<LogicHolder> processedFns) {
			throw new NotImplementedException();
		}

		@Override
		public Set<LogicHolder> references() {
			throw new NotImplementedException();
		}

		@Override
		public Set<HandlerImplements> referencesHI() {
			throw new NotImplementedException();
		}

		@Override
		public boolean enhanceWith(LogicHolder fn, NestedVarReader nestedVars) {
			throw new NotImplementedException();
		}

		@Override
		public boolean dependsOn(LogicHolder f) {
			throw new NotImplementedException();
		}

		@Override
		public void clearPatterns() {
			throw new NotImplementedException();
		}
	}

	public final String baseName;
	public final List<HandlerLambda> boundVars;
	public final boolean inCard;
	public final InputPosition typeLocation;
	public final HandlerName handlerName;

	public HandlerImplements(InputPosition kw, InputPosition location, InputPosition typeLocation, NamedType parent, HandlerName handlerName, TypeReference implementing, boolean inCard, List<HandlerLambda> lambdas) {
		super(kw, location, parent, implementing, handlerName);
		this.typeLocation = typeLocation;
		this.handlerName = handlerName;
		this.baseName = handlerName.baseName;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}

	@Override
	public String signature() {
		return handlerName.uniqueName();
	}

	@Override
	public int argCount() {
		return boundVars.size();
	}

	@Override
	public Type get(int pos) {
		if (pos == boundVars.size())
			return this;
		HandlerLambda p = boundVars.get(pos);
		if (p.patt instanceof TypedPattern)
			return ((TypedPattern)p.patt).type();
		else
			throw new NotImplementedException("Pattern not handled: " + p.getClass());
	}

	public ObjectMethod getMethod(String called) {
		for (ObjectMethod m : super.implementationMethods)
			if (m.name().name.equals(called))
				return m;
		return null;
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return this == other;
	}
	
	@Override
	public Type type() {
		return this;
	}

	@Override
	public StateDefinition state() {
		if (parent instanceof StateHolder)
			return ((StateHolder)parent).state();
		else
			throw new NotImplementedException();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	public NestedVarReader nestedVars() {
		if (boundVars != null && !boundVars.isEmpty() && boundVars.get(0).isNested) {
			return new AsNested();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "HandlerImplements[" + handlerName.uniqueName() + "]";
	}
}
