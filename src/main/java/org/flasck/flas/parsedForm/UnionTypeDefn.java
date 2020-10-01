package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.UnionFieldConsumer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.UnionFinder;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.NotImplementedException;

public class UnionTypeDefn implements Locatable, UnionFieldConsumer, RepositoryEntry, PolyHolder {
	private final static Logger logger = LoggerFactory.getLogger("TCUnification");
	public final transient boolean generate;
	private final InputPosition location;
	private final SolidName name;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();
	private List<PolyType> polyvars;

	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, PolyType... polyvars) {
		this(location, generate, defining, Arrays.asList(polyvars));
	}
	
	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, List<PolyType> polyvars) {
		this.generate = generate;
		this.location = location;
		this.name = defining;
		this.polyvars = polyvars;
	}
	
	public SolidName name() {
		return name;
	}
	
	@Override
	public boolean hasPolys() {
		return !polyvars.isEmpty();
	}

	public UnionTypeDefn addCase(TypeReference tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return name.uniqueName() + polyvars;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public List<PolyType> polys() {
		return polyvars;
	}
	
	public Type matches(ErrorReporter errors, InputPosition pos, UnionFinder finder, Set<Type> members, boolean needAll) {
		Set<String> all = new HashSet<>();
		Set<String> remaining = new HashSet<>();
		for (TypeReference tr : cases) {
			all.add(tr.defn().name().uniqueName());
			remaining.add(tr.defn().name().uniqueName());
		}
		logger.debug("considering if " + this + " is a match for " + members + " with needAll = " + needAll);
		logger.debug("have cases " + this.cases);
		SetMap<String, Type> polys = new SetMap<String, Type>();
		for (Type t : members) {
			if (t == this) {
				remaining.clear();
				continue;
			}
			NamedType sd;
			if (t instanceof StructDefn || t instanceof UnionTypeDefn) {
				sd = (NamedType) t;
			} else if (t instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance)t;
				sd = pi.struct();
				TypeReference mine;
				if (sd == this) {
					remaining.clear();
					List<TypeReference> trs = new ArrayList<>();
					for (PolyType pt : polyvars)
						trs.add(new TypeReference(pt.location(), pt.shortName()).bind(pt));
					mine = new TypeReference(location, name.baseName(), trs);
					mine.bind(this);
				} else {
					mine = findCase(sd.name().uniqueName());
					if (mine == null) {
						logger.debug("not " + this.signature() + " because there is no case to handle " + sd.name().uniqueName());
						return null;
					}
				}
				List<Type> pip = pi.polys();
				if (!mine.hasPolys() || pip.size() != mine.polys().size())
					throw new NotImplementedException("I can't see how this isn't an error that should have been caught somewhere else");
				for (int i=0;i<pip.size();i++) {
					polys.add(mine.polys().get(i).name(), pip.get(i));
				}
			} else {
				logger.debug("rejecting because something");
				return null;
			}
			if (sd != this && !all.contains(sd.name().uniqueName())) {
				logger.debug("rejecting because something");
				return null;
			}
			remaining.remove(sd.name().uniqueName());
		}
		if (needAll && !remaining.isEmpty()) {
			logger.debug("rejecting because all are needed and we are missing " + remaining);
			return null;
		}
		if (!polys.isEmpty()) {
			List<Type> bound = new ArrayList<>();
			for (PolyType pt : this.polyvars) {
				if (polys.contains(pt.shortName())) {
					Set<Type> tr = polys.get(pt.shortName());
					Type ut; 
					if (tr.size() == 1)
						ut = tr.iterator().next();
					else {
						ut = finder.findUnionWith(errors, pos, tr, true);
						if (ut == null)
							return null;
					}
					bound.add(ut);
				} else
				bound.add(LoadBuiltins.any);
			}
			PolyInstance ret = new PolyInstance(this.location(), this, bound);
			logger.debug("returning " + ret);
			return ret;
		} else {
			logger.debug("returning " + this);
			return this;
		}
	}

	public TypeReference findCase(String ctor) {
		for (TypeReference c : cases)
			if (c.name().equals(ctor))
				return c;
		return null;
	}

	public boolean hasCase(StructDefn sd) {
		for (TypeReference c : cases)
			if (c.defn().equals(sd))
				return true;
		return false;
	}

	@Override
	public String signature() {
		return name.uniqueName();
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("Union[" + toString() + "]");
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		if (this == other)
			return true;
		if (other instanceof UnifiableType) {
			((UnifiableType)other).incorporatedBy(pos, this);
			return true;
		}
		other = removePoly(other);
		for (TypeReference ty : cases)
			if (removePoly(ty.defn()) == other)
				return true;
		// TODO: should we check the poly vars?
		return false;
	}
	
	public Type removePoly(Type other) {
		while (other instanceof PolyInstance) {
			other = ((PolyInstance)other).struct();
		}
		return other;
	}
}
