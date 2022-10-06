# RAL Language Reference: Overview

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

# 
