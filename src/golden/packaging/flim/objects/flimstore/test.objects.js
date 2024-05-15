var test__objects = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";

test__objects.Accumulator = function(_cxt, _incard) {
  FLObject.call(this, _cxt, _incard);
  this._card = _incard;
  this.state = _cxt.fields();
  return ;
}
test__objects.Accumulator.prototype = new FLObject();
test__objects.Accumulator.prototype.constructor = test__objects.Accumulator;

test__objects.Accumulator.prototype._areYouA = function(_cxt, ty) {
  return ty == 'test.objects.Accumulator';
}

test__objects.Accumulator.prototype._areYouA.nfargs = function() { return 1; }

test__objects.Accumulator.prototype._updateDisplay = function(_cxt) {
  if (_cxt.isTruthy(this._card)) {
    this._card._updateDisplay(_cxt, this._card._renderTree);
  }
  return ;
}

test__objects.Accumulator.prototype._updateDisplay.nfargs = function() { return 0; }

test__objects.Accumulator._ctor_makeMe = function(_cxt, _card) {
  const _v1 = new test__objects.Accumulator(_cxt, _card);
  const _v2 = _cxt.array();
  _v1.state.set('n', 0);
  const _v3 = _cxt.array();
  _cxt.addAll(_v2, _v3);
  const _v4 = new ResponseWithMessages(_cxt, _v1, _v2);
  return _v4;
}

test__objects.Accumulator._ctor_makeMe.nfargs = function() { return 1; }

test__objects.Accumulator.prototype.inc = function(_cxt, _0) {
  _0 = _cxt.head(_0);
  if (_cxt.isA(_0, 'Number')) {
    const k = _0;
    const _v1 = FLBuiltin.plus;
    const _v2 = _cxt.closure(_v1, this.state.get('n'), k);
    const _v3 = Assign.eval(_cxt, this, 'n', _v2);
    const _v4 = _cxt.array(_v3);
    return _v4;
  } else 
    return FLError.eval(_cxt, 'inc: no matching case');
}

test__objects.Accumulator.prototype.inc.nfargs = function() { return 1; }

test__objects.Accumulator.prototype.info = function(_cxt) {
  return this.state.get('x');
}

test__objects.Accumulator.prototype.info.nfargs = function() { return 0; }

test__objects.Accumulator.prototype.value = function(_cxt, _0) {
  const q = _0;
  const _v1 = FLBuiltin.plus;
  const _v2 = _cxt.closure(_v1, this.state.get('n'), q);
  return _v2;
}

test__objects.Accumulator.prototype.value.nfargs = function() { return 1; }

test__objects._init = function(_cxt) {
  const _v1 = _cxt.registerObject('test.objects.Accumulator', test__objects.Accumulator);
  if (_cxt.isTruthy(test__objects._builtin_init)) {
    test__objects._builtin_init(_cxt);
  }
}

test__objects._init.nfargs = function() { return 0; }
test__objects.Accumulator.prototype._methods = function() {
  return {
    "inc": test__objects.Accumulator.prototype.inc
  };
};
test__objects.Accumulator.prototype._eventHandlers = function() {
  return {};
};

export { test__objects };
