var test__golden = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";
import { test__from } from "/js/test.from.js";

test__golden.g = function(_cxt, _0, _1) {
  const x = _0;
  const y = _1;
  const _v1 = FLBuiltin.mul;
  const _v2 = test__from.f;
  const _v3 = _cxt.closure(_v2, x);
  const _v4 = _cxt.closure(_v1, _v3, y);
  return _v4;
}

test__golden.g.nfargs = function() { return 2; }

test__golden.newHello = function(_cxt) {
  const _v1 = _cxt.getSingleton('test.golden.newHello');
  if (_v1) return _v1;
  const _v2 = test__from.Hello.eval(_cxt, 42);
  _cxt.cacheSingleton('test.golden.newHello', _v2);
  return _v2;
}

test__golden.newHello.nfargs = function() { return 0; }

test__golden._init = function(_cxt) {
  if (_cxt.isTruthy(test__golden._builtin_init)) {
    test__golden._builtin_init(_cxt);
  }
}

test__golden._init.nfargs = function() { return 0; }

export { test__golden };
