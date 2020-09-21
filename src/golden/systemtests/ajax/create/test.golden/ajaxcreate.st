	configure
		ajax create aj0 "http://wherever"
			subscribe "/resource"
				responses // no more text allowed, but comments are
					{ hello: "world" }
					{ "count": 42 }