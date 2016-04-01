package org.flasck.flas.droidgen;

import java.io.File;
import java.util.ArrayList;

import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.app.BuildOutput;
import com.gmmapowell.quickbuild.app.QuickBuild;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildExecutor;
import com.gmmapowell.quickbuild.build.android.AdbInstallCommand;
import com.gmmapowell.quickbuild.build.android.AdbStartCommand;
import com.gmmapowell.quickbuild.build.android.AndroidCommand;
import com.gmmapowell.quickbuild.build.android.AndroidNature;
import com.gmmapowell.quickbuild.build.android.AndroidUseLibraryCommand;
import com.gmmapowell.quickbuild.build.java.ExcludeCommand;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;

public class DroidBuilder {
	public final File androidDir;
	final ByteCodeEnvironment bce;
	final File qbcdir;

	public DroidBuilder(File androidDir, ByteCodeEnvironment bce) {
		this.androidDir = androidDir;
		this.bce = bce;
		qbcdir = new File(androidDir, "qbout/classes");
	}

	public void init() {
		if (androidDir == null)
			return;
		if (!androidDir.exists()) {
			// create a directory structure to put things in
			FileUtils.assertDirectory(androidDir);
			FileUtils.assertDirectory(new File(androidDir, "src"));
			FileUtils.assertDirectory(new File(androidDir, "src/main"));
			FileUtils.assertDirectory(new File(androidDir, "src/main/java"));
			FileUtils.assertDirectory(new File(androidDir, "src/android"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/assets"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/gen"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/lib"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/res"));
			FileUtils.assertDirectory(new File(androidDir, "src/android/rawapk"));
			FileUtils.assertDirectory(new File(androidDir, "qbout"));
		}
		FileUtils.assertDirectory(qbcdir);
		FileUtils.cleanDirectory(qbcdir);
		// HACK ALERT! This is to pick up the "support library" for FlasckAndroid, which should probably be in a well-known JAR
//		cmd.addToJRR(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes"));
		FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid/qbout/classes", "org"), new File(qbcdir, "org"));
		FileUtils.copyRecursive(new File("/Users/gareth/user/Personal/Projects/Android/HelloAndroid", "src/android/assets"), new File(androidDir, "src/android/assets"));
		FileUtils.copyRecursive(new File("/Users/gareth/Ziniki/Code/Tools/QuickBuild/qbout/classes/", "com/gmmapowell/quickbuild/annotations/android/"), new File(qbcdir, "com/gmmapowell/quickbuild/annotations/android/"));
	}
	
	public void build() {
		// there are a number of possibilities here:
		// just build and deploy "in memory"
		// build from a QB file (by name)
		// defer the building
		// for now, just do the "easy and obvious thing", i.e. build this app
		ConfigFactory cf = new ConfigFactory();
		BuildOutput outlog = new BuildOutput(false);
		Config config = new Config(cf, outlog, androidDir, "xx", null);
		JavaNature jn = cf.getNature(config, JavaNature.class);
		jn.addLib(new File("/Users/gareth/user/Personal/Projects/Android/qb/libs"), new ArrayList<ExcludeCommand>());
		cf.getNature(config, AndroidNature.class);
		QuickBuild.readHomeConfig(config, null);
//		LibsCommand lc = new LibsCommand(new TokenizedLine(0, "libs /Users/gareth/user/Personal/Projects/Android/qb/libs"));
//		config.addChild(lc);
		TokenizedLine toks = new TokenizedLine(1, "android " + androidDir.getName());
		AndroidCommand cmd = new AndroidCommand(toks);
		cmd.addChild(new AndroidUseLibraryCommand(new TokenizedLine(4, "use ZinUtils.jar")));
		config.addChild(cmd);
		AdbInstallCommand install = new AdbInstallCommand(new TokenizedLine(2, "adbinstall " + androidDir.getName() + " qbout/" + androidDir.getName() + ".apk"));
		config.addChild(install);
		AdbStartCommand start = new AdbStartCommand(new TokenizedLine(3, "adbstart AdbInstalled\\[qbout_" + androidDir.getName() + " test.ziniki/test.ziniki.CounterCard"));
		config.addChild(start);
		
		config.done();
		cf.done();

		ArrayList<String> sdf = new ArrayList<String>();
		sdf.add("Manifest");
		BuildContext cxt = new BuildContext(config, cf, outlog, true, true, false, sdf, sdf, false, null, null, false, true, false);
		cxt.configure();

		new BuildExecutor(cxt, false).doBuild();

	}
}
