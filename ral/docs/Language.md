# RAL Manual: Language Reference

## Language Structure

A RAL file is made up of *declarations*.

Declarations in turn contain expressions and statements (statements containing more expressions and more statements).

In particular, macros and scripts contain code, while other declarations focus more on type structure or "meta" concerns such as includes.

## Type System

RAL is designed with a relatively "modern" type system, though it lacks features such as generics (it can generally be assumed they'd be impractical anyway in a CAOS environment).

The main inspiration for RAL's type system is TypeScript, due to the similarities in requirements (i.e. translating to another language that is not necessarily as strict with types).

This includes the presence of union types that may be any one of a given set of types, and nullability based on this (for example, `Agent?` may not be directly accessed, but the `!` null-cast operator converts it to `Agent`, which may).

The built-in types are as follows:

+ `any` - This type is hard to work with due to the limitations of CAOS `set*` commands. In particular, an `any` value cannot be directly stored, as the major type (agent/string/value) cannot be determined. Nevertheless, it exists.
  
  + It is impossible to implicitly cast `any` to any value except itself.
    It is not even possible to implicitly cast `any` to a union that includes `any`, because such a union automatically collapses as all of it's other types implicitly cast to `any`.

+ `str` - String. Strings in CAOS are a list of bytes in the Windows-1252 codepage (likely different for localized versions).

+ `int` - 32-bit integer.
  
  + Note that this should *not* be directly cast to `float`, as CAOS has distinct behaviour for float/float division.
    As such, implicitly casting this to `float` is not allowed (`num` however is perfectly fine).
    If you need to perform such a cast, perform `1.0 * i` as the floating-point multiplication coerces the integer into a float.

+ `bool` - Bool. This may be implicitly cast to `int`, but not vice versa.

+ `float` - 32-bit floating-point number.

+ `null` - Null. This type is distinct from agent types in order to support strict nullability.

+ `Agent` - Agent. This is the root class that lives at `0 0 0` in the Scriptorium. Any agent may be cast to this.

+ `num` - Number. Equal to `int|float`, and so may be cast from either.

The tools with which the user may construct types are as follows:

+ Interfaces - Interfaces represent the APIs of Agents, the messages that they can receive and the fields that can be read or modified.

+ Classes - Classes are declared points in the Scriptorium's Family/Genus/Species hierarchy. There's no way to get a class that isn't named, though how the classifier of a named class is selected is up to how much the constant evaluator will let you get away with.
  Classes are based on Interfaces, both in theory and in implementation, though their distinction is simply 'having a classifier'.

+ Unions - Written as `A | B`, unions are not the most useful of tools, as there's no good way to get a value out of them except via an explicit cast.
  
  + A very specific type of union suffix, `| null`, can be written as the shortened form `?`.
    As such, `Agent?` is internally exactly equal to `Agent | null`, and thus such can and will appear in error messages.

## Declarations

Declarations are the basic element of a RAL source file.
Everything else is defined inside declarations.

### Includes/etc.

#### include

`include` includes a file, found in the set of search paths.

The only search path by default is the parent directory of the initial file. More can be added with `addSearchPath`.

If a file is included more than once, the second and onward includes are ignored.

Example:

```
include "std/c3.ral";
```

#### addSearchPath

Adds a search path for `include`. If the search path is relative, then it is relative to the current source file.

Example:

```
addSearchPath "/media/modus/ralStandardLibrary/";
include "std/c3.ral";
```

### Type System

#### typedef

`typedef` defines a name for a type.

This is usually useful to name unions.

Example:

```
typedef number integer|float;
```

#### class

`class` defines a name for a classifier in the Family/Genus/Species tree of the Scriptorium.

In addition, `extends InterfaceName` may be added an arbitrary amount of times, to indicate the class supports some interfaces.

*Be wary to avoid a situation where fields and messages will overlap. If the compiler detects this, it will error, and if it doesn't, then it is not certain which definition will "win".*

```
class Canary 2 13 3987 extends Bird;
```

#### interface

`interface` defines an interface, a set of message/script numbers and fields that may be attached to a `class`.

In addition, `extends InterfaceName` may be added an arbitrary amount of times, to indicate that supporting this interface implies supporting some other interfaces, or that this interface implies extending a class (this has it's uses, as interfaces are types).

Example:

```
interface Bird extends GoodBug;
```

#### field

`field` defines a field on a class or interface.

It consists of a field type, the class/interface name, `.` (indicating field access), the field name, and finally the Object Variable number.

For example, `ov00` would be `0`.

Example:

```
field string Canary.tweetText 1;
```

#### message

`message` declares a message on a class or interface.

It consists of a message-ID-of expression (i.e. `Agent:someMessage`) followed by the message number.

At some point explicit type declarations for parameters may become a thing, but not presently.**

**Example:

```
message Canary:tweet 1000;
```

#### script (declaration form)

`script` in it's declaration form is usually equivalent to `message`, except where `messageHook` is in play.

As such, see `message` for syntax. You should only see this in the standard library.

#### overrideOwnr

This is used in the standard library to mark specific script numbers that the engine misuses in colourful ways, in particular 101 through 105.

In this event, what would usually be `ownr`'s type is moved to `from`, and `ownr`'s type is set to the one given.

Example:

```
overrideOwnr 101 Pointer;
script Canary 101 {
// These would normally be a type error.
let Pointer pointer = ownr;
let Canary myself = from;
}
```

#### messageHook

This is used in the standard library to mark those message numbers which the engine does not treat as equivalent to script numbers.

This causes RAL to not automatically create script definitions for message definitions and vice versa.

Example:

```
messageHook 0;
script Agent:scrDeactivated 0;
message Agent:msgActivate1 0;
```

#### assertConst

`assertConst` asserts that the given constant boolean is true.

This assertion occurs at compile-time and thus is very limited in what it will accept.

This is mainly useful as a debugging tool.

Example:

```
assertConst 1; // valid
assertConst 0; // error
```

#### Constants

A constant can be declared with the syntax `myConst = 1;`.

The expression must be evaluatable at compile-time at the point of declaration - almost needless to say, this sets quite a few limits on what is permitted. However, there is enough flexibility present for useful.

*Be aware that constants overrule in-scope variable declarations. This is to ensure consistency, as the parser and evaluator do not have access to information about scope.*

Example:

```
myConst = 1;
alwaysFalse = 0;
install {
&'outv {myConst}';
if alwaysFalse {
    &'outs "This code will never be run!"';
}
}
```

### Code

#### script (statement form)

`script` in it's statement form declares a script.

RAL only allows declaring scripts on named classes (not that this is particularly hard to ensure - classes may be named with the `class` declaration).

The script may be specified as `Class:scriptName` or as `Class 123` (where 123 is the script number, declared with `message` or `script` as appropriate).

It is generally preferrable to use script names.

```
script Canary:eaten {
&'dbg: outs {"Ouchie!"}';
}
```

#### install

`install` declares the install script of an agent, used to place it in the world.

The keyword, `install`, is simply followed by a statement/block. *If multiple install sections are declared, the contents of each are merged into one big install section in the order of their declaration.*

```
install {
newSimple(Canary, "canary.c16", 1, 0, 3000);
}
```

#### remove

Like `install`, `remove` declares a global script - however, the remove script is intended to clean up the agent's Scriptorium presence, along with the agent itself.

The keyword, `remove`, is simply followed by a statement/block. *If multiple remove sections are declared, the contents of each are merged into one big remove section in the order of their declaration.*

Example:

```
remove {
scrx(Canary, Canary:tweet);
}
```

#### macro

It is reasonably evident to a programmer who has had to read any significant amount of CAOS (the Portal code is great for shredding your soul) that the lack of global named functions with arbitrary amounts of arguments in CAOS... or global named functions... or global functions... is a severe drain on the sanity of anyone with the misfortune of having to work in it.

As such, RAL includes macros, meant to act as the RAL equivalent to global functions.

In practice, RAL macros are expressions with parameters that are either aliased or copied into temporary variables.

There are two forms of macro: Statement macros and expression macros.

Both become callable expressions, but statement macros have their 'return' values aliased as accessible variables that you write to, while expression macros are simply a substitution of an expression (but see *statement expressions* in the relevant section).

The syntax of an expression macro is simply `macro NAME(PARAM...) EXPRESSION`.

It is polite to append a semicolon after an expression macro that is not a *statement expression*.

Parameters are separated by `,` and take the form of `TYPE NAME` or  `TYPE &NAME`. The presence of the `&` character, declaring the parameter as inline, is invalid (and redundant) for the return values of a statement macro, but for regular (non-return) parameters it's always valid.

Essentially, the difference is that an inline parameter is declared as if an `alias` had occurred in a scope immediately surrounding the call, while a non-inline parameter is declared as if a `let` had occurred in that same scope.

The syntax of a statement macro is `macro (RET...) NAME(PARAM...) STATEMENT`, where `RET` is of the same format as `PARAM` but without inlining being allowed (as it's redundant - all return values are inline).

It is allowed to declare multiple macros with the same name if and only if they have a different number of parameters.

Example:

```
macro textWithSideEffects() {
&'outs {"Side effect!\n"}';
return "Bloop.";
}

macro test1(str text) {
// As the argument is not inline, a temporary variable is created.
// Thus the side effects only execute once.
&'outs {text}';
&'outs {text}';
return 1;
}

macro (int retVal) test2(str &text) {
// As the argument is inline, 'text' here is substituted for the expression.
// Thus the side effects execute twice.
&'outs {text}';
&'outs {text}';
// Note that if there are any side-effects necessary in order to write to retVal, they occur here.
retVal = 1;
}

install {
test1(textWithSideEffects());
test2(textWithSideEffects());
}
```

## Statements

Statements are the unit of sequenced, non-value-returning code in RAL.

### Blocks

Blocks are one of the most basic kinds of statement. A block separates scopes, and allows multiple statements to be written in any place a single statement can be written.

Example:

```
{
    let int counter = 0;
}
// counter = 1; // would error, counter doesn't exist here
```

### Inline Statements

Inline statements, or `&`, are used to reasonably-directly write CAOS into the output.

These statements start with `&`, followed by string-embeds containing the CAOS code, and finally ending with a semicolon (`;`).

Example:

```
// Inline statements may contain expressions.
let int number = 0;
&'outv {number + 1}';
// Inline statements may be written over multiple string-embeds.
&'outs "Now is the time\n"\n'
 'outs "For all good Norns\n"\n'
 'outs "To respect the buzzing of the airlock\n"';
```

### let

`let` is used to introduce variables.

**TODO: Everything you ever wanted to know about let**

### alias

`alias` is used to retype variables, provide different names for them, or outright treat complex expressions as simple variables (re-run on every use, mind).

It also works nicely as a teaching mechanism for the "odd" parts of RAL macros...

**TODO: The Identity Theft Of `targ`**

### if

`if` is a conditional branch statement.

**TODO: To If Or Not To If**

### while

**TODO everything**

### break

**TODO everything**

### foreach

**TODO everything**

### with

**TODO everything**

### Expression Statements and Assignment Statements

Assignment statements assign some expressions to some other expressions.

Expression statements are like assignment statements, but no assignment has been specified, so the necessary amount of discard variables are created and the expression is "assigned" to these variables.

**TODO examples**

### Message Emitting Statements

**TODO everything**

## Expressions

Expressions are how values to be read or written are expressed.

RAL uses tuples as expressions for versatility, though it does not have a tuple type.

### Constants

Constants are the simplest kind of expression.

The available types of constant in RAL are numbers and strings. You might be tempted to count `null`, but that's internally an alias.

**TODO example**

### String Embedding

**TODO 'the astounding world of {hand}'**

### instanceof

### Message IDs

### Field Accesses

### Macro Calls

### Inline Expressions

**TODO**

### Statement Expressions

Statement expressions are expressions of statements.

Like blocks, their syntax is to surround the statements with `{}`.

However, unlike blocks, at their very end a `return ...;` statement may be provided containing the expressions to return to the caller.

Macros typically use these and don't show it.

**TODO: Examples**

### Regular Ol' Brackets

**TODO: ...**

### Explicit Casts

**TODO: Cast McCasty And The Runtime Error Skull**

### Variable IDs

Variable IDs represent variables or expressions declared or aliased by `let` and `alias`, among other causes.

#### Initial Scope

The initial scope is the set of variables available in every script.

Note that these variables may be retyped with `alias` and casting.

+ `ownr` - Type is usually derived from the classifier of this script, except where `overrideOwnr` intervenes.

+ `from` - Type is assumed to be `any`, usually, except `overrideOwnr` changes that.

+ `part` - Type is `int`.

+ `_p1_` - Type is `any`.

+ `_p2_` - Type is `any`.

+ `null` - Type is `null`.

+ `targ` - Type is `Agent?`.
