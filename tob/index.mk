rel: tob
rel-sdk: tob

.PHONY: tob-book

# LICENSING???
# rel: tob-book
# rel-sdk: tob-book

tob-book:
	rm -rf tob-book
	cd tob ; mdbook build
