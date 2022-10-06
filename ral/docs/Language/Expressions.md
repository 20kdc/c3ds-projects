# RAL Language Reference: Expressions

Expressions are how values to be read *or written* are expressed.

RAL uses tuples as expressions for versatility, though it does not have a tuple type.

## Constants

Constants are the simplest kind of expression.

The available types of constant in RAL are numbers and strings. You might be tempted to count `null`, but that's internally an alias.

```
1234; // int
"Hello!"; // str
1234.56; // float
```

## Operators

Operators are used to perform simple operations on values such as numbers and strings.

The mathematical operators are `+` (add/string concatenate), `-` (subtract), `/` (divide), `*` (multiply).

Modulo in CAOS has specific requirements and thus in RAL has been restrained to a macro.

The boolean operators are `!` (logical NOT), `&&` (logical AND), and `||` (logical OR).

The bitwise operators are `~` (bitwise NOT), `&` (bitwise AND), and `|` (bitwise OR).

The comparison operators are `>=` (above or equal), `<=` (below or equal), `>` (above), `<` (below), `!=` (not equal), and `==` (equal).

### The Secret Operator: `,`

`,` is an operator which merges expressions together into a group. This is what creates the tuples, and how macros have more than one parameter.

## Increment/Decrement Expressions

## String Embedding

Strings surrounded by `'` instead of `"` allow for "embedding". In these strings, sections surrounded by `{}` are expressions - the values of which are inserted into the string.

```
outs('The hand is named {hand()}.'); // The hand is named Lambert.
```

## instanceof

`instanceof` checks if a given value matches a given classifier (returning false if the value is null).

The amount of checking performed is based on the existing type.

```
let isDolphin = targ instanceof Dolphin;
```

## Message/Script IDs

Message/script ID expressions are used when other methods of writing message and script IDs do not apply.

They consist of the type name, a separator (`:` for scripts, `->` for messages), and the name of the message or script.

```
scrx(Dolphin, Dolphin:chirp);
```

## Field Accesses

Field accesses take the form of a suffix `.` followed by the ID of a field on the given agent.

They allow accessing fields on objects (though it's relatively inefficient to access fields on agents not `ownr` or `targ`).

```
kitten.meowing = true;
```

## Macro Calls

Macro calls look into the set of defined macros and call one.

**TODO**

## Inline Expressions

Inline expressions are `&` followed by the syntax for a string embedding.
However, rather than this being a real string embedding, variables are instead replaced with their CAOS variants.

Somethihng of a hiccup is that the type is `any` - explicit casts are particularly useful for this.

**TODO**

## Statement Expressions

Statement expressions are expressions of statements.

Like blocks, their syntax is to surround the statements with `{}`.

However, unlike blocks, at their very end a `return ...;` statement may be provided containing the expressions to return to the caller.

Macros typically use these and don't show it.

**TODO: Examples**

## Regular Ol' Brackets

While to some this may seem obvious, any expression may be surrounded by `()` in order to isolate it from other expressions structurally.

## Explicit Casts

Explicit casts, written as `!` followed by something that is not an ID (indicates cast to non-nullable) or by a type (indicates cast to that type), mostly ignores the existing type of the value in favour of an overridden one.
This works both ways and may be used to cast a variable you are going to write into.

This is particularly important for working with inline statements and expressions.

```
let modu = &'modu'!str;
```

## Variable IDs

Variable IDs represent variables or expressions declared or aliased by `let` and `alias`, among other causes.

### Initial Scope

The initial scope is the set of variables available in every script.

Note that these variables may be retyped with `alias` and casting.

+ `ownr` - Type is usually derived from the classifier of this script, except where `overrideOwnr` intervenes.

+ `from` - Type is assumed to be `any`, usually, except `overrideOwnr` changes that.

+ `part` - Type is `int`.

+ `_p1_` - Type is `any`.

+ `_p2_` - Type is `any`.

+ `null` - Type is `null`.

+ `targ` - Type is `Agent?`.
