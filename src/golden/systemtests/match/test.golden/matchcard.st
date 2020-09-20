Test that we can match card contents using the match directive

	configure
		data MatchCard m
		
	test we can use match
		match m text
			hello

	test check that match can also fail
		match m text
			goodbye