package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.StateHolder;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.ShouldBeError;

public class Apply implements Type, SignatureNeedsParensType {
	public final static InputPosition unknown = new InputPosition("unknown", 1, 0, "unknown");
	public final List<Type> tys;

	public Apply(Type... types) {
		if (types.length < 2)
			throw new RuntimeException("Must have at least one input and one output");
		tys = new ArrayList<>();
		for (Type t : types) {
			if (t == null)
				throw new CantHappenException("cannot have null type");
			tys.add(t);
		}
	}

	public Apply(List<Type> types) {
		if (types.size() < 2)
			throw new RuntimeException("Must have at least one input and one output");
		for (Type t : types) {
			if (t == null)
				throw new CantHappenException("cannot have null type");
		}
		this.tys = types;
	}

	public Apply(List<Type> argTypes, Type result) {
		if (argTypes.isEmpty())
			throw new RuntimeException("Must have at least one input and one output");
		tys = new ArrayList<>();
		tys.addAll(argTypes);
		tys.add(result);
		for (Type t : tys) {
			if (t == null)
				throw new CantHappenException("cannot have null type");
		}
	}

	@Override
	public String signature() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (int i=0;i<tys.size();i++) {
			Type t = tys.get(i);
			sb.append(sep);
			sep = "->";
			if (t == null)
				sb.append("<<UNDEFINED>>");
			else {
				boolean needParens = t instanceof SignatureNeedsParensType && i < tys.size()-1;
				if (needParens)
					sb.append("(");
				sb.append(t.signature());
				if (needParens)
					sb.append(")");
			}
		}
		return sb.toString();
	}

	public Type appliedTo(StateHolder ty) {
		if (ty != tys.get(0))
			throw new ShouldBeError("Applying an Apply to the wrong type");
		if (tys.size() == 2)
			return tys.get(1);
		else {
			List<Type> copy = new ArrayList<>(tys);
			copy.remove(0);
			return new Apply(copy);
		}
	}

	public Type discard(int discard) {
		if (tys.size() < discard+1)
			throw new ShouldBeError("Only have " + tys.size() + "; cannot discard " + discard);
		if (tys.size() == discard+1)
			return tys.get(discard);
		else {
			List<Type> copy = new ArrayList<>(tys);
			while (discard-- > 0)
				copy.remove(0);
			return new Apply(copy);
		}
	}

	@Override
	public int argCount() {
		return tys.size()-1;
	}

	@Override
	public Type get(int pos) {
		return tys.get(pos);
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		// TODO: there are incorrect assertions here ...
		//  because UTs are infintely subtle (other may be a UT)
		//  because of currying
		if (other instanceof UnifiableType)
			return other.incorporates(pos, this);
		if (!(other instanceof Apply))
			return false;
		List<Type> otys = ((Apply)other).tys;
		if (tys.size() != otys.size())
			return false;
		for (int i=0;i<tys.size();i++) {
			Type fi = this.tys.get(i);
			Type oi = otys.get(i);
			if (oi instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType) oi;
				ut.incorporatedBy(unknown, fi);
			} else if (fi instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType) fi;
				ut.isPassed(unknown, oi);
			} else if (!fi.incorporates(unknown, oi)) {
				return false;
			}			
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Type t : tys) {
			sb.append("-->");
			if (t == null)
				sb.append("<<UNDEFINED>>");
			else
				sb.append("(" + t.toString() + ")");
		}
		return sb.toString();
	}
}
