var test__golden = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";
import { test__objects } from "/js/test.objects.js";

test__golden.current = function(_cxt, _0) {
  _0 = _cxt.head(_0);
  if (_cxt.isA(_0, 'test.objects.Accumulator')) {
    const t = _0;
    const _v1 = _cxt.mkacor(test__objects.Accumulator.prototype.value,t,1);
    const _v2 = _cxt.closure(_v1, 0);
    return _v2;
  } else 
    return FLError.eval(_cxt, 'current: no matching case');
}

test__golden.current.nfargs = function() { return 1; }

test__golden._init = function(_cxt) {
  if (_cxt.isTruthy(test__golden._builtin_init)) {
    test__golden._builtin_init(_cxt);
  }
}

test__golden._init.nfargs = function() { return 0; }

export { test__golden };
