## Overview

This project contains the Control Flow Automata (CFA) formalism. Its main purpose is to describe programs as a graphs, where nodes correspond to program locations and edges are annotated with program statements.
The project contains:
* Classes to represent CFAs.
* A domain specific language (DSL) to parse CFAs from a textual representation.

### Related projects

* [`cfa-analysis`](../cfa-analysis/README.md): CFA specific analysis modules enabling the algorithms to operate on them.
* [`cfa-cli`](../cfa-cli/README.md): An executable (command line) tool for running analyses on CFAs.

## CFA formalism

A CFA is a directed graph (`V`, `L`, `E`) with

* variables `V = {v1, v2, ..., vn}`,
* locations `L`, with dedicated initial (`l0`), final (`lf`) and error (`le`) locations,
* edges `E` between locations, labeled with statements over the variables.

Currently, there are three kind of supported statements.
* Assignments have the form `v := expr`, where `expr` is a side-effect free expression with the same type as `v`.
After performing the assignment statement, the value of variable `v` is the result of evaluating `expr`.
For example, if `x` is `1` and the assignment `x := x + 1` is performed, `x` becomes `2` (and the rest of the variables are unchanged).
* Assumptions have the form `assume expr`, where `expr` is a side-effect free Boolean expression.
It is only possible to take the edge if `expr` evaluates to true.
For example, `assume x == 0` can be taken if and only if `x` equals `0`.
After the assumption, variables are unchanged.
* Havocs have the form `havoc v`.
After performing the havoc, `v` is assigned a non-deterministic value.
This can be used to simulate non-deterministic input from the user or the environment.

Algorithms are usually interested in proving that the error location is not reachable.
For more information see Section 2.1 of [our JAR paper](https://link.springer.com/content/pdf/10.1007%2Fs10817-019-09535-x.pdf).


Variables of the CFA can have the following types.
- `bool`: Booleans.
- `int`: Mathematical, unbounded SMT integers.
- `rat`: Rational numbers (implemented as SMT reals).
- `[K] -> V`: SMT arrays (associative maps) from a given key type `K` to a value type `V`.

Expressions of the CFA include the following.
- Identifiers (variables).
- Literals, e.g., `true`, `false` (Bool), `0`, `123` (integer), `4 % 5` (rational).
- Comparison, e.g., `=`, `/=`, `<`, `>`, `<=`, `>=`.
- Boolean operators, e.g., `and`, `or`, `not`, `imply`, `iff`.
- Arithmetic, e.g., `+`, `-`, `/`, `*`, `mod`, `rem`.
- Conditional: `if . then . else .`
- Array read (`a[i]`) and write (`a[i <- v]`).

### Textual representation (DSL)

An example CFA realizing a counter:

```
main process counter {
    var x : int

    init loc L0
    loc L1
    loc L2
    loc L3
    final loc END
    error loc ERR

    L0 -> L1 { x := 0 }
    L1 -> L2 { assume x < 5 }
    L1 -> L3 { assume not (x < 5) }
    L2 -> L1 { x := x + 1 }
    L3 -> END { assume x <= 5 }
    L3 -> ERR { assume not (x <= 5) }
}
```

Variables can be defined by `var <NAME> : <TYPE>`, locations by `(init|final|error|) loc <NAME>`, and edges by `<SOURCE> -> <TARGET> {<STATEMENT>}`.
Note that it is also possible to include multiple statements on one edge (in new lines).

See _src/test/resources_ for more examples and _src/main/antlr_ for the full grammar.

### C frontend

[Gazer](https://github.com/FTSRG/gazer) is an LLVM-based frontend for Theta that can translate C programs into CFAs, run Theta and map the verification results back to the C source level.
