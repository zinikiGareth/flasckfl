package org.flasck.flas.compiler.jsgen.creators;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.IVFWriter;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.CantHappenException;

public class JSClass implements JSClassCreator {
	public class Field {
		private final boolean isFinal;
		private final Access access;
		private final NameOfThing type;
		private final String var;
		private final Integer value;

		public Field(boolean isFinal, Access access, NameOfThing type, String var, Integer value) {
			this.isFinal = isFinal;
			this.access = access;
			this.type = type;
			this.var = var;
			this.value = value;
		}
	}

	private final JSEnvironment jse;
	private final NameOfThing name;
	private final JSMethod ctor;
	private boolean genJS = true;
	private NameOfThing baseClass;
	private String javaBase;
	private boolean isInterface;
	private final List<JSMethod> methods = new ArrayList<>();
	private final List<Field> fields = new ArrayList<>();
	private final List<Field> ifields = new ArrayList<>();
	private final List<String> intfs = new ArrayList<>();
	private boolean wantTypeNameClzVar;
	
	public JSClass(JSEnvironment jse, NameOfThing clz) {
		this.jse = jse;
		this.name = clz;
		ctor = classMethod(null);
	}

	@Override
	public void wantsTypeName() {
		wantTypeNameClzVar = true;
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	public String clzname() {
		return name.jsName();
	}

	@Override
	public void notJS() {
		this.genJS = false;
	}

	@Override
	public boolean wantJS() {
		return genJS;
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
	public void field(boolean isStatic, Access access, NameOfThing type, String var) {
		if (hasField(var))
			throw new CantHappenException("duplicate field " + var);
		fields.add(new Field(isStatic, access, type, var, null));
	}

	@Override
	public void field(boolean isStatic, Access access, NameOfThing type, String var, int value) {
		if (hasField(var))
			throw new CantHappenException("duplicate field " + var);
		fields.add(new Field(isStatic, access, type, var, value));
	}

	@Override
	public void inheritsField(boolean isStatic, Access access, NameOfThing type, String var) {
		if (hasField(var))
			throw new CantHappenException("duplicate field " + var);
		ifields.add(new Field(isStatic, access, type, var, null));
	}

	@Override
	public boolean hasField(String var) {
		for (Field f : fields) {
			if (f.var.equals(var))
				return true;
		}
		for (Field f : ifields) {
			if (f.var.equals(var))
				return true;
		}
		return false;
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
		if (!genJS)
			return;
		Set<NameOfThing> names = new HashSet<>();
		names.add(name);
		if (name.container() instanceof FunctionName)
			JSMethod.ensureContainingNames(iw, name.container(), names);
		ctor.write(iw, names);
		if (this.baseClass != null) {
			iw.println(name.jsName() + ".prototype = new " + this.baseClass.jsName() + "();");
			iw.println(name.jsName() + ".prototype.constructor = " + name.jsName() + ";");
		}
		if (wantTypeNameClzVar) {
			iw.println("\n");
			iw.println(name.jsName() + "._typename = '" + name.uniqueName() + "'");
		}
		for (JSMethod m : methods)
			m.write(iw, names);
	}

	public void generate(ByteCodeEnvironment bce) {
		if (bce == null)
			return;
		
		ByteCodeCreator bcc;
		bcc = bce.newClass(name.javaName());
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
			FieldInfo fi = bcc.defineField(f.isFinal, f.access, f.type.javaName(), f.var);
			if (f.value != null)
				fi.constValue(f.value);
		}
		for (Field f : ifields) {
			bcc.inheritsField(f.isFinal, f.access, f.type.javaName(), f.var);
		}
		if (!isInterface)
			ctor.generate(bce, false);
		for (JSMethod m : methods)
			m.generate(bce, isInterface);
	}
	
	public String asivm() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		IVFWriter iw = new IVFWriter(pw);
		asivm(iw);
		return sw.toString();
	}

	private void asivm(IVFWriter iw) {
		iw.println("class " + this.name.uniqueName());
		if (ctor != null)
			ctor.asivm(iw.indent());
		for (JSMethod m : this.methods)
			m.asivm(iw.indent());
	}

	@Override
	public String toString() {
		return "JSClass[" + name + "]";
	}
}
