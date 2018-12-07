if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Item = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Item';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
    if (v0.desc) {
      this.desc = v0.desc;
    }
  }
  else {
  }
}

test.golden.Item = function(v0) {
  "use strict";
  return new test.golden._Item({desc: v0});
}

test.golden._Webzip = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Webzip';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['org.ziniki.CrosetService'] = test.golden.Webzip._C0.apply(this);
  this.cs = this._contracts['org.ziniki.CrosetService'];
  this._contracts['org.flasck.Init'] = test.golden.Webzip._C1.apply(this);
}

test.golden._Webzip.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  this.name = FLEval.full(test.golden._Webzip.prototype.inits_name.apply(this, [msgs]));
  this.list = FLEval.full(test.golden._Webzip.prototype.inits_list.apply(this, [msgs]));
  return msgs.curr;
}

test.golden.Webzip = function(v0) {
  "use strict";
  return new test.golden._Webzip(v0);
}

test.golden.Webzip.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Webzip._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'org.ziniki.CrosetService';
}

test.golden.Webzip._C0 = function() {
  "use strict";
  return new test.golden.Webzip.__C0(this);
}

test.golden.Webzip.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Webzip._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'org.flasck.Init';
}

test.golden.Webzip._C1 = function() {
  "use strict";
  return new test.golden.Webzip.__C1(this);
}

test.golden._Webzip.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Webzip.B1(new CardArea(parent, wrapper, this));
}

test.golden._Webzip.B1 = function(parent) {
  var d = '<div ';
  d += '>\n     hello\n     <div ';
  var content1_id = parent._wrapper.cardId+'_'+(uniqid++);
  d += 'id=\'' + content1_id + '\' ';
  d += '>';
  d += '</div>\n     <div ';
  var listitems_id = parent._wrapper.cardId+'_'+(uniqid++);
  d += 'id=\'' + listitems_id + '\' ';
  d += '>';
  d += '</div>\n  </div>';
  DivArea.call(this, parent, null, null, d);
  if (!parent) return;
  var b2_parent = new DivArea(parent, null, null, null, this._doc.getElementById(content1_id));
  var b2 = new test.golden._Webzip.B2(b2_parent);
  var b6_parent = new DivArea(parent, null, null, null, this._doc.getElementById(listitems_id));
  var b6 = new test.golden._Webzip.B6(b6_parent);
}

test.golden._Webzip.B1.prototype = new DivArea();

test.golden._Webzip.B1.prototype.constructor = test.golden._Webzip.B1;

test.golden._Webzip.B2 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b3 = new test.golden._Webzip.B3(this);
  var b4 = new test.golden._Webzip.B4(this);
  var b5 = new test.golden._Webzip.B5(this);
}

test.golden._Webzip.B2.prototype = new DivArea();

test.golden._Webzip.B2.prototype.constructor = test.golden._Webzip.B2;

test.golden._Webzip.B3 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('Dear ');
}

test.golden._Webzip.B3.prototype = new TextArea();

test.golden._Webzip.B3.prototype.constructor = test.golden._Webzip.B3;

test.golden._Webzip.B4 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('');
}

test.golden._Webzip.B4.prototype = new TextArea();

test.golden._Webzip.B4.prototype.constructor = test.golden._Webzip.B4;

test.golden._Webzip.B5 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'name', test.golden._Webzip.B5.prototype._contentExpr);
  test.golden._Webzip.B5.prototype._contentExpr.call(this);
}

test.golden._Webzip.B5.prototype = new TextArea();

test.golden._Webzip.B5.prototype.constructor = test.golden._Webzip.B5;

test.golden._Webzip.B5.prototype._contentExpr = function() {
  this._assignToText(this.contents_0());
}

test.golden._Webzip.B6 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b7 = new test.golden._Webzip.B7(this);
}

test.golden._Webzip.B6.prototype = new DivArea();

test.golden._Webzip.B6.prototype.constructor = test.golden._Webzip.B6;

