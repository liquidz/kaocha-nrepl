.PHONY: repl test install release clean

VERSION := 1.10

repl:
	iced repl with-profile $(VERSION)

test:
	lein test-all

install:
	lein install

clean:
	lein clean
