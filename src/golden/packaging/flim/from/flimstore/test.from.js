var test__from = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";

test__from.Hello = function(_cxt) {
  this.state = _cxt.fields();
  this.state.set('_type', 'test.from.Hello');
  return ;
}


test__from.Hello._typename = 'test.from.Hello'

test__from.Hello.prototype._areYouA = function(_cxt, ty) {
  if (_cxt.isTruthy(ty == 'test.from.Hello')) {
    return true;
  } else if (_cxt.isTruthy(ty == 'test.from.Something')) {
    return true;
  } else 
    return false;
}

test__from.Hello.prototype._areYouA.nfargs = function() { return 1; }

test__from.Hello.eval = function(_cxt, n) {
  const _v1 = new test__from.Hello(_cxt);
  _v1.state.set('n', n);
  _v1.state.set('msg', 'hello');
  return _v1;
}

test__from.Hello.eval.nfargs = function() { return 1; }

test__from.Hello.prototype._field_msg = function(_cxt) {
  return this.state.get('msg');
}

test__from.Hello.prototype._field_msg.nfargs = function() { return 0; }

test__from.Hello.prototype._field_n = function(_cxt) {
  return this.state.get('n');
}

test__from.Hello.prototype._field_n.nfargs = function() { return 0; }

test__from.apply = function(_cxt, _0, _1) {
  const f = _0;
  const x = _1;
  const _v1 = _cxt.closure(f, x);
  return _v1;
}

test__from.apply.nfargs = function() { return 2; }

test__from.f = function(_cxt, _0) {
  _0 = _cxt.head(_0);
  if (_cxt.isA(_0, 'test.from.Hello')) {
    const h = _0;
    const _v1 = _cxt.mkacor(test__from.Hello.prototype._field_n,h,0);
    return _v1;
  } else 
    return FLError.eval(_cxt, 'f: no matching case');
}

test__from.f.nfargs = function() { return 1; }

test__from._init = function(_cxt) {
  const _v1 = _cxt.registerStruct('test.from.Hello', test__from.Hello);
  if (_cxt.isTruthy(test__from._builtin_init)) {
    test__from._builtin_init(_cxt);
  }
}

test__from._init.nfargs = function() { return 0; }

export { test__from };
