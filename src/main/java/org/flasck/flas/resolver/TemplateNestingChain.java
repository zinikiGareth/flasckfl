package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.TemplateNamer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeHelpers;
import org.zinutils.exceptions.HaventConsideredThisException;

public class TemplateNestingChain implements NestingChain {
	public class Link {
		final TypeReference decl;
		final VarName name;
		Type actual;
		private boolean inferred;
		
		public Link(TypeReference decl, VarName nameVar) {
			this.decl = decl;
			this.name = nameVar;
			this.actual = null;
			this.inferred = false;
		}
		
		public Link(Type type, VarName name) {
			this.decl = null;
			this.name = name;
			this.actual = type;
			this.inferred = true;
		}

		public VarName name() {
			return name;
		}
		
		public Type type() {
			return actual;
		}

		public void clean() {
			this.actual = null;
		}
	}

	private final TemplateNamer namer;
	private final List<Link> links = new ArrayList<>();

	public TemplateNestingChain(TemplateNamer namer) {
		this.namer = namer;
	}

	@Override
	public void clean() {
		Iterator<Link> it = links.iterator();
		while (it.hasNext()) {
			Link l = it.next();
			if (l.inferred)
				it.remove();
			else
				l.clean();
		}
	}
	
	@Override
	public boolean isEmpty() {
		return links.isEmpty();
	}
	
	@Override
	public Iterator<Link> iterator() {
		return links.iterator();
	}

	@Override
	public void declare(TypeReference typeReference, VarName nameVar) {
		links.add(new Link(typeReference, nameVar));
	}
	
	@Override
	public List<TypeReference> types() {
		List<TypeReference> ret = new ArrayList<TypeReference>();
		for (Link l : links) {
			if (l.decl != null)
				ret.add(l.decl);
		}
		return ret;
	}

	@Override
	public void addInferred(InputPosition loc, Type ty) {
		if (!links.isEmpty() && links.get(0).decl != null) {
			// it's either duplication or an error
			// duplication is boring and we should catch errors in typecheck
			return;
		} else
			links.add(new Link(ty, namer.nameVar(loc, "_expr" + (links.size()+1))));
	}

	@Override
	public void resolvedTypes(ErrorReporter errors) {
		for (Link l : links) {
			if (l.actual != null)
				continue;
			if (l.decl != null && l.decl.defn() != null)
				l.actual = l.decl.defn();
			else {
				// it may just be that something went wrong
				if (!errors.hasErrors())
					throw new HaventConsideredThisException("type of " + l.name.uniqueName() + " was not resolved");
			}
		}
	}
	
	@Override
	public RepositoryEntry resolve(RepositoryResolver resolver, UnresolvedVar var) {
		for (Link l : links) {
			if (l.actual == null)
				continue;
			else if (l.name != null && l.name.var.equals(var.var))
				return new TemplateNestedField(var.location, l.name, l.actual, null);
			else if (l.actual instanceof StructDefn) {
				StructDefn ty = (StructDefn) l.actual;
				StructField field = ty.findField(var.var);
				if (field != null) {
					resolver.visitTypeReference(field.type, true, -1);
					return new TemplateNestedField(var.location, l.name, field.type(), field);
				}
			} else if (TypeHelpers.isList(l.actual)) {
				Type ty = TypeHelpers.extractListPoly(l.actual);
				if (ty instanceof StructDefn) {
					StructDefn sd = (StructDefn) ty;
					StructField field = sd.findField(var.var);
					if (field != null) {
						resolver.visitTypeReference(field.type, true, -1);
						return new TemplateNestedField(var.location, l.name, field.type(), field);
					}
				}
			}
		}
		return null;
	}

}
