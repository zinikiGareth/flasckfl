package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.mock.IndentWriter;

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
	private NameOfThing baseClass;
	private String javaBase;
	private boolean isInterface;
	private final List<JSMethod> methods = new ArrayList<>();
	private final List<Field> fields = new ArrayList<>();
	private final List<Field> ifields = new ArrayList<>();
	private final List<String> intfs = new ArrayList<>();
	
	public JSClass(JSEnvironment jse, NameOfThing clz) {
		this.jse = jse;
		this.name = clz;
		ctor = classMethod(null);
	}

	public String name() {
		return name.jsName();
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
		fields.add(new Field(isStatic, access, type, var, null));
	}

	@Override
	public void field(boolean isStatic, Access access, NameOfThing type, String var, int value) {
		fields.add(new Field(isStatic, access, type, var, value));
	}

	@Override
	public void inheritsField(boolean isStatic, Access access, NameOfThing type, String var) {
		ifields.add(new Field(isStatic, access, type, var, null));
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
			iw.println(name.jsName() + ".prototype = new " + this.baseClass.jsName() + "();");
			iw.println(name.jsName() + ".prototype.constructor = " + name.jsName() + ";");
		}
		for (JSMethod m : methods)
			m.write(iw);
	}

	public void generate(ByteCodeEnvironment bce) {
		if (bce == null)
			return;
		
		ByteCodeCreator bcc;
		if (name instanceof CSName)
			bcc = bce.newClass(name.javaClassName());
		else
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
	
	@Override
	public String toString() {
		return "JSClass[" + name + "]";
	}
}
