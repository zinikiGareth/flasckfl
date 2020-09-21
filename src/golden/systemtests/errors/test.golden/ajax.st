	configure
		ajax
		ajax create
		ajax create 42
		ajax create server
		ajax create server "qq qq/33"

		ajax create fred "https://baseuri"
			fred
		
	test we cannot have create in a stage
		ajax create server "http://foo"