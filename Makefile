.PHONY: repl test install release clean

VERSION := 1.10.1

repl:
	iced repl with-profile $(VERSION)

test:
	lein test-all

coverage:
	lein with-profile +release cloverage

install:
	lein with-profile +release install

clean:
	lein clean
