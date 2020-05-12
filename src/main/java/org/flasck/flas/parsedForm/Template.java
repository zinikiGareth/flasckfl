package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parser.TemplateBindingConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain;
import org.flasck.flas.tc3.Type;

public class Template implements Locatable, RepositoryEntry, TemplateBindingConsumer {
	public final InputPosition kw;
	private final InputPosition loc;
	public final TemplateReference defines;
	private final List<TemplateBinding> bindings = new ArrayList<TemplateBinding>();
	private final Set<Type> types = new LinkedHashSet<>();
	private final int posn;

	public Template(InputPosition kw, InputPosition loc, TemplateReference defines, int posn) {
		this.kw = kw;
		this.loc = loc;
		this.defines = defines;
		this.posn = posn;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	public TemplateName name() {
		return defines.name;
	}

	public int position() {
		return posn;
	}

	@Override
	public void addBinding(TemplateBinding binding) {
		bindings.add(binding);
	}
	
	public Iterable<TemplateBinding> bindings() {
		return bindings;
	}

	public void canUse(Type ty) {
		types.add(ty);
	}

	public NestingChain nestingChain() {
		if (types.isEmpty())
			return null;
		// Because we are doing this very early (resolution), it's possible there is more than one entry here.
		// I think that will always be an error, but we can leave it in place for the typechecker to pick up
		// Note that we are still building this list as we use it
		
		// The StructDefn here is a reasonable working assumption
		return new TemplateNestingChain((StructDefn) types.iterator().next());
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this);
	}

	@Override
	public String toString() {
		return "Template[" + defines.name.uniqueName() + "]";
	}
}
