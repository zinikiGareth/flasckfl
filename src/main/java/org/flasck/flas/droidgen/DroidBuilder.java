package org.flasck.flas.droidgen;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.UtilException;
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
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.build.maven.MavenLibraryCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.config.LibsCommand;

public class DroidBuilder {
	public final File androidDir;
	final ByteCodeEnvironment bce;
	final File qbcdir;
	private String launchCard;
	final List<File> libs = new ArrayList<File>();
	final List<String> maven = new ArrayList<String>();

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
			FileUtils.assertDirectory(new File(androidDir, "src/android/assets/css"));
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
	
	public void setLaunchCard(String launchCard) {
		if (launchCard.contains("/"))
			this.launchCard = launchCard;
		else
			this.launchCard = launchCard.substring(0, launchCard.lastIndexOf(".")) + "/" + launchCard;
	}

	public void useLib(String name) {
		File ul = new File(name);
		if (!ul.canRead())
			throw new UtilException("Cannot read " + ul);
		if (ul.isDirectory()) {
			for (File f : ul.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			})) {
				libs.add(f);
			}
		} else if (name.endsWith("*.jar"))
			libs.add(ul);
		else
			throw new UtilException("Cannot interpret " + ul + " as a directory or jar");
	}

	public void useMaven(String id) {
		maven.add(id);
	}
	
	public void useCSS(String dir) {
		File f = new File(dir);
		if (!f.canRead())
			throw new UtilException("Cannot copy CSS " + dir + " as it does not exist");
		File cssdir = new File(androidDir, "src/android/assets/css");
		if (f.isDirectory()) {
			for (File q : f.listFiles()) {
				if (q.isDirectory())
					FileUtils.copyRecursive(q, cssdir);
				else
					FileUtils.copy(q, new File(cssdir, q.getName()));
			}
		}
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
		/*JavaNature jn = */cf.getNature(config, JavaNature.class);
//		jn.addLib(new File("/Users/gareth/user/Personal/Projects/Android/qb/libs"), new ArrayList<ExcludeCommand>());
		cf.getNature(config, AndroidNature.class);
		QuickBuild.readHomeConfig(config, null);
//		LibsCommand lc = new LibsCommand(new TokenizedLine(0, "libs /Users/gareth/user/Personal/Projects/Android/qb/libs"));
//		config.addChild(lc);
		int setupLineNo = 100;
		int lineNo = 200;
		TokenizedLine toks = new TokenizedLine(lineNo++, "android " + androidDir.getName());
		AndroidCommand cmd = new AndroidCommand(toks);
		for (String mvn : maven) {
			MavenLibraryCommand mlc = new MavenLibraryCommand(new TokenizedLine(setupLineNo++, "maven " + mvn));
			config.addChild(mlc);
			cmd.addChild(new AndroidUseLibraryCommand(new TokenizedLine(lineNo++, "use " + mvn)));
		}
		for (File f : libs) {
			LibsCommand lc = new LibsCommand(new TokenizedLine(setupLineNo++, "libs " + f.getPath()));
			config.addChild(lc);
//			jn.addLib(f, new ArrayList<ExcludeCommand>());
			cmd.addChild(new AndroidUseLibraryCommand(new TokenizedLine(lineNo++, "use " + f.getPath())));
		}
		config.addChild(cmd);
		String apkName = "qbout/" + androidDir.getName() + ".apk";
		AdbInstallCommand install = new AdbInstallCommand(new TokenizedLine(lineNo++, "adbinstall " + androidDir.getName() + " " + apkName));
		config.addChild(install);
		if (launchCard != null) {
			AdbStartCommand start = new AdbStartCommand(new TokenizedLine(lineNo++, "adbstart " + apkName + " " + launchCard));
			config.addChild(start);
		}
		
		config.done();
		cf.done();

		ArrayList<String> sdf = new ArrayList<String>();
//		sdf.add("Manifest");
		BuildContext cxt = new BuildContext(config, cf, outlog, true, true, false, sdf, sdf, false, null, null, false, true, false);
		cxt.configure();

		new BuildExecutor(cxt, false).doBuild();

	}
}
