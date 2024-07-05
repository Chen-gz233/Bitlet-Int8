run:
	sbt "runMain gemmini.gemminiPE"

test:
	sbt "testOnly gemmini.Int8Test"

test_1024:
	sbt "testOnly gemmini.Int8Test_1024"
clean:
	rm -rf generated/*
	rm -rf test_run_dir/*
	rm -rf project/*
	rm -rf target/*
