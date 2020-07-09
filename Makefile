.PHONY: repl test coverage install release deploy outdated clean

VERSION := 1.10.1

repl:
	iced repl with-profile $(VERSION)

test:
	lein test-all

coverage:
	lein with-profile +release cloverage

install:
	lein with-profile +release install

release:
	lein with-profile +release release

deploy:
	lein with-profile +release deploy clojars

outdated:
	lein with-profile +antq run -m antq.core

clean:
	lein clean
