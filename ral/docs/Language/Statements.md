# RAL Language Reference: Statements

Statements are the unit of sequenced, non-value-returning code in RAL.

## Blocks

Blocks are one of the most basic kinds of statement. A block separates scopes, and allows multiple statements to be written in any place a single statement can be written.

Example:

```
{
    let int counter = 0;
}
// counter = 1; // would error, counter doesn't exist here
```

## Inline Statements

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

## let

`let` is used to introduce variables.

**TODO: Everything you ever wanted to know about let**

## alias

`alias` is used to retype variables, provide different names for them, or outright treat complex expressions as simple variables (re-run on every use, mind).

It also works nicely as a teaching mechanism for the "odd" parts of RAL macros...

**TODO: The Identity Theft Of `targ`**

## if

`if` is a conditional branch statement.

**TODO: To If Or Not To If**

## while

**TODO everything**

## break

**TODO everything**

## foreach

**TODO everything**

## with

**TODO everything**

## Modify-Assignment Expressions

**TODO everything**

## Expression Statements and Assignment Statements

Assignment statements assign some expressions to some other expressions.

Expression statements are like assignment statements, but no assignment has been specified, so the necessary amount of discard variables are created and the expression is "assigned" to these variables.

**TODO examples**

## Message Emitting Statements

**TODO everything***
