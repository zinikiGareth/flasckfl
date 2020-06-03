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
import org.flasck.flas.parser.UnionFieldConsumer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.UnionFinder;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.NotImplementedException;

public class UnionTypeDefn implements Locatable, UnionFieldConsumer, RepositoryEntry, PolyHolder {
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
	
	public Type matches(Set<Type> members, UnionFinder finder) {
		Set<String> all = new HashSet<>();
		Set<String> left = new HashSet<>();
		for (TypeReference tr : cases) {
			all.add(tr.name());
			left.add(tr.name());
		}
		SetMap<String, Type> polys = new SetMap<String, Type>();
		for (Type t : members) {
			if (t == this) {
				left.clear();
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
					left.clear();
					List<TypeReference> trs = new ArrayList<>();
					for (PolyType pt : polyvars)
						trs.add(new TypeReference(pt.location(), pt.shortName()).bind(pt));
					mine = new TypeReference(location, name.baseName(), trs);
					mine.bind(this);
				} else {
					mine = findCase(sd.name().uniqueName());
					if (mine == null)
						return null;
				}
				List<Type> pip = pi.getPolys();
				if (!mine.hasPolys() || pip.size() != mine.polys().size())
					throw new NotImplementedException("I can't see how this isn't an error that should have been caught somewhere else");
				for (int i=0;i<pip.size();i++) {
					polys.add(mine.polys().get(i).name(), pip.get(i));
				}
			} else
				return null;
			if (sd != this && !all.contains(sd.name().uniqueName()))
				return null;
			left.remove(sd.name().uniqueName());
		}
		if (!left.isEmpty())
			return null;
		if (!polys.isEmpty()) {
			List<Type> bound = new ArrayList<>();
			for (PolyType pt : this.polyvars) {
				if (polys.contains(pt.shortName())) {
					Set<Type> tr = polys.get(pt.shortName());
					Type ut; 
					if (tr.size() == 1)
						ut = tr.iterator().next();
					else {
						ut = finder.findUnionWith(tr);
						if (ut == null)
							return null;
					}
					bound.add(ut);
				} else
				bound.add(LoadBuiltins.any);
			}
			return new PolyInstance(this.location(), this, bound);
		} else
			return this;
		/*
		// TODO: we need to do deeper analysis to make sure poly vars are consistently instantiated
		// TODO: this prep work is the same for each of the union types to be considered - should we do it before calling this?
		Set<StructDefn> structs = new HashSet<StructDefn>();
		for (Type t : members) {
			if (t instanceof StructDefn)
				structs.add((StructDefn) t);
			else if (t instanceof PolyInstance)
				structs.add(((PolyInstance)t).struct());
			else
				return false;
		}
		if (cases.size() != structs.size())
			return false;
		for (TypeReference s : cases) {
			if (!structs.contains((Type)s.defn()))
				return false;
		}
		return true;
		*/
	}

	public TypeReference findCase(String ctor) {
		for (TypeReference c : cases)
			if (c.name().equals(ctor))
				return c;
		return null;
	}

	@Override
	public String signature() {
		return name.uniqueName();
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
