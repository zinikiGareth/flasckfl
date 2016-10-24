if (typeof org === 'undefined') {
  org = function() {
  }
}

if (typeof org.ziniki === 'undefined') {
  org.ziniki = function() {
  }
}

org.ziniki._Id = function(v0) {
  "use strict";
  this._ctor = 'org.ziniki.Id';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
  }
  else {
  }
}

org.ziniki.Id = function(v0) {
  "use strict";
  return new org.ziniki._Id({id: v0});
}

if (typeof org.ziniki.loadCroset_0 === 'undefined') {
  org.ziniki.loadCroset_0 = function() {
  }
}

if (typeof org.ziniki.loadCroset_0.ItemHandler === 'undefined') {
  org.ziniki.loadCroset_0.ItemHandler = function() {
  }
}

org.ziniki.loadCroset_0._ItemHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'org.ziniki.loadCroset_0.ItemHandler';
  this._special = 'handler';
  this._contract = 'org.ziniki.KVUpdate';
  this.set = v0;
  this.ck = v1;
}

org.ziniki.loadCroset_0.ItemHandler = function(v0, v1) {
  "use strict";
  return new org.ziniki.loadCroset_0._ItemHandler(v0, v1);
}

if (typeof org.ziniki.loadCroset_0.SetHandler === 'undefined') {
  org.ziniki.loadCroset_0.SetHandler = function() {
  }
}

org.ziniki.loadCroset_0._SetHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'org.ziniki.loadCroset_0.SetHandler';
  this._special = 'handler';
  this._contract = 'org.ziniki.CrosetHandler';
  this.requestObj = v0;
  this.set = v1;
}

org.ziniki.loadCroset_0.SetHandler = function(v0, v1) {
  "use strict";
  return new org.ziniki.loadCroset_0._SetHandler(v0, v1);
}

if (typeof org.ziniki.scanNatural_0 === 'undefined') {
  org.ziniki.scanNatural_0 = function() {
  }
}

if (typeof org.ziniki.scanNatural_0.ItemHandler === 'undefined') {
  org.ziniki.scanNatural_0.ItemHandler = function() {
  }
}

org.ziniki.scanNatural_0._ItemHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'org.ziniki.scanNatural_0.ItemHandler';
  this._special = 'handler';
  this._contract = 'org.ziniki.KVUpdate';
  this.set = v0;
  this.nk = v1;
}

org.ziniki.scanNatural_0.ItemHandler = function(v0, v1) {
  "use strict";
  return new org.ziniki.scanNatural_0._ItemHandler(v0, v1);
}

if (typeof org.ziniki.scanNatural_0.QHandler === 'undefined') {
  org.ziniki.scanNatural_0.QHandler = function() {
  }
}

org.ziniki.scanNatural_0._QHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'org.ziniki.scanNatural_0.QHandler';
  this._special = 'handler';
  this._contract = 'org.ziniki.QueryHandler';
  this.requestObj = v0;
  this.set = v1;
}

org.ziniki.scanNatural_0.QHandler = function(v0, v1) {
  "use strict";
  return new org.ziniki.scanNatural_0._QHandler(v0, v1);
}

org.ziniki.loadCroset = function(v0, v1, v2, v3, v4, v5) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'org.ziniki.KeyValue')) {
    v0 = FLEval.head(v0);
    if (v0 instanceof FLError) {
      return v0;
    }
    if (FLEval.isA(v0, 'org.ziniki.CrosetContract')) {
      var v10 = FLEval.closure(org.ziniki.loadCroset_0.set);
      var v6 = FLEval.closure(FLEval.curry, org.ziniki.loadCroset_0.ItemHandler, 2, v10);
      var v9 = FLEval.closure(FLEval.curry, org.ziniki.loadCroset_0.requestObj, 4, v6, v1, v2);
      var v7 = FLEval.closure(org.ziniki.loadCroset_0.SetHandler, v9, v10);
      var v8 = FLEval.closure(org.ziniki.loadCroset_0.request, v7, v0, v5, v3, v4);
      var v11 = FLEval.closure(Cons, v8, Nil);
      return FLEval.closure(MessageWrapper, v10, v11);
    }
  }
  return FLEval.error("org.ziniki.loadCroset: case not handled");
}

org.ziniki.loadCroset_0._ItemHandler.prototype.update = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Cons, this.ck, v1);
  var v3 = FLEval.closure(Send, this.set, 'insert', v2);
  return FLEval.closure(Cons, v3, Nil);
}

org.ziniki.loadCroset_0._SetHandler.prototype.remove = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Send, this.set, 'deleteSet', v1);
  return FLEval.closure(Cons, v2, Nil);
}

