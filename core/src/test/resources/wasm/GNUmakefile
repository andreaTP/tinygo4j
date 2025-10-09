.PHONY: all import usage export callback slices usage main withdep datatypes hello-it types-test
all: import usage export callback slices usage main withdep datatypes hello-it types-test

import:
	./build.sh import unknown

export:
	./build.sh export

callback:
	./build.sh callback command

slices:
	./build.sh slices command

usage:
	./build.sh usage

main:
	./build.sh main command

withdep:
	./build.sh withdep

datatypes:
	./build.sh datatypes

hello-it:
	./build.sh hello-it

 types-test:
	./build.sh types-test

clean:
	rm -rf compiled/*
