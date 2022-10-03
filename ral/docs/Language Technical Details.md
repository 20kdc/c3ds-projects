# Technical Details

## Expressions

Expressions are represented as slices (`RALExprSlice`). Slices are made up of slots.

Slots may be subdivided, but if you do this then parts of the expression may be outright dropped, *including side-effects.*

This is necessary so that things like in

*The answer in regards to how this interacts with macros, statement expressions and side-effects is that if you try to push it, you can probably find cases where a void macro will be sliced off, or an expression has to be executed twice to fulfill a particularly hare-brained request.*

*The following are <u>compiler guarantees</u>, so if the compiler violates these, it's a bug:*

1. An assignment statement will *always* compile a read of the *root of the right-hand-side* expression.

2. Deliberately discarding a value with `_` will generate an uninitialized return value temporary specific to that slot (i.e. `_,_ = 1,2;` creates two temporaries), and will compile the expression (and therefore it's side-effects).

3. Corollary to guarantee 1: The same applies to variables created deliberately with `let`. The same *does not* apply to `alias` or inlined parameters to a `` as that's not how they work.

4. An expression statement such as `1;` or `doTheThing();` is exactly equal to an assignment statement with however many discard values are required on the left side. (Internally, both assignments and expression statements are considered `RALAssignStatement` so check there fore details.)
   Therefore, guarantee 1 is in effect for expression statements.

5. An assignment statement will always compile the root expression, and a macro call (which for `someCall();` is the root expression) will always compile it's own root expression in turn.

Note, however, no guarantees are made about when the compiler *will not* compile an expression's side-effects - this is because macros with no return values and empty (and thus elidable) expression slices look very much the same.

There's a current rule in the compiler about writing single slots at a time, but this rule is potentially up for negotiation, which is why it's not included in the guarantees.

## Lexer

The RAL lexer has 5 distinct token categories:

- ID

- Keyword

- String

- Float

- Integer

However, in practice, there are only 4:

- ID/Keyword

- Operator (considered a keyword)

- String - starts with `"` or `'`

- Float/Integer - must start with a digit, `+`, or `-`,  and must contain at least one digit, `.`, or `e`.
  
  - The distinction between Float and Integer is based on Java's `Integer.parseInt` and `Float.parseFloat` functions.
    If the number parses as an integer, then that is what you get.
    Otherwise, if the number parses as a float, that's what you get.
    Otherwise, a compiler error is given.