test.golden._Webzip.B7 = function(parent) {
  ListArea.call(this, parent);
  if (!parent) return;
  test.golden._Webzip.B7.prototype._assignToVar.call(this);
  this._onAssign(this._card, 'list', test.golden._Webzip.B7.prototype._assignToVar);
}

test.golden._Webzip.B7.prototype = new ListArea();

test.golden._Webzip.B7.prototype.constructor = test.golden._Webzip.B7;

test.golden._Webzip.B7.prototype._newChild = function() {
  return new test.golden._Webzip.B8(this);
}

test.golden._Webzip.B8 = function(parent) {
  var d = '<li ';
  d += '><span ';
  var name_id = parent._wrapper.cardId+'_'+(uniqid++);
  d += 'id=\'' + name_id + '\' ';
  d += '>';
  d += '</span></li>';
  DivArea.call(this, parent, null, null, d);
  if (!parent) return;
  this._src_e = this;
  var b9_parent = new DivArea(parent, null, null, null, this._doc.getElementById(name_id));
  b9_parent._src_e = this._src_e;
  var b9 = new test.golden._Webzip.B9(b9_parent);
}

test.golden._Webzip.B8.prototype = new DivArea();

test.golden._Webzip.B8.prototype.constructor = test.golden._Webzip.B8;

test.golden._Webzip.B8.prototype._assignToVar = function(obj) {
  if (this. e == obj) return;
  if (this.e) {
     this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this.e = obj;
  if (this.e) {
    this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this._fireInterests();
}

test.golden._Webzip.B9 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  this._src_e = parent._src_e;
  var b10 = new test.golden._Webzip.B10(this);
}

test.golden._Webzip.B9.prototype = new DivArea();

test.golden._Webzip.B9.prototype.constructor = test.golden._Webzip.B9;

test.golden._Webzip.B10 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._src_e = parent._src_e;
  this._src_e._interested(this, test.golden._Webzip.B10.prototype._contentExpr);
  this._onAssign(this._src_e.e, 'desc', test.golden._Webzip.B10.prototype._contentExpr);
  test.golden._Webzip.B10.prototype._contentExpr.call(this);
}

test.golden._Webzip.B10.prototype = new TextArea();

test.golden._Webzip.B10.prototype.constructor = test.golden._Webzip.B10;

test.golden._Webzip.B10.prototype._contentExpr = function() {
  this._assignToText(this.contents_2());
}

test.golden._Webzip.B7.prototype._assignToVar = function() {
  var lv = FLEval.full(this.lvs_1());
  ListArea.prototype._assignToVar.call(this, lv);
}

test.golden._Webzip.B10.prototype.contents_2 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._src_e.e, 'desc');
}

test.golden._Webzip.B5.prototype.contents_0 = function() {
  "use strict";
  return this._card.name;
}

test.golden._Webzip.B7.prototype.lvs_1 = function() {
  "use strict";
  return this._card.list;
}

test.golden.Webzip.__C1.prototype.onready = function(v0) {
  "use strict";
  var v1 = FLEval.closure(test.golden.Item, 'do some work');
  var v2 = FLEval.closure(Cons, v1, Nil);
  var v3 = FLEval.closure(Send, this._card.list, 'append', v2, FLEval.idemHandler);
  var v4 = FLEval.closure(test.golden.Item, 'watch rugby');
  var v5 = FLEval.closure(Cons, v4, Nil);
  var v6 = FLEval.closure(Send, this._card.list, 'append', v5, FLEval.idemHandler);
  var v7 = FLEval.closure(Cons, v6, Nil);
  return FLEval.closure(Cons, v3, v7);
}

test.golden._Webzip.prototype.inits_list = function(msgs) {
  "use strict";
  var v0 = FLEval.closure(FLEval.curry, FLEval.octor, 1, Croset, '_ctor_create');
  var ret = FLEval.closure(v0, this.cs);
  return ret;
}

test.golden._Webzip.prototype.inits_name = function(msgs) {
  "use strict";
  return 'Mr. Smith';
}

test.golden;
