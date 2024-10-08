package org.flasck.flas.compiler.jsgen;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.NotImplementedException;

public class JSFunctionStateStore implements JSFunctionState {
	public final Map<UnitDataDeclaration, JSExpr> mocks;
	public final Map<IntroduceVar, JSExpr> introductions;
	private final Map<NameOfThing, JSExpr> containers;
	private Map<String, JSExpr> templateObj;
	private final JSMethodCreator meth;
	private JSExpr ocret;
	private JSExpr ocmsgs;
	private boolean cacheResult;
	private final Map<Class<?>, Map<String, Object>> moduleCaches;

	public JSFunctionStateStore(JSMethodCreator meth) {
		this(meth, new TreeMap<>(), new TreeMap<>(IntroduceVar.comparator), new HashMap<>(), new HashMap<>());
	}

	public JSFunctionStateStore(JSMethodCreator meth, Map<UnitDataDeclaration, JSExpr> mocks, Map<IntroduceVar, JSExpr> introductions, Map<NameOfThing, JSExpr> containers, Map<Class<?>, Map<String, Object>> moduleCaches) {
		this.meth = meth;
		this.mocks = mocks;
		this.introductions = introductions;
		this.containers = containers;
		this.moduleCaches = moduleCaches;
	}

	@Override
	public JSMethodCreator meth() {
		return meth;
	}
	
	@Override
	public Map<UnitDataDeclaration, JSExpr> mocks() {
		return mocks;
	}
	
	@Override
	public Map<IntroduceVar, JSExpr> introductions() {
		return introductions;
	}

	@Override
	public Map<NameOfThing, JSExpr> containers() {
		return containers;
	}

	@Override
	public boolean hasContainer(NameOfThing name) {
		return containers.containsKey(name);
	}

	@Override
	public void container(NameOfThing name, JSExpr expr) {
		if (containers.containsKey(name))
			throw new CantHappenException("should not offer multiple definitions for " + name);
		containers.put(name, expr);
	}

	@Override
	public JSExpr container(NameOfThing name) {
		if (!containers.containsKey(name))
			throw new CantHappenException("There is no container for " + (name == null ? "NULL": name.uniqueName()) + " in " + meth.jsName() + "; have " + containers);
		return containers.get(name);
	}
	
	public void provideTemplateObject(Map<String, JSExpr> tc) {
		this.templateObj = tc;
	}

	@Override
	public Map<String, JSExpr> templateObj() {
		return templateObj;
	}

	@Override
	public void objectCtor(JSExpr ocret, JSExpr ocmsgs) {
		this.ocret = ocret;
		this.ocmsgs = ocmsgs;
	}
	
	@Override
	public JSExpr ocret() {
		return ocret;
	}

	@Override
	public JSExpr ocmsgs() {
		return ocmsgs;
	}

	@Override
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo) {
		if (mocks.containsKey(udd))
			throw new NotImplementedException("Duplicate mock " + udd.name.uniqueName());
		mocks.put(udd, resolvesTo);
	}

	@Override
	public void addIntroduction(IntroduceVar var, JSExpr jsv) {
		if (introductions.containsKey(var))
			throw new NotImplementedException("Duplicate introduction " + var.name().uniqueName());
		introductions.put(var, jsv);
	}

	@Override
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else
			throw new RuntimeException("No mock for " + udd);
	}

	@Override
	public JSExpr resolveIntroduction(IntroduceVar var) {
		if (introductions.containsKey(var))
			return introductions.get(var);
		else
			throw new RuntimeException("No introduction for " + var);
	}

	@Override
	public void cacheResult() {
		this.cacheResult = true;
	}

	@Override
	public boolean shouldCacheResult() {
		return cacheResult;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T stateFor(Class<?> clz, String name) {
		Map<String, Object> cache = moduleCaches.get(clz);
		if (cache == null || !cache.containsKey(name)) {
			throw new InvalidUsageException("there is no value for " + clz);
		}
		return (T) cache.get(name);
	}
	
	@Override
	public <T> void cacheFor(Class<?> clz, String name, T o) {
		if (!moduleCaches.containsKey(clz)) {
			moduleCaches.put(clz, new HashMap<>());
		}
		Map<String, Object> cache = moduleCaches.get(clz);
		cache.put(name, o);
	}
	
	@Override
	public Map<Class<?>, Map<String, Object>> moduleCaches() {
		return moduleCaches;
	}
}