org.ziniki.loadCroset_0._SetHandler.prototype.update = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Send, this.set, 'mergeAppend', v1);
  var v3 = FLEval.closure(FLEval.field, v0, 'keys');
  var v4 = FLEval.closure(map, this.requestObj, v3);
  var v5 = FLEval.closure(Cons, v4, Nil);
  return FLEval.closure(Cons, v2, v5);
}

org.ziniki.loadCroset_0.request = function(s0, s1, s2, s3, s4) {
  "use strict";
  var v3 = FLEval.closure(s0);
  var v4 = FLEval.closure(Cons, v3, Nil);
  var v5 = FLEval.closure(Cons, s4, v4);
  var v6 = FLEval.closure(Cons, s3, v5);
  var v7 = FLEval.closure(Cons, s2, v6);
  var v8 = FLEval.closure(Send, s1, 'get', v7);
  return FLEval.closure(Cons, v8, Nil);
}

org.ziniki.loadCroset_0.requestObj = function(s0, s1, s2, v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'Crokey')) {
    var v2 = FLEval.closure(FLEval.field, v0, 'id');
    var v3 = FLEval.closure(s0, v0);
    var v4 = FLEval.closure(Cons, v3, Nil);
    var v5 = FLEval.closure(Cons, v2, v4);
    var v6 = FLEval.closure(Cons, s2, v5);
    var v7 = FLEval.closure(Send, s1, 'typed', v6);
    return FLEval.closure(Cons, v7, Nil);
  }
  return FLEval.error("org.ziniki.loadCroset_0.requestObj: case not handled");
}

org.ziniki.loadCroset_0.set = function() {
  "use strict";
  return FLEval.closure(Croset, Nil);
}

org.ziniki.scanNatural = function(v0, v1, v2, v3, v4) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'org.ziniki.KeyValue')) {
    v0 = FLEval.head(v0);
    if (v0 instanceof FLError) {
      return v0;
    }
    if (FLEval.isA(v0, 'org.ziniki.Query')) {
      var v9 = FLEval.closure(org.ziniki.scanNatural_0.set);
      var v5 = FLEval.closure(FLEval.curry, org.ziniki.scanNatural_0.ItemHandler, 2, v9);
      var v8 = FLEval.closure(FLEval.curry, org.ziniki.scanNatural_0.requestObj, 4, v5, v1, v3);
      var v6 = FLEval.closure(org.ziniki.scanNatural_0.QHandler, v8, v9);
      var v7 = FLEval.closure(org.ziniki.scanNatural_0.request, v6, v2, v4, v0, v3);
      var v10 = FLEval.closure(Cons, v7, Nil);
      return FLEval.closure(MessageWrapper, v9, v10);
    }
  }
  return FLEval.error("org.ziniki.scanNatural: case not handled");
}

org.ziniki.scanNatural_0._ItemHandler.prototype.update = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Cons, this.nk, v1);
  var v3 = FLEval.closure(Send, this.set, 'insert', v2);
  return FLEval.closure(Cons, v3, Nil);
}

org.ziniki.scanNatural_0._QHandler.prototype.keys = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Send, this.set, 'mergeAppend', v1);
  var v3 = FLEval.closure(FLEval.field, v0, 'keys');
  var v4 = FLEval.closure(map, this.requestObj, v3);
  var v5 = FLEval.closure(Cons, v4, Nil);
  return FLEval.closure(Cons, v2, v5);
}

org.ziniki.scanNatural_0.request = function(s0, s1, s2, s3, s4) {
  "use strict";
  var v3 = FLEval.closure(s0);
  var v4 = FLEval.closure(Cons, v3, Nil);
  var v5 = FLEval.closure(Cons, s2, v4);
  var v6 = FLEval.closure(Cons, s4, v5);
  var v7 = FLEval.closure(Cons, s1, v6);
  var v8 = FLEval.closure(Send, s3, 'scan', v7);
  return FLEval.closure(Cons, v8, Nil);
}

org.ziniki.scanNatural_0.requestObj = function(s0, s1, s2, v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'NaturalCrokey')) {
    var v2 = FLEval.closure(FLEval.field, v0, 'id');
    var v3 = FLEval.closure(s0, v0);
    var v4 = FLEval.closure(Cons, v3, Nil);
    var v5 = FLEval.closure(Cons, v2, v4);
    var v6 = FLEval.closure(Cons, s2, v5);
    var v7 = FLEval.closure(Send, s1, 'typed', v6);
    return FLEval.closure(Cons, v7, Nil);
  }
  return FLEval.error("org.ziniki.scanNatural_0.requestObj: case not handled");
}

org.ziniki.scanNatural_0.set = function() {
  "use strict";
  return FLEval.closure(Croset, Nil);
}

org.ziniki;
