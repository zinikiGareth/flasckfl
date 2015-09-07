org = function() {
}

org.ziniki = function() {
}

org.ziniki._Crokey = function(v0) {
  "use strict";
  this._ctor = 'org.ziniki.Crokey';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
    if (v0.key) {
      this.key = v0.key;
    }
  }
  else {
  }
}

org.ziniki.Crokey = function(v0, v1) {
  "use strict";
  return new org.ziniki._Crokey({id: v0, key: v1});
}

org.ziniki._ID = function(v0) {
  "use strict";
  this._ctor = 'org.ziniki.ID';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
  }
  else {
  }
}

org.ziniki.ID = function(v0) {
  "use strict";
  return new org.ziniki._ID({id: v0});
}

org.ziniki;
