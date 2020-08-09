package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	public class Field {
		private final boolean isFinal;
		private final Access access;
		private final NameOfThing type;
		private final String var;

		public Field(boolean isFinal, Access access, NameOfThing type, String var) {
			this.isFinal = isFinal;
			this.access = access;
			this.type = type;
			this.var = var;
		}
	}

	private final JSEnvironment jse;
	private final String name;
	private final JSMethod ctor;
	private NameOfThing baseClass;
	private String javaBase;
	private boolean isInterface;
	private final List<JSMethod> methods = new ArrayList<>();
	private final List<Field> fields = new ArrayList<>();
	private final List<String> intfs = new ArrayList<>();
	
	public JSClass(JSEnvironment jse, String fullName) {
		this.jse = jse;
		this.name = fullName;
		ctor = classMethod(null);
		this.ctor.argument("_cxt");
	}

	public String name() {
		return name;
	}

	@Override
	public void inheritsFrom(NameOfThing baseClass, String javaName) {
		this.baseClass = baseClass;
		this.javaBase = javaName;
		if (this.baseClass != null)
			this.ctor.inheritFrom(baseClass);
	}
	
	@Override
	public void implementsJava(String clz) {
		this.intfs.add(clz);
	}
	
	@Override
	public void justAnInterface() {
		this.isInterface = true;
	}
	
	@Override
	public void field(NameOfThing type, String var) {
		fields.add(new Field(true, Access.PRIVATE, type, var));
	}

	@Override
	public void arg(String a) {
		this.ctor.argument(a);
	}

	@Override
	public JSMethodCreator createMethod(String name, boolean prototype) {
		JSMethod meth = new JSMethod(jse, null, this.name, prototype, name);
		methods.add(meth);
		return meth;
	}
	
	public JSMethod classMethod(String mname) {
		return new JSMethod(jse, null, this.name, false, mname);
	}

	@Override
	public JSMethodCreator constructor() {
		return ctor;
	}

	public void writeTo(IndentWriter iw) {
		ctor.write(iw);
		if (this.baseClass != null) {
			iw.println(name + ".prototype = new " + this.baseClass.jsName() + "();");
			iw.println(name + ".prototype.constructor = " + name + ";");
		}
		for (JSMethod m : methods)
			m.write(iw);
	}

	public void generate(ByteCodeEnvironment bce) {
		if (bce == null)
			return;
		
		ByteCodeCreator bcc = bce.newClass(name);
		if (isInterface) {
			bcc.makeInterface();
		}
		if (javaBase != null)
			bcc.superclass(javaBase);
		else if (baseClass != null)
			bcc.superclass(baseClass.javaName());
		else
			bcc.superclass(J.OBJECT);
		for (String s : intfs)
			bcc.implementsInterface(s);
		bcc.generateAssociatedSourceFile();
		for (Field f : fields) {
			bcc.defineField(f.isFinal, f.access, f.type.javaName(), f.var);
		}
		if (!isInterface)
			ctor.generate(bce, false);
		for (JSMethod m : methods)
			m.generate(bce, isInterface);
	}
	
	@Override
	public String toString() {
		return "JSClass[" + name + "]";
	}
}
