# Snohetta

Faster Scala BigInteger.

Developed during my personal research with projects that required faster math libraries but that used Scala.

## Usage

See tests on how to use this code.

## Building and Testing

```bash
git clone git@github.com:JasonGiedymin/Snohetta.git
sbt compile
sbt test
```

## Status

JVM Cold:

    [info]   + Scala BigInt: 5620 ms
    [info]   + Scala BigInt2: 2173 ms
    [info]   + BigInt2 Parallel: 648 ms

JVM Warmed:

    [info]   + Scala BigInt: 5510 ms
    [info]   + Scala BigInt2: 1831 ms
    [info]   + BigInt2 Parallel: 561 ms

```text
[info] Loading project definition from /Users/jason/programming/github/Snohetta/project
[info] Set current project to Snohetta (in build file:/Users/jason/programming/github/Snohetta/)
[info] Compiling 1 Scala source to /Users/jason/programming/github/Snohetta/target/scala-2.10/test-classes...
[info] BasicSpec:
[info] BigInt2
[info]   must be able to add
[info]   - small Integers
[info]   - large Integers
[info]   must be able to subtract
[info]   - small Integers
[info]   - large Integers
[info]   must be able to multiply
[info]   - small Integers
[info]   - large Integers
[info]   must be able to divide
[info]   - small Integers
[info]   - large Integers
[info]   must be faster than BigInt
[info]   + Scala  BigInt: 5620 ms
[info]   + Scala BigInt2: 2173 ms
[info]   + BigInt2 Parallel: 648 ms
```
