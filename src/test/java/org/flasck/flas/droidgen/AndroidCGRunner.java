package org.flasck.flas.droidgen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeFile;
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.utils.FileUtils;

public class AndroidCGRunner extends CGHarnessRunner {
	public AndroidCGRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError, FileNotFoundException {
		super(builder, figureClasses());
	}
	
	private static Class<?>[] figureClasses() throws FileNotFoundException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		{
			ByteCodeCreator counter = new ByteCodeCreator(bce, "test.ziniki.Counter");
			counter.superclass("org.flasck.android.FlasckActivity");
			counter.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
			counter.defineField(false, Access.PROTECTED, JavaType.int_, "counter");
		}
		CGHClassLoaderImpl zcl = new CGHClassLoaderImpl();
		List<Class<?>> ret = new ArrayList<Class<?>>();
		byte[] bs = FileUtils.readAllStream(new FileInputStream("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes/test/ziniki/Counter$B1.class"));
		ByteCodeFile bcf = new ByteCodeFile(new ByteArrayInputStream(bs));
		String name = FileUtils.convertToDottedName(new File(bcf.getName().replaceFirst(".class$", "")));
		expected.put(name, bs);
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.ziniki.Counter$B1");
			bcc.superclass("org.flasck.android.TextArea");
			bcc.addInnerClassReference(Access.PUBLICSTATIC, "test.ziniki.Counter", "B1");
			FieldInfo card = bcc.defineField(true, Access.PRIVATE, new JavaType("test.ziniki.Counter"), "_card");
			{
				GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
				PendingVar counter = gen.argument("test.ziniki.Counter", "counter");
				PendingVar parent = gen.argument("org/flasck/android/Area", "parent");
				NewMethodDefiner ctor = gen.done();
				ctor.callSuper("void", "org/flasck/android/TextArea", "<init>", parent.getVar(), ctor.as(ctor.aNull(), "java.lang.String")).flush();
				ctor.assign(card.asExpr(ctor), counter.getVar()).flush();
				ctor.callVirtual("void", ctor.getField(card.asExpr(ctor), "_wrapper"), "onAssign", ctor.stringConst("counter"), ctor.as(ctor.myThis(), "org.flasck.android.Area"), ctor.stringConst("_contentExpr")).flush();
				ctor.callVirtual("void", ctor.myThis(), "_contentExpr").flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_contentExpr");
				gen.returns("void");
				NewMethodDefiner meth = gen.done();
				Var str = meth.avar("java.lang.String", "str");
				meth.assign(str, meth.callStatic("java.lang.Integer", "java.lang.String", "toString", meth.getField(meth.getField(meth.myThis(), "_card"), "counter"))).flush();
				meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
				meth.returnVoid().flush();
			}
			holder.addEntry(bcc.getCreatedName(), bcc);
		}
		Class<?> testClass = testClass(zcl, bce, "test.ziniki.Counter$B1", false);
		ret.add(testClass);
		return ret.toArray(new Class<?>[ret.size()]);
	}

	@Override
	protected void cleanUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String getName() {
		return "Android Generation Tests";
	}

}
