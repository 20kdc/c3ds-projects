# RAL Manual: Language Reference

## Language Structure

A RAL file is made up of *declarations*.

Declarations in turn contain expressions and statements (statements containing more expressions and more statements).

In particular (something about macros).

## Technical Details

### Lexer

The RAL lexer has 5 distinct token categories:

+ ID

+ Keyword

+ String

+ Float

+ Integer

However, in practice, there are only 4:

+ ID/Keyword

+ Operator (considered a keyword)

+ String

+ Float/Integer




