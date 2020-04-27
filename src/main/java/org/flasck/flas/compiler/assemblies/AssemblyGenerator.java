package org.flasck.flas.compiler.assemblies;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.repository.AssemblyVisitor;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.fl.ClientContext;
import org.ziniki.deployment.fl.JVMApplicationAssembly;
import org.ziniki.deployment.fl.JVMPackageInfo;

public class AssemblyGenerator implements AssemblyVisitor {
	private final FLEvalContext cx;

	public AssemblyGenerator(AssemblyVisitor storer) {
		if (storer != null)
			cx = storer.getCreationContext();
		else
			cx = new ClientContext();
	}

	@Override
	public FLEvalContext getCreationContext() {
		return cx;
	}

	@Override
	public void visitAssembly(Assembly a) {
		String name = a.name().uniqueName();
		
		JVMApplicationAssembly store = new JVMApplicationAssembly(cx);
		a.storeAs(store);
		store.set("title", "hello, world");
		ArrayList<Object> packages = new ArrayList<>();
		store.set("packages", packages);
		
		JVMPackageInfo pi = new JVMPackageInfo(cx);
		List<String> jslibs = new ArrayList<>();
		jslibs.add(name);
		jslibs.add("dummy test");
		pi.set("javascript", jslibs);
		packages.add(pi);
	}

	@Override
	public void traversalDone() {
		// TODO Auto-generated method stub

	}

}
