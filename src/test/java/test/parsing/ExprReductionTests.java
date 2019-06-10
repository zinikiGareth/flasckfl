package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ExprTermConsumer;
import org.flasck.flas.parser.Punctuator;
import org.flasck.flas.parser.TDAStackReducer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExprReductionTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ExprTermConsumer builder = context.mock(ExprTermConsumer.class);
	private final InputPosition pos = new InputPosition("-", 1, 0, "");
	private final TDAStackReducer reducer = new TDAStackReducer(errors, builder);

	@Test // 42
	public void aLiteralByItselfIsNotFurtherReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "42", -1));
		reducer.done();
	}

	@Test // f 42
	public void aFunctionCallCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.number(42)).location("-", 1, 0, 4)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new NumericLiteral(pos.copySetEnd(4), "42", -1));
		reducer.done();
	}

	@Test // -42
	public void aUnaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(42)).location("-", 1, 0, 3)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(3), "42", -1));
		reducer.done();
	}

	@Test // 2+4
	public void aBinaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.number(4)).location("-", 1, 0, 8)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "4", -1));
		reducer.done();
	}

	@Test // 4-2
	public void binaryMinusBeReducedToo() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(4), ExprMatcher.number(2)).location("-", 1, 0, 7)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "4", -1));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(7), "2", -1));
		reducer.done();
	}

	@Test // 2 + f x
	public void functionToRightOfOperatorBindsTightly() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "x"));
		reducer.done();
	}

	@Test // f x + 2
	public void functionToLeftOfOperatorBindsTightly() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x")), ExprMatcher.number(2)).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(6), "x"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "2", -1));
		reducer.done();
	}

	@Test // s . a
	public void canGetAField() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("s"), ExprMatcher.unresolved("a")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "s"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "a"));
		reducer.done();
	}

	@Test // ds . f x => (ds.f) x
	public void dotOperatorMakesNewFunctionToCall() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("ds"), ExprMatcher.unresolved("f")), ExprMatcher.unresolved("x")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "ds"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(6), "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "x"));
		reducer.done();
	}

	@Test // f s . x => f (s.x)
	public void dotOperatorHasPriorityInAnArgList() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("s"), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos, "s"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "x"));
		reducer.done();
	}

	@Test // s . m . a => (s.m).a
	public void dotAssociatesLeft() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("s"), ExprMatcher.unresolved("m")), ExprMatcher.unresolved("a")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "s"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "m"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "a"));
		reducer.done();
	}

	@Test // (f x) . a
	public void parensOverrideDotAssociativity() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x")), ExprMatcher.unresolved("a")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(3), "x"));
		reducer.term(new Punctuator(pos.copySetEnd(4), ")"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "a"));
		reducer.done();
	}

	@Test // s . => error
	public void cantLeaveADotHanging() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos.copySetEnd(6)), with("field access requires an explicit field name"));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "s"));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(6), "."));
		reducer.done();
	}

	@Test // s . 3 => error
	public void cantUseAConstantForTheFieldName() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos), with("field access requires a field name"));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "s"));
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new NumericLiteral(pos, "42", 6));
		reducer.done();
	}

	@Test // . a => error
	public void cantStartWithADot() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos), with("field access requires a struct or object"));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedOperator(pos, "."));
		reducer.term(new UnresolvedVar(pos.copySetEnd(6), "a"));
		reducer.done();
	}

	@Test // . => error
	public void cantHaveADotByItself() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos.copySetEnd(6)), with("field access requires a struct or object"));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedOperator(pos.copySetEnd(6), "."));
		reducer.done();
	}

	@Test // 2 * -3
	public void unaryOperatorBindsMoreTightlyThanMultiply() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(3))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "3", -1));
		reducer.done();
	}

	@Test // 2 * (-3)
	public void unaryOperatorCanBePlacedInParens() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(3))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "3", -1));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // 2 + 3 + 4
	public void plusAssociatesToTheLeft() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // 2 + 3 * 4
	public void multiplyBindsBeforePlus() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // 2 * 3 * 4
	public void multiplyBindsBeforePlusFromTheLeftAsWell() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.number(3)), ExprMatcher.number(4)).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos.copySetEnd(9), "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // Nil
	public void typeNamesAreRecognizedAsConstructors() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("Nil")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "Nil"));
		reducer.done();
	}

	@Test // Cons a b
	public void constructorsAreTreatedLikeFunctions() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("Cons"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "Cons"));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "b"));
		reducer.done();
	}

	@Test // type Nil
	public void typeOperatorReturnsAConstructorConstant() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.typeof("Nil").location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "type"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "Nil"));
		reducer.done();
	}

	@Test // type Nil Nil
	public void typeOperatorCanOnlyHaveOneArgument() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos), with("type operator must have exactly one argument"));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "type"));
		reducer.term(new UnresolvedVar(pos, "Nil"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "Nil"));
		reducer.done();
	}

	@Test // 2 * (3+4)
	public void parensCanMakePlusStrong() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "4", -1));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // a + 2*3 + b
	public void multiplyOverPlusBothWays() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.unresolved("a"), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.number(3)), ExprMatcher.unresolved("b"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "b"));
		reducer.done();
	}

	@Test // (a + 2)*(3 + b)
	public void parensCanOvercomeMultiply() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.unresolved("a"), ExprMatcher.number(2)), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.unresolved("b"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new Punctuator(pos.copySetEnd(8), ")"));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}
	
	@Test // 2*(3+(4-2))
	public void parensCanBeNested() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(4), ExprMatcher.number(2)))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "4", -1));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "2", -1));
		reducer.term(new Punctuator(pos.copySetEnd(10), ")"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // f (2*x)
	public void parensAreStrongerThanFnCall() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "x"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // 2*(f x)
	public void parensCanWrapAFnCall() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "x"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // (a,b)
	public void parensCanBeUsedToCreateTuples() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("()"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // (a,b,2*c)
	public void commasActAsCloseParens() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("()"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("c"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "("));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(7), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // []
	public void itIsPossibleToHaveAnEmptyList() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "["));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // ([])
	public void anEmptyListMayBeWrappedInParens() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new Punctuator(pos, "["));
		reducer.term(new Punctuator(pos.copySetEnd(8), "]"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // [a,b]
	public void squaresCanBeUsedToCreateLists() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "["));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // [a*b,b,2*c]
	public void commasForceElementReductionInLists() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")), ExprMatcher.unresolved("b"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("c"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(3), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(5), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(7), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // [a,[b,c],d)
	public void listsCanBeNested() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"),
										ExprMatcher.unresolved("a"),
										ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.unresolved("b"), ExprMatcher.unresolved("c")),
										ExprMatcher.unresolved("d")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new Punctuator(pos.copySetEnd(3), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(5), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(5), "]"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "d"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // {}
	public void emptyHashCase() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("{}")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "{"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "}"));
		reducer.done();
	}

	@Test // {a:b}
	public void simpleOneEntryHash() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("{}"),
										ExprMatcher.apply(ExprMatcher.operator(":"),
											ExprMatcher.string("a"),
											ExprMatcher.unresolved("b"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "{"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "}"));
		reducer.done();
	}

	// TODO?  Somebody needs to complain if we assign the same variable twice
	@Test // {a:b,c:d*2}
	public void hashWithTwoElements() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("{}"),
										ExprMatcher.apply(ExprMatcher.operator(":"),
											ExprMatcher.string("a"),
											ExprMatcher.unresolved("b")),
										ExprMatcher.apply(ExprMatcher.operator(":"),
												ExprMatcher.string("c"),
												ExprMatcher.apply(ExprMatcher.operator("*"),
													ExprMatcher.unresolved("d"),
													ExprMatcher.number(2)))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "{"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "c"));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "d"));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(7), "*"));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new Punctuator(pos.copySetEnd(12), "}"));
		reducer.done();
	}

	// Some complex cases that came up in regression testing
	
	@Test // { tgh : { } , tduhnh : - - vwsoskr 816 }
	public void aComplexHash() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("{}"),
										ExprMatcher.apply(ExprMatcher.operator(":"),
											ExprMatcher.string("tgh"),
											ExprMatcher.apply(ExprMatcher.operator("{}"))),
										ExprMatcher.apply(ExprMatcher.operator(":"),
											ExprMatcher.string("tduhnh"),
											ExprMatcher.apply(ExprMatcher.operator("-"),
												ExprMatcher.apply(ExprMatcher.operator("-"),
													ExprMatcher.apply(ExprMatcher.unresolved("vwsoskr"), ExprMatcher.number(816))
											
											))))));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "{"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "tgh"));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new Punctuator(pos, "{"));
		reducer.term(new Punctuator(pos.copySetEnd(4), "}"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(6), "tduhnh"));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "vwsoskr"));
		reducer.term(new NumericLiteral(pos.copySetEnd(10), "816", 12));
		reducer.term(new Punctuator(pos.copySetEnd(12), "}"));
		reducer.done();
	}
	
	@Test // { tgh : { } , tduhnh : - - vwsoskr 816 }
	public void aComplexExpr() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(
										ExprMatcher.apply(ExprMatcher.operator(":"),
											ExprMatcher.apply(ExprMatcher.operator("+"),
												ExprMatcher.unresolved("true"),
												ExprMatcher.number(516)),
											ExprMatcher.apply(ExprMatcher.operator("-"),
												ExprMatcher.apply(ExprMatcher.operator("[]")),
												ExprMatcher.number(472))
											)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "true"));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(2), "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(10), "516", 12));
		reducer.term(new Punctuator(pos, ":"));
		reducer.term(new Punctuator(pos, "["));
		reducer.term(new Punctuator(pos.copySetEnd(4), "]"));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(10), "472", 12));
		reducer.done();
	}
	
	
	// do we have anything that associates right?

	// some error cases
	@Test // )
	public void parensCannotBeClosedIfTheyWereNeverOpened() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "invalid tokens after expression");
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, ")"));
		reducer.done();
	}

	@Test // ()
	public void itIsNotPossibleToHaveAnEmptyTuple() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "empty tuples are not permitted");
			oneOf(errors).message(pos, "syntax error");
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}
}
