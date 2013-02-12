/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Portions Copyright (c) 1995  Colin Plumb.  All rights reserved.
 */

package math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Arrays;
import java.util.Random;

/**
 * Immutable arbitrary-precision integers.  All operations behave as if
 * BigIntegers were represented in two's-complement notation (like Java's
 * primitive integer types).  BigInteger provides analogues to all of Java's
 * primitive integer operators, and all relevant methods from java.lang.Math.
 * Additionally, BigInteger provides operations for modular arithmetic, GCD
 * calculation, primality testing, prime generation, bit manipulation,
 * and a few other miscellaneous operations.
 *
 * <p>Semantics of arithmetic operations exactly mimic those of Java's integer
 * arithmetic operators, as defined in <i>The Java Language Specification</i>.
 * For example, division by zero throws an {@code ArithmeticException}, and
 * division of a negative by a positive yields a negative (or zero) remainder.
 * All of the details in the Spec concerning overflow are ignored, as
 * BigIntegers are made as large as necessary to accommodate the results of an
 * operation.
 *
 * <p>Semantics of shift operations extend those of Java's shift operators
 * to allow for negative shift distances.  A right-shift with a negative
 * shift distance results in a left shift, and vice-versa.  The unsigned
 * right shift operator ({@code >>>}) is omitted, as this operation makes
 * little sense in combination with the "infinite word size" abstraction
 * provided by this class.
 *
 * <p>Semantics of bitwise logical operations exactly mimic those of Java's
 * bitwise integer operators.  The binary operators ({@code and},
 * {@code or}, {@code xor}) implicitly perform sign extension on the shorter
 * of the two operands prior to performing the operation.
 *
 * <p>Comparison operations perform signed integer comparisons, analogous to
 * those performed by Java's relational and equality operators.
 *
 * <p>Modular arithmetic operations are provided to compute residues, perform
 * exponentiation, and compute multiplicative inverses.  These methods always
 * return a non-negative result, between {@code 0} and {@code (modulus - 1)},
 * inclusive.
 *
 * <p>Bit operations operate on a single bit of the two's-complement
 * representation of their operand.  If necessary, the operand is sign-
 * extended so that it contains the designated bit.  None of the single-bit
 * operations can produce a BigInteger with a different sign from the
 * BigInteger being operated on, as they affect only a single bit, and the
 * "infinite word size" abstraction provided by this class ensures that there
 * are infinitely many "virtual sign bits" preceding each BigInteger.
 *
 * <p>For the sake of brevity and clarity, pseudo-code is used throughout the
 * descriptions of BigInteger methods.  The pseudo-code expression
 * {@code (i + j)} is shorthand for "a BigInteger whose value is
 * that of the BigInteger {@code i} plus that of the BigInteger {@code j}."
 * The pseudo-code expression {@code (i == j)} is shorthand for
 * "{@code true} if and only if the BigInteger {@code i} represents the same
 * value as the BigInteger {@code j}."  Other pseudo-code expressions are
 * interpreted similarly.
 *
 * <p>All methods and constructors in this class throw
 * {@code NullPointerException} when passed
 * a null object reference for any input parameter.
 *
 * @see     BigDecimal
 * @author  Josh Bloch
 * @author  Michael McCloskey
 * @author  Alan Eliasen
 * @author  Tim Buktu
 * @since JDK1.1
 */

public class BigInteger extends Number implements Comparable<BigInteger> {

    static final long INFLATED = Long.MIN_VALUE;

    /**
     * The signum of this BigInteger: -1 for negative, 0 for zero, or
     * 1 for positive.  Note that the BigInteger zero <i>must</i> have
     * a signum of 0.  This is necessary to ensures that there is exactly one
     * representation for each BigInteger value.
     *
     * @serial
     */
    final int signum;

    /**
     * The magnitude of this BigInteger, in <i>big-endian</i> order: the
     * zeroth element of this array is the most-significant int of the
     * magnitude.  The magnitude must be "minimal" in that the most-significant
     * int ({@code mag[0]}) must be non-zero.  This is necessary to
     * ensure that there is exactly one representation for each BigInteger
     * value.  Note that this implies that the BigInteger zero has a
     * zero-length mag array.
     */
    final int[] mag;

    // These "redundant fields" are initialized with recognizable nonsense
    // values, and cached the first time they are needed (or never, if they
    // aren't needed).

    /**
     * One plus the bitCount of this BigInteger. Zeros means unitialized.
     *
     * @serial
     * @see #bitCount
     * @deprecated Deprecated since logical value is offset from stored
     * value and correction factor is applied in accessor method.
     */
    @Deprecated
    private int bitCount;

    /**
     * One plus the bitLength of this BigInteger. Zeros means unitialized.
     * (either value is acceptable).
     *
     * @serial
     * @see #bitLength()
     * @deprecated Deprecated since logical value is offset from stored
     * value and correction factor is applied in accessor method.
     */
    @Deprecated
    private int bitLength;

    /**
     * Two plus the lowest set bit of this BigInteger, as returned by
     * getLowestSetBit().
     *
     * @serial
     * @see #getLowestSetBit
     * @deprecated Deprecated since logical value is offset from stored
     * value and correction factor is applied in accessor method.
     */
    @Deprecated
    private int lowestSetBit;

    /**
     * Two plus the index of the lowest-order int in the magnitude of this
     * BigInteger that contains a nonzero int, or -2 (either value is acceptable).
     * The least significant int has int-number 0, the next int in order of
     * increasing significance has int-number 1, and so forth.
     * @deprecated Deprecated since logical value is offset from stored
     * value and correction factor is applied in accessor method.
     */
    @Deprecated
    private int firstNonzeroIntNum;

    /**
     * This mask is used to obtain the value of an int as if it were unsigned.
     */
    final static long LONG_MASK = 0xffffffffL;

    /**
     * The threshold value for using Karatsuba multiplication.  If the number
     * of ints in both mag arrays are greater than this number, then
     * Karatsuba multiplication will be used.   This value is found
     * experimentally to work well.
     */
    private static final int KARATSUBA_THRESHOLD = 50;

    /**
     * The threshold value for using 3-way Toom-Cook multiplication.
     * If the number of ints in both mag arrays are greater than this number,
     * then Toom-Cook multiplication will be used.   This value is found
     * experimentally to work well.
     */
    private static final int TOOM_COOK_THRESHOLD = 75;

    /**
     * The threshold value for using Karatsuba squaring.  If the number
     * of ints in the number are larger than this value,
     * Karatsuba squaring will be used.   This value is found
     * experimentally to work well.
     */
    private static final int KARATSUBA_SQUARE_THRESHOLD = 90;

    /**
     * The threshold value for using Toom-Cook squaring.  If the number
     * of ints in the number are larger than this value,
     * Toom-Cook squaring will be used.   This value is found
     * experimentally to work well.
     */
    private static final int TOOM_COOK_SQUARE_THRESHOLD = 140;

    /**
     * The threshold value for using Burnikel-Ziegler division.  If the number
     * of ints in the number are larger than this value,
     * Burnikel-Ziegler division will be used.   This value is found
     * experimentally to work well.
     */
    private static final int BURNIKEL_ZIEGLER_THRESHOLD = 50;

    /**
     * The threshold value, in bits, for using Newton iteration when
     * computing the reciprocal of a number.
     */
    private static final int NEWTON_THRESHOLD = 100;

    //Constructors

    /**
     * Translates a byte array containing the two's-complement binary
     * representation of a BigInteger into a BigInteger.  The input array is
     * assumed to be in <i>big-endian</i> byte-order: the most significant
     * byte is in the zeroth element.
     *
     * @param  val big-endian two's-complement binary representation of
     *         BigInteger.
     * @throws NumberFormatException {@code val} is zero bytes long.
     */
    public BigInteger(byte[] val) {
        if (val.length == 0)
            throw new NumberFormatException("Zero length BigInteger");

        if (val[0] < 0) {
            mag = makePositive(val);
            signum = -1;
        } else {
            mag = stripLeadingZeroBytes(val);
            signum = (mag.length == 0 ? 0 : 1);
        }
    }

    /**
     * This private constructor translates an int array containing the
     * two's-complement binary representation of a BigInteger into a
     * BigInteger. The input array is assumed to be in <i>big-endian</i>
     * int-order: the most significant int is in the zeroth element.
     */
    private BigInteger(int[] val) {
        if (val.length == 0)
            throw new NumberFormatException("Zero length BigInteger");

        if (val[0] < 0) {
            mag = makePositive(val);
            signum = -1;
        } else {
            mag = trustedStripLeadingZeroInts(val);
            signum = (mag.length == 0 ? 0 : 1);
        }
    }

    /**
     * Translates the sign-magnitude representation of a BigInteger into a
     * BigInteger.  The sign is represented as an integer signum value: -1 for
     * negative, 0 for zero, or 1 for positive.  The magnitude is a byte array
     * in <i>big-endian</i> byte-order: the most significant byte is in the
     * zeroth element.  A zero-length magnitude array is permissible, and will
     * result in a BigInteger value of 0, whether signum is -1, 0 or 1.
     *
     * @param  signum signum of the number (-1 for negative, 0 for zero, 1
     *         for positive).
     * @param  magnitude big-endian binary representation of the magnitude of
     *         the number.
     * @throws NumberFormatException {@code signum} is not one of the three
     *         legal values (-1, 0, and 1), or {@code signum} is 0 and
     *         {@code magnitude} contains one or more non-zero bytes.
     */
    public BigInteger(int signum, byte[] magnitude) {
        this.mag = stripLeadingZeroBytes(magnitude);

        if (signum < -1 || signum > 1)
            throw(new NumberFormatException("Invalid signum value"));

        if (this.mag.length==0) {
            this.signum = 0;
        } else {
            if (signum == 0)
                throw(new NumberFormatException("signum-magnitude mismatch"));
            this.signum = signum;
        }
    }

    /**
     * A constructor for internal use that translates the sign-magnitude
     * representation of a BigInteger into a BigInteger. It checks the
     * arguments and copies the magnitude so this constructor would be
     * safe for external use.
     */
    private BigInteger(int signum, int[] magnitude) {
        this.mag = stripLeadingZeroInts(magnitude);

        if (signum < -1 || signum > 1)
            throw(new NumberFormatException("Invalid signum value"));

        if (this.mag.length==0) {
            this.signum = 0;
        } else {
            if (signum == 0)
                throw(new NumberFormatException("signum-magnitude mismatch"));
            this.signum = signum;
        }
    }

    /**
     * Translates the String representation of a BigInteger in the
     * specified radix into a BigInteger.  The String representation
     * consists of an optional minus or plus sign followed by a
     * sequence of one or more digits in the specified radix.  The
     * character-to-digit mapping is provided by {@code
     * Character.digit}.  The String may not contain any extraneous
     * characters (whitespace, for example).
     *
     * @param val String representation of BigInteger.
     * @param radix radix to be used in interpreting {@code val}.
     * @throws NumberFormatException {@code val} is not a valid representation
     *         of a BigInteger in the specified radix, or {@code radix} is
     *         outside the range from {@link Character#MIN_RADIX} to
     *         {@link Character#MAX_RADIX}, inclusive.
     * @see    Character#digit
     */
    public BigInteger(String val, int radix) {
        int cursor = 0, numDigits;
        final int len = val.length();

        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            throw new NumberFormatException("Radix out of range");
        if (len == 0)
            throw new NumberFormatException("Zero length BigInteger");

        // Check for at most one leading sign
        int sign = 1;
        int index1 = val.lastIndexOf('-');
        int index2 = val.lastIndexOf('+');
        if ((index1 + index2) <= -1) {
            // No leading sign character or at most one leading sign character
            if (index1 == 0 || index2 == 0) {
                cursor = 1;
                if (len == 1)
                    throw new NumberFormatException("Zero length BigInteger");
            }
            if (index1 == 0)
                sign = -1;
        } else
            throw new NumberFormatException("Illegal embedded sign character");

        // Skip leading zeros and compute number of digits in magnitude
        while (cursor < len &&
                Character.digit(val.charAt(cursor), radix) == 0)
            cursor++;
        if (cursor == len) {
            signum = 0;
            mag = ZERO.mag;
            return;
        }

        numDigits = len - cursor;
        signum = sign;

        // Pre-allocate array of expected size. May be too large but can
        // never be too small. Typically exact.
        int numBits = (int)(((numDigits * bitsPerDigit[radix]) >>> 10) + 1);
        int numWords = (numBits + 31) >>> 5;
        int[] magnitude = new int[numWords];

        // Process first (potentially short) digit group
        int firstGroupLen = numDigits % digitsPerInt[radix];
        if (firstGroupLen == 0)
            firstGroupLen = digitsPerInt[radix];
        String group = val.substring(cursor, cursor += firstGroupLen);
        magnitude[numWords - 1] = Integer.parseInt(group, radix);
        if (magnitude[numWords - 1] < 0)
            throw new NumberFormatException("Illegal digit");

        // Process remaining digit groups
        int superRadix = intRadix[radix];
        int groupVal = 0;
        while (cursor < len) {
            group = val.substring(cursor, cursor += digitsPerInt[radix]);
            groupVal = Integer.parseInt(group, radix);
            if (groupVal < 0)
                throw new NumberFormatException("Illegal digit");
            destructiveMulAdd(magnitude, superRadix, groupVal);
        }
        // Required for cases where the array was overallocated.
        mag = trustedStripLeadingZeroInts(magnitude);
    }

    /*
     * Constructs a new BigInteger using a char array with radix=10.
     * Sign is precalculated outside and not allowed in the val.
     */
    BigInteger(char[] val, int sign, int len) {
        int cursor = 0, numDigits;

        // Skip leading zeros and compute number of digits in magnitude
        while (cursor < len && Character.digit(val[cursor], 10) == 0) {
            cursor++;
        }
        if (cursor == len) {
            signum = 0;
            mag = ZERO.mag;
            return;
        }

        numDigits = len - cursor;
        signum = sign;
        // Pre-allocate array of expected size
        int numWords;
        if (len < 10) {
            numWords = 1;
        } else {
            int numBits = (int)(((numDigits * bitsPerDigit[10]) >>> 10) + 1);
            numWords = (numBits + 31) >>> 5;
        }
        int[] magnitude = new int[numWords];

        // Process first (potentially short) digit group
        int firstGroupLen = numDigits % digitsPerInt[10];
        if (firstGroupLen == 0)
            firstGroupLen = digitsPerInt[10];
        magnitude[numWords - 1] = parseInt(val, cursor,  cursor += firstGroupLen);

        // Process remaining digit groups
        while (cursor < len) {
            int groupVal = parseInt(val, cursor, cursor += digitsPerInt[10]);
            destructiveMulAdd(magnitude, intRadix[10], groupVal);
        }
        mag = trustedStripLeadingZeroInts(magnitude);
    }

    // Create an integer with the digits between the two indexes
    // Assumes start < end. The result may be negative, but it
    // is to be treated as an unsigned value.
    private int parseInt(char[] source, int start, int end) {
        int result = Character.digit(source[start++], 10);
        if (result == -1)
            throw new NumberFormatException(new String(source));

        for (int index = start; index<end; index++) {
            int nextVal = Character.digit(source[index], 10);
            if (nextVal == -1)
                throw new NumberFormatException(new String(source));
            result = 10*result + nextVal;
        }

        return result;
    }

    // bitsPerDigit in the given radix times 1024
    // Rounded up to avoid underallocation.
    private static long bitsPerDigit[] = { 0, 0,
            1024, 1624, 2048, 2378, 2648, 2875, 3072, 3247, 3402, 3543, 3672,
            3790, 3899, 4001, 4096, 4186, 4271, 4350, 4426, 4498, 4567, 4633,
            4696, 4756, 4814, 4870, 4923, 4975, 5025, 5074, 5120, 5166, 5210,
            5253, 5295};

    // Multiply x array times word y in place, and add word z
    private static void destructiveMulAdd(int[] x, int y, int z) {
        // Perform the multiplication word by word
        long ylong = y & LONG_MASK;
        long zlong = z & LONG_MASK;
        int len = x.length;

        long product = 0;
        long carry = 0;
        for (int i = len-1; i >= 0; i--) {
            product = ylong * (x[i] & LONG_MASK) + carry;
            x[i] = (int)product;
            carry = product >>> 32;
        }

        // Perform the addition
        long sum = (x[len-1] & LONG_MASK) + zlong;
        x[len-1] = (int)sum;
        carry = sum >>> 32;
        for (int i = len-2; i >= 0; i--) {
            sum = (x[i] & LONG_MASK) + carry;
            x[i] = (int)sum;
            carry = sum >>> 32;
        }
    }

    /**
     * Translates the decimal String representation of a BigInteger into a
     * BigInteger.  The String representation consists of an optional minus
     * sign followed by a sequence of one or more decimal digits.  The
     * character-to-digit mapping is provided by {@code Character.digit}.
     * The String may not contain any extraneous characters (whitespace, for
     * example).
     *
     * @param val decimal String representation of BigInteger.
     * @throws NumberFormatException {@code val} is not a valid representation
     *         of a BigInteger.
     * @see    Character#digit
     */
    public BigInteger(String val) {
        this(val, 10);
    }

    /**
     * Constructs a randomly generated BigInteger, uniformly distributed over
     * the range 0 to (2<sup>{@code numBits}</sup> - 1), inclusive.
     * The uniformity of the distribution assumes that a fair source of random
     * bits is provided in {@code rnd}.  Note that this constructor always
     * constructs a non-negative BigInteger.
     *
     * @param  numBits maximum bitLength of the new BigInteger.
     * @param  rnd source of randomness to be used in computing the new
     *         BigInteger.
     * @throws IllegalArgumentException {@code numBits} is negative.
     * @see #bitLength()
     */
    public BigInteger(int numBits, Random rnd) {
        this(1, randomBits(numBits, rnd));
    }

    private static byte[] randomBits(int numBits, Random rnd) {
        if (numBits < 0)
            throw new IllegalArgumentException("numBits must be non-negative");
        int numBytes = (int)(((long)numBits+7)/8); // avoid overflow
        byte[] randomBits = new byte[numBytes];

        // Generate random bytes and mask out any excess bits
        if (numBytes > 0) {
            rnd.nextBytes(randomBits);
            int excessBits = 8*numBytes - numBits;
            randomBits[0] &= (1 << (8-excessBits)) - 1;
        }
        return randomBits;
    }

    /**
     * Constructs a randomly generated positive BigInteger that is probably
     * prime, with the specified bitLength.
     *
     * <p>It is recommended that the {@link #probablePrime probablePrime}
     * method be used in preference to this constructor unless there
     * is a compelling need to specify a certainty.
     *
     * @param  bitLength bitLength of the returned BigInteger.
     * @param  certainty a measure of the uncertainty that the caller is
     *         willing to tolerate.  The probability that the new BigInteger
     *         represents a prime number will exceed
     *         (1 - 1/2<sup>{@code certainty}</sup>).  The execution time of
     *         this constructor is proportional to the value of this parameter.
     * @param  rnd source of random bits used to select candidates to be
     *         tested for primality.
     * @throws ArithmeticException {@code bitLength < 2}.
     * @see    #bitLength()
     */
    public BigInteger(int bitLength, int certainty, Random rnd) {
        BigInteger prime;

        if (bitLength < 2)
            throw new ArithmeticException("bitLength < 2");
        prime = (bitLength < SMALL_PRIME_THRESHOLD
                ? smallPrime(bitLength, certainty, rnd)
                : largePrime(bitLength, certainty, rnd));
        signum = 1;
        mag = prime.mag;
    }

    // Minimum size in bits that the requested prime number has
    // before we use the large prime number generating algorithms.
    // The cutoff of 95 was chosen empirically for best performance.
    private static final int SMALL_PRIME_THRESHOLD = 95;

    // Certainty required to meet the spec of probablePrime
    private static final int DEFAULT_PRIME_CERTAINTY = 100;

    /**
     * Returns a positive BigInteger that is probably prime, with the
     * specified bitLength. The probability that a BigInteger returned
     * by this method is composite does not exceed 2<sup>-100</sup>.
     *
     * @param  bitLength bitLength of the returned BigInteger.
     * @param  rnd source of random bits used to select candidates to be
     *         tested for primality.
     * @return a BigInteger of {@code bitLength} bits that is probably prime
     * @throws ArithmeticException {@code bitLength < 2}.
     * @see    #bitLength()
     * @since 1.4
     */
    public static BigInteger probablePrime(int bitLength, Random rnd) {
        if (bitLength < 2)
            throw new ArithmeticException("bitLength < 2");

        return (bitLength < SMALL_PRIME_THRESHOLD ?
                smallPrime(bitLength, DEFAULT_PRIME_CERTAINTY, rnd) :
                largePrime(bitLength, DEFAULT_PRIME_CERTAINTY, rnd));
    }

    /**
     * Find a random number of the specified bitLength that is probably prime.
     * This method is used for smaller primes, its performance degrades on
     * larger bitlengths.
     *
     * This method assumes bitLength > 1.
     */
    private static BigInteger smallPrime(int bitLength, int certainty, Random rnd) {
        int magLen = (bitLength + 31) >>> 5;
        int temp[] = new int[magLen];
        int highBit = 1 << ((bitLength+31) & 0x1f);  // High bit of high int
        int highMask = (highBit << 1) - 1;  // Bits to keep in high int

        while(true) {
            // Construct a candidate
            for (int i=0; i<magLen; i++)
                temp[i] = rnd.nextInt();
            temp[0] = (temp[0] & highMask) | highBit;  // Ensure exact length
            if (bitLength > 2)
                temp[magLen-1] |= 1;  // Make odd if bitlen > 2

            BigInteger p = new BigInteger(temp, 1);

            // Do cheap "pre-test" if applicable
            if (bitLength > 6) {
                long r = p.remainder(SMALL_PRIME_PRODUCT).longValue();
                if ((r%3==0)  || (r%5==0)  || (r%7==0)  || (r%11==0) ||
                        (r%13==0) || (r%17==0) || (r%19==0) || (r%23==0) ||
                        (r%29==0) || (r%31==0) || (r%37==0) || (r%41==0))
                    continue; // Candidate is composite; try another
            }

            // All candidates of bitLength 2 and 3 are prime by this point
            if (bitLength < 4)
                return p;

            // Do expensive test if we survive pre-test (or it's inapplicable)
            if (p.primeToCertainty(certainty, rnd))
                return p;
        }
    }

    private static final BigInteger SMALL_PRIME_PRODUCT
            = valueOf(3L*5*7*11*13*17*19*23*29*31*37*41);

    /**
     * Find a random number of the specified bitLength that is probably prime.
     * This method is more appropriate for larger bitlengths since it uses
     * a sieve to eliminate most composites before using a more expensive
     * test.
     */
    private static BigInteger largePrime(int bitLength, int certainty, Random rnd) {
        BigInteger p;
        p = new BigInteger(bitLength, rnd).setBit(bitLength-1);
        p.mag[p.mag.length-1] &= 0xfffffffe;

        // Use a sieve length likely to contain the next prime number
        int searchLen = (bitLength / 20) * 64;
        BitSieve searchSieve = new BitSieve(p, searchLen);
        BigInteger candidate = searchSieve.retrieve(p, certainty, rnd);

        while ((candidate == null) || (candidate.bitLength() != bitLength)) {
            p = p.add(BigInteger.valueOf(2*searchLen));
            if (p.bitLength() != bitLength)
                p = new BigInteger(bitLength, rnd).setBit(bitLength-1);
            p.mag[p.mag.length-1] &= 0xfffffffe;
            searchSieve = new BitSieve(p, searchLen);
            candidate = searchSieve.retrieve(p, certainty, rnd);
        }
        return candidate;
    }

    /**
     * Returns the first integer greater than this {@code BigInteger} that
     * is probably prime.  The probability that the number returned by this
     * method is composite does not exceed 2<sup>-100</sup>. This method will
     * never skip over a prime when searching: if it returns {@code p}, there
     * is no prime {@code q} such that {@code this < q < p}.
     *
     * @return the first integer greater than this {@code BigInteger} that
     *         is probably prime.
     * @throws ArithmeticException {@code this < 0}.
     * @since 1.5
     */
    public BigInteger nextProbablePrime() {
        if (this.signum < 0)
            throw new ArithmeticException("start < 0: " + this);

        // Handle trivial cases
        if ((this.signum == 0) || this.equals(ONE))
            return TWO;

        BigInteger result = this.add(ONE);

        // Fastpath for small numbers
        if (result.bitLength() < SMALL_PRIME_THRESHOLD) {

            // Ensure an odd number
            if (!result.testBit(0))
                result = result.add(ONE);

            while(true) {
                // Do cheap "pre-test" if applicable
                if (result.bitLength() > 6) {
                    long r = result.remainder(SMALL_PRIME_PRODUCT).longValue();
                    if ((r%3==0)  || (r%5==0)  || (r%7==0)  || (r%11==0) ||
                            (r%13==0) || (r%17==0) || (r%19==0) || (r%23==0) ||
                            (r%29==0) || (r%31==0) || (r%37==0) || (r%41==0)) {
                        result = result.add(TWO);
                        continue; // Candidate is composite; try another
                    }
                }

                // All candidates of bitLength 2 and 3 are prime by this point
                if (result.bitLength() < 4)
                    return result;

                // The expensive test
                if (result.primeToCertainty(DEFAULT_PRIME_CERTAINTY, null))
                    return result;

                result = result.add(TWO);
            }
        }

        // Start at previous even number
        if (result.testBit(0))
            result = result.subtract(ONE);

        // Looking for the next large prime
        int searchLen = (result.bitLength() / 20) * 64;

        while(true) {
            BitSieve searchSieve = new BitSieve(result, searchLen);
            BigInteger candidate = searchSieve.retrieve(result,
                    DEFAULT_PRIME_CERTAINTY, null);
            if (candidate != null)
                return candidate;
            result = result.add(BigInteger.valueOf(2 * searchLen));
        }
    }

    /**
     * Returns {@code true} if this BigInteger is probably prime,
     * {@code false} if it's definitely composite.
     *
     * This method assumes bitLength > 2.
     *
     * @param  certainty a measure of the uncertainty that the caller is
     *         willing to tolerate: if the call returns {@code true}
     *         the probability that this BigInteger is prime exceeds
     *         {@code (1 - 1/2<sup>certainty</sup>)}.  The execution time of
     *         this method is proportional to the value of this parameter.
     * @return {@code true} if this BigInteger is probably prime,
     *         {@code false} if it's definitely composite.
     */
    boolean primeToCertainty(int certainty, Random random) {
        int rounds = 0;
        int n = (Math.min(certainty, Integer.MAX_VALUE-1)+1)/2;

        // The relationship between the certainty and the number of rounds
        // we perform is given in the draft standard ANSI X9.80, "PRIME
        // NUMBER GENERATION, PRIMALITY TESTING, AND PRIMALITY CERTIFICATES".
        int sizeInBits = this.bitLength();
        if (sizeInBits < 100) {
            rounds = 50;
            rounds = n < rounds ? n : rounds;
            return passesMillerRabin(rounds, random);
        }

        if (sizeInBits < 256) {
            rounds = 27;
        } else if (sizeInBits < 512) {
            rounds = 15;
        } else if (sizeInBits < 768) {
            rounds = 8;
        } else if (sizeInBits < 1024) {
            rounds = 4;
        } else {
            rounds = 2;
        }
        rounds = n < rounds ? n : rounds;

        return passesMillerRabin(rounds, random) && passesLucasLehmer();
    }

    /**
     * Returns true iff this BigInteger is a Lucas-Lehmer probable prime.
     *
     * The following assumptions are made:
     * This BigInteger is a positive, odd number.
     */
    private boolean passesLucasLehmer() {
        BigInteger thisPlusOne = this.add(ONE);

        // Step 1
        int d = 5;
        while (jacobiSymbol(d, this) != -1) {
            // 5, -7, 9, -11, ...
            d = (d<0) ? Math.abs(d)+2 : -(d+2);
        }

        // Step 2
        BigInteger u = lucasLehmerSequence(d, thisPlusOne, this);

        // Step 3
        return u.mod(this).equals(ZERO);
    }

    /**
     * Computes Jacobi(p,n).
     * Assumes n positive, odd, n>=3.
     */
    private static int jacobiSymbol(int p, BigInteger n) {
        if (p == 0)
            return 0;

        // Algorithm and comments adapted from Colin Plumb's C library.
        int j = 1;
        int u = n.mag[n.mag.length-1];

        // Make p positive
        if (p < 0) {
            p = -p;
            int n8 = u & 7;
            if ((n8 == 3) || (n8 == 7))
                j = -j; // 3 (011) or 7 (111) mod 8
        }

        // Get rid of factors of 2 in p
        while ((p & 3) == 0)
            p >>= 2;
        if ((p & 1) == 0) {
            p >>= 1;
            if (((u ^ (u>>1)) & 2) != 0)
                j = -j; // 3 (011) or 5 (101) mod 8
        }
        if (p == 1)
            return j;
        // Then, apply quadratic reciprocity
        if ((p & u & 2) != 0)   // p = u = 3 (mod 4)?
            j = -j;
        // And reduce u mod p
        u = n.mod(BigInteger.valueOf(p)).intValue();

        // Now compute Jacobi(u,p), u < p
        while (u != 0) {
            while ((u & 3) == 0)
                u >>= 2;
            if ((u & 1) == 0) {
                u >>= 1;
                if (((p ^ (p>>1)) & 2) != 0)
                    j = -j;     // 3 (011) or 5 (101) mod 8
            }
            if (u == 1)
                return j;
            // Now both u and p are odd, so use quadratic reciprocity
            assert (u < p);
            int t = u; u = p; p = t;
            if ((u & p & 2) != 0) // u = p = 3 (mod 4)?
                j = -j;
            // Now u >= p, so it can be reduced
            u %= p;
        }
        return 0;
    }

    private static BigInteger lucasLehmerSequence(int z, BigInteger k, BigInteger n) {
        BigInteger d = BigInteger.valueOf(z);
        BigInteger u = ONE; BigInteger u2;
        BigInteger v = ONE; BigInteger v2;

        for (int i=k.bitLength()-2; i>=0; i--) {
            u2 = u.multiply(v).mod(n);

            v2 = v.square().add(d.multiply(u.square())).mod(n);
            if (v2.testBit(0))
                v2 = v2.subtract(n);

            v2 = v2.shiftRight(1);

            u = u2; v = v2;
            if (k.testBit(i)) {
                u2 = u.add(v).mod(n);
                if (u2.testBit(0))
                    u2 = u2.subtract(n);

                u2 = u2.shiftRight(1);
                v2 = v.add(d.multiply(u)).mod(n);
                if (v2.testBit(0))
                    v2 = v2.subtract(n);
                v2 = v2.shiftRight(1);

                u = u2; v = v2;
            }
        }
        return u;
    }

    private static volatile Random staticRandom;

    private static Random getSecureRandom() {
        if (staticRandom == null) {
            staticRandom = new java.security.SecureRandom();
        }
        return staticRandom;
    }

    /**
     * Returns true iff this BigInteger passes the specified number of
     * Miller-Rabin tests. This test is taken from the DSA spec (NIST FIPS
     * 186-2).
     *
     * The following assumptions are made:
     * This BigInteger is a positive, odd number greater than 2.
     * iterations<=50.
     */
    private boolean passesMillerRabin(int iterations, Random rnd) {
        // Find a and m such that m is odd and this == 1 + 2**a * m
        BigInteger thisMinusOne = this.subtract(ONE);
        BigInteger m = thisMinusOne;
        int a = m.getLowestSetBit();
        m = m.shiftRight(a);

        // Do the tests
        if (rnd == null) {
            rnd = getSecureRandom();
        }
        for (int i=0; i<iterations; i++) {
            // Generate a uniform random on (1, this)
            BigInteger b;
            do {
                b = new BigInteger(this.bitLength(), rnd);
            } while (b.compareTo(ONE) <= 0 || b.compareTo(this) >= 0);

            int j = 0;
            BigInteger z = b.modPow(m, this);
            while(!((j==0 && z.equals(ONE)) || z.equals(thisMinusOne))) {
                if (j>0 && z.equals(ONE) || ++j==a)
                    return false;
                z = z.modPow(TWO, this);
            }
        }
        return true;
    }

    /**
     * This internal constructor differs from its public cousin
     * with the arguments reversed in two ways: it assumes that its
     * arguments are correct, and it doesn't copy the magnitude array.
     */
    BigInteger(int[] magnitude, int signum) {
        this.signum = (magnitude.length==0 ? 0 : signum);
        this.mag = magnitude;
    }

    /**
     * This private constructor is for internal use and assumes that its
     * arguments are correct.
     */
    private BigInteger(byte[] magnitude, int signum) {
        this.signum = (magnitude.length==0 ? 0 : signum);
        this.mag = stripLeadingZeroBytes(magnitude);
    }

    //Static Factory Methods

    /**
     * Returns a BigInteger whose value is equal to that of the
     * specified {@code long}.  This "static factory method" is
     * provided in preference to a ({@code long}) constructor
     * because it allows for reuse of frequently used BigIntegers.
     *
     * @param  val value of the BigInteger to return.
     * @return a BigInteger with the specified value.
     */
    public static BigInteger valueOf(long val) {
        // If -MAX_CONSTANT < val < MAX_CONSTANT, return stashed constant
        if (val == 0)
            return ZERO;
        if (val > 0 && val <= MAX_CONSTANT)
            return posConst[(int) val];
        else if (val < 0 && val >= -MAX_CONSTANT)
            return negConst[(int) -val];

        return new BigInteger(val);
    }

    /**
     * Constructs a BigInteger with the specified value, which may not be zero.
     */
    private BigInteger(long val) {
        if (val < 0) {
            val = -val;
            signum = -1;
        } else {
            signum = 1;
        }

        int highWord = (int)(val >>> 32);
        if (highWord==0) {
            mag = new int[1];
            mag[0] = (int)val;
        } else {
            mag = new int[2];
            mag[0] = highWord;
            mag[1] = (int)val;
        }
    }

    /**
     * Returns a BigInteger with the given two's complement representation.
     * Assumes that the input array will not be modified (the returned
     * BigInteger will reference the input array if feasible).
     */
    private static BigInteger valueOf(int val[]) {
        return (val[0]>0 ? new BigInteger(val, 1) : new BigInteger(val));
    }

    /**
     * Returns an <code>n</code>-int number all of whose bits are ones
     * @param n number of ints in the <code>mag</code> array
     * @return a number equal to <code>ONE.shiftLeft(32*n).subtract(ONE)</code>
     */
    private static BigInteger ones(int n) {
        int[] mag = new int[n];
        Arrays.fill(mag, -1);
        return new BigInteger(mag, 1);
    }

    // Constants

    /**
     * Initialize static constant array when class is loaded.
     */
    private final static int MAX_CONSTANT = 16;
    private static BigInteger posConst[] = new BigInteger[MAX_CONSTANT+1];
    private static BigInteger negConst[] = new BigInteger[MAX_CONSTANT+1];
    static {
        for (int i = 1; i <= MAX_CONSTANT; i++) {
            int[] magnitude = new int[1];
            magnitude[0] = i;
            posConst[i] = new BigInteger(magnitude,  1);
            negConst[i] = new BigInteger(magnitude, -1);
        }
    }

    /**
     * The BigInteger constant zero.
     *
     * @since   1.2
     */
    public static final BigInteger ZERO = new BigInteger(new int[0], 0);

    /**
     * The BigInteger constant one.
     *
     * @since   1.2
     */
    public static final BigInteger ONE = valueOf(1);

    /**
     * The BigInteger constant two.  (Not exported.)
     */
    private static final BigInteger TWO = valueOf(2);

    /**
     * The BigInteger constant -1.  (Not exported.)
     */
    private static final BigInteger NEGATIVE_ONE = valueOf(-1);

    /**
     * The BigInteger constant ten.
     *
     * @since   1.5
     */
    public static final BigInteger TEN = valueOf(10);

    // Arithmetic Operations

    /**
     * Returns a BigInteger whose value is {@code (this + val)}.
     *
     * @param  val value to be added to this BigInteger.
     * @return {@code this + val}
     */
    public BigInteger add(BigInteger val) {
        if (val.signum == 0)
            return this;
        if (signum == 0)
            return val;
        if (val.signum == signum)
            return new BigInteger(add(mag, val.mag), signum);

        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, val.mag)
                : subtract(val.mag, mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);

        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    /**
     * Package private methods used by BigDecimal code to add a BigInteger
     * with a long. Assumes val is not equal to INFLATED.
     */
    BigInteger add(long val) {
        if (val == 0)
            return this;
        if (signum == 0)
            return valueOf(val);
        if (Long.signum(val) == signum)
            return new BigInteger(add(mag, Math.abs(val)), signum);
        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, Math.abs(val)) : subtract(Math.abs(val), mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);
        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    /**
     * Adds the contents of the int array x and long value val. This
     * method allocates a new int array to hold the answer and returns
     * a reference to that array.  Assumes x.length &gt; 0 and val is
     * non-negative
     */
    private static int[] add(int[] x, long val) {
        int[] y;
        long sum = 0;
        int xIndex = x.length;
        int[] result;
        int highWord = (int)(val >>> 32);
        if (highWord==0) {
            result = new int[xIndex];
            sum = (x[--xIndex] & LONG_MASK) + val;
            result[xIndex] = (int)sum;
        } else {
            if (xIndex == 1) {
                result = new int[2];
                sum = val  + (x[0] & LONG_MASK);
                result[1] = (int)sum;
                result[0] = (int)(sum >>> 32);
                return result;
            } else {
                result = new int[xIndex];
                sum = (x[--xIndex] & LONG_MASK) + (val & LONG_MASK);
                result[xIndex] = (int)sum;
                sum = (x[--xIndex] & LONG_MASK) + (highWord & LONG_MASK) + (sum >>> 32);
                result[xIndex] = (int)sum;
            }
        }
        // Copy remainder of longer number while carry propagation is required
        boolean carry = (sum >>> 32 != 0);
        while (xIndex > 0 && carry)
            carry = ((result[--xIndex] = x[xIndex] + 1) == 0);
        // Copy remainder of longer number
        while (xIndex > 0)
            result[--xIndex] = x[xIndex];
        // Grow result if necessary
        if (carry) {
            int bigger[] = new int[result.length + 1];
            System.arraycopy(result, 0, bigger, 1, result.length);
            bigger[0] = 0x01;
            return bigger;
        }
        return result;
    }

    /**
     * Adds the contents of the int arrays x and y. This method allocates
     * a new int array to hold the answer and returns a reference to that
     * array.
     */
    private static int[] add(int[] x, int[] y) {
        // If x is shorter, swap the two arrays
        if (x.length < y.length) {
            int[] tmp = x;
            x = y;
            y = tmp;
        }

        int xIndex = x.length;
        int yIndex = y.length;
        int result[] = new int[xIndex];
        long sum = 0;
        if(yIndex==1) {
            sum = (x[--xIndex] & LONG_MASK) + (y[0] & LONG_MASK) ;
            result[xIndex] = (int)sum;
        } else {
            // Add common parts of both numbers
            while(yIndex > 0) {
                sum = (x[--xIndex] & LONG_MASK) +
                        (y[--yIndex] & LONG_MASK) + (sum >>> 32);
                result[xIndex] = (int)sum;
            }
        }
        // Copy remainder of longer number while carry propagation is required
        boolean carry = (sum >>> 32 != 0);
        while (xIndex > 0 && carry)
            carry = ((result[--xIndex] = x[xIndex] + 1) == 0);

        // Copy remainder of longer number
        while (xIndex > 0)
            result[--xIndex] = x[xIndex];

        // Grow result if necessary
        if (carry) {
            int bigger[] = new int[result.length + 1];
            System.arraycopy(result, 0, bigger, 1, result.length);
            bigger[0] = 0x01;
            return bigger;
        }
        return result;
    }

    private static int[] subtract(long val, int[] little) {
        int highWord = (int)(val >>> 32);
        if (highWord==0) {
            int result[] = new int[1];
            result[0] = (int)(val - (little[0] & LONG_MASK));
            return result;
        } else {
            int result[] = new int[2];
            if(little.length==1) {
                long difference = ((int)val & LONG_MASK) - (little[0] & LONG_MASK);
                result[1] = (int)difference;
                // Subtract remainder of longer number while borrow propagates
                boolean borrow = (difference >> 32 != 0);
                if(borrow) {
                    result[0] = highWord - 1;
                } else {        // Copy remainder of longer number
                    result[0] = highWord;
                }
                return result;
            } else { // little.length==2
                long difference = ((int)val & LONG_MASK) - (little[1] & LONG_MASK);
                result[1] = (int)difference;
                difference = (highWord & LONG_MASK) - (little[0] & LONG_MASK) + (difference >> 32);
                result[0] = (int)difference;
                return result;
            }
        }
    }

    /**
     * Subtracts the contents of the second argument (val) from the
     * first (big).  The first int array (big) must represent a larger number
     * than the second.  This method allocates the space necessary to hold the
     * answer.
     * assumes val &gt;= 0
     */
    private static int[] subtract(int[] big, long val) {
        int highWord = (int)(val >>> 32);
        int bigIndex = big.length;
        int result[] = new int[bigIndex];
        long difference = 0;

        if (highWord==0) {
            difference = (big[--bigIndex] & LONG_MASK) - val;
            result[bigIndex] = (int)difference;
        } else {
            difference = (big[--bigIndex] & LONG_MASK) - (val & LONG_MASK);
            result[bigIndex] = (int)difference;
            difference = (big[--bigIndex] & LONG_MASK) - (highWord & LONG_MASK) + (difference >> 32);
            result[bigIndex] = (int)difference;
        }


        // Subtract remainder of longer number while borrow propagates
        boolean borrow = (difference >> 32 != 0);
        while (bigIndex > 0 && borrow)
            borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);

        // Copy remainder of longer number
        while (bigIndex > 0)
            result[--bigIndex] = big[bigIndex];

        return result;
    }

    /**
     * Returns a BigInteger whose value is {@code (this - val)}.
     *
     * @param  val value to be subtracted from this BigInteger.
     * @return {@code this - val}
     */
    public BigInteger subtract(BigInteger val) {
        if (val.signum == 0)
            return this;
        if (signum == 0)
            return val.negate();
        if (val.signum != signum)
            return new BigInteger(add(mag, val.mag), signum);

        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, val.mag)
                : subtract(val.mag, mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);
        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    /**
     * Subtracts the contents of the second int arrays (little) from the
     * first (big).  The first int array (big) must represent a larger number
     * than the second.  This method allocates the space necessary to hold the
     * answer.
     */
    private static int[] subtract(int[] big, int[] little) {
        int bigIndex = big.length;
        int result[] = new int[bigIndex];
        int littleIndex = little.length;
        long difference = 0;

        // Subtract common parts of both numbers
        while(littleIndex > 0) {
            difference = (big[--bigIndex] & LONG_MASK) -
                    (little[--littleIndex] & LONG_MASK) +
                    (difference >> 32);
            result[bigIndex] = (int)difference;
        }

        // Subtract remainder of longer number while borrow propagates
        boolean borrow = (difference >> 32 != 0);
        while (bigIndex > 0 && borrow)
            borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);

        // Copy remainder of longer number
        while (bigIndex > 0)
            result[--bigIndex] = big[bigIndex];

        return result;
    }

    /**
     * Returns a BigInteger whose value is {@code (this * val)}.
     *
     * @param  val value to be multiplied by this BigInteger.
     * @return {@code this * val}
     */
    public BigInteger multiply(BigInteger val) {
        if (val.signum == 0 || signum == 0)
            return ZERO;

        int xlen = mag.length;
        int ylen = val.mag.length;

        if ((xlen < KARATSUBA_THRESHOLD) || (ylen < KARATSUBA_THRESHOLD))
        {
            int resultSign = signum == val.signum ? 1 : -1;
            if (val.mag.length == 1) {
                return  multiplyByInt(mag,val.mag[0], resultSign);
            }
            if(mag.length == 1) {
                return multiplyByInt(val.mag,mag[0], resultSign);
            }
            int[] result = multiplyToLen(mag, xlen,
                    val.mag, ylen, null);
            result = trustedStripLeadingZeroInts(result);
            return new BigInteger(result, resultSign);
        }
        else
        if ((xlen < TOOM_COOK_THRESHOLD) && (ylen < TOOM_COOK_THRESHOLD))
            return multiplyKaratsuba(this, val);
        else
        if (!shouldMultiplySchönhageStrassen(xlen*32) || !shouldMultiplySchönhageStrassen(ylen*32))
            return multiplyToomCook3(this, val);
        else
            return multiplySchönhageStrassen(this, val);
    }

    private static BigInteger multiplyByInt(int[] x, int y, int sign) {
        if(Integer.bitCount(y)==1) {
            return new BigInteger(shiftLeft(x,Integer.numberOfTrailingZeros(y)), sign);
        }
        int xlen = x.length;
        int[] rmag =  new int[xlen + 1];
        long carry = 0;
        long yl = y & LONG_MASK;
        int rstart = rmag.length - 1;
        for (int i = xlen - 1; i >= 0; i--) {
            long product = (x[i] & LONG_MASK) * yl + carry;
            rmag[rstart--] = (int)product;
            carry = product >>> 32;
        }
        if (carry == 0L) {
            rmag = java.util.Arrays.copyOfRange(rmag, 1, rmag.length);
        } else {
            rmag[rstart] = (int)carry;
        }
        return new BigInteger(rmag, sign);
    }

    /**
     * Package private methods used by BigDecimal code to multiply a BigInteger
     * with a long. Assumes v is not equal to INFLATED.
     */
    BigInteger multiply(long v) {
        if (v == 0 || signum == 0)
            return ZERO;
        if (v == INFLATED)
            return multiply(BigInteger.valueOf(v));
        int rsign = (v > 0 ? signum : -signum);
        if (v < 0)
            v = -v;
        long dh = v >>> 32;      // higher order bits
        long dl = v & LONG_MASK; // lower order bits

        int xlen = mag.length;
        int[] value = mag;
        int[] rmag = (dh == 0L) ? (new int[xlen + 1]) : (new int[xlen + 2]);
        long carry = 0;
        int rstart = rmag.length - 1;
        for (int i = xlen - 1; i >= 0; i--) {
            long product = (value[i] & LONG_MASK) * dl + carry;
            rmag[rstart--] = (int)product;
            carry = product >>> 32;
        }
        rmag[rstart] = (int)carry;
        if (dh != 0L) {
            carry = 0;
            rstart = rmag.length - 2;
            for (int i = xlen - 1; i >= 0; i--) {
                long product = (value[i] & LONG_MASK) * dh +
                        (rmag[rstart] & LONG_MASK) + carry;
                rmag[rstart--] = (int)product;
                carry = product >>> 32;
            }
            rmag[0] = (int)carry;
        }
        if (carry == 0L)
            rmag = java.util.Arrays.copyOfRange(rmag, 1, rmag.length);
        return new BigInteger(rmag, rsign);
    }

    /**
     * Multiplies int arrays x and y to the specified lengths and places
     * the result into z. There will be no leading zeros in the resultant array.
     */
    private int[] multiplyToLen(int[] x, int xlen, int[] y, int ylen, int[] z) {
        int xstart = xlen - 1;
        int ystart = ylen - 1;

        if (z == null || z.length < (xlen+ ylen))
            z = new int[xlen+ylen];

        long carry = 0;
        for (int j=ystart, k=ystart+1+xstart; j>=0; j--, k--) {
            long product = (y[j] & LONG_MASK) *
                    (x[xstart] & LONG_MASK) + carry;
            z[k] = (int)product;
            carry = product >>> 32;
        }
        z[xstart] = (int)carry;

        for (int i = xstart-1; i >= 0; i--) {
            carry = 0;
            for (int j=ystart, k=ystart+1+i; j>=0; j--, k--) {
                long product = (y[j] & LONG_MASK) *
                        (x[i] & LONG_MASK) +
                        (z[k] & LONG_MASK) + carry;
                z[k] = (int)product;
                carry = product >>> 32;
            }
            z[i] = (int)carry;
        }
        return z;
    }

    /**
     * Multiplies two BigIntegers using the Karatsuba multiplication
     * algorithm.  This is a recursive divide-and-conquer algorithm which is
     * more efficient for large numbers than what is commonly called the
     * "grade-school" algorithm used in multiplyToLen.  If the numbers to be
     * multiplied have length n, the "grade-school" algorithm has an
     * asymptotic complexity of O(n^2).  In contrast, the Karatsuba algorithm
     * has complexity of O(n^(log2(3))), or O(n^1.585).  It achieves this
     * increased performance by doing 3 multiplies instead of 4 when
     * evaluating the product.  As it has some overhead, should be used when
     * both numbers are larger than a certain threshold (found
     * experimentally).
     *
     * See:  http://en.wikipedia.org/wiki/Karatsuba_algorithm
     */
    private static BigInteger multiplyKaratsuba(BigInteger x, BigInteger y)
    {
        int xlen = x.mag.length;
        int ylen = y.mag.length;

        // The number of ints in each half of the number.
        int half = (Math.max(xlen, ylen)+1) / 2;

        // xl and yl are the lower halves of x and y respectively,
        // xh and yh are the upper halves.
        BigInteger xl = x.getLower(half);
        BigInteger xh = x.getUpper(half);
        BigInteger yl = y.getLower(half);
        BigInteger yh = y.getUpper(half);

        BigInteger p1 = xh.multiply(yh);  // p1 = xh*yh
        BigInteger p2 = xl.multiply(yl);  // p2 = xl*yl

        // p3=(xh+xl)*(yh+yl)
        BigInteger p3 = xh.add(xl).multiply(yh.add(yl));

        // result = p1 * 2^(32*2*half) + (p3 - p1 - p2) * 2^(32*half) + p2
        BigInteger result = p1.shiftLeft(32*half).add(p3.subtract(p1).subtract(p2)).shiftLeft(32*half).add(p2);

        if (x.signum != y.signum)
            return result.negate();
        else
            return result;
    }

    /**
     * Multiplies two BigIntegers using a 3-way Toom-Cook multiplication
     * algorithm.  This is a recursive divide-and-conquer algorithm which is
     * more efficient for large numbers than what is commonly called the
     * "grade-school" algorithm used in multiplyToLen.  If the numbers to be
     * multiplied have length n, the "grade-school" algorithm has an
     * asymptotic complexity of O(n^2).  In contrast, 3-way Toom-Cook has a
     * complexity of about O(n^1.465).  It achieves this increased asymptotic
     * performance by breaking each number into three parts and by doing 5
     * multiplies instead of 9 when evaluating the product.  Due to overhead
     * (additions, shifts, and one division) in the Toom-Cook algorithm, it
     * should only be used when both numbers are larger than a certain
     * threshold (found experimentally).  This threshold is generally larger
     * than that for Karatsuba multiplication, so this algorithm is generally
     * only used when numbers become significantly larger.
     *
     * The algorithm used is the "optimal" 3-way Toom-Cook algorithm outlined
     * by Marco Bodrato.
     *
     *  See: http://bodrato.it/toom-cook/
     *       http://bodrato.it/papers/#WAIFI2007
     *
     * "Towards Optimal Toom-Cook Multiplication for Univariate and
     * Multivariate Polynomials in Characteristic 2 and 0." by Marco BODRATO;
     * In C.Carlet and B.Sunar, Eds., "WAIFI'07 proceedings", p. 116-133,
     * LNCS #4547. Springer, Madrid, Spain, June 21-22, 2007.
     *
     */
    private static BigInteger multiplyToomCook3(BigInteger a, BigInteger b)
    {
        int alen = a.mag.length;
        int blen = b.mag.length;

        int largest = Math.max(alen, blen);

        // k is the size (in ints) of the lower-order slices.
        int k = (largest+2)/3;   // Equal to ceil(largest/3)

        // r is the size (in ints) of the highest-order slice.
        int r = largest - 2*k;

        // Obtain slices of the numbers. a2 and b2 are the most significant
        // bits of the numbers a and b, and a0 and b0 the least significant.
        BigInteger a0, a1, a2, b0, b1, b2;
        a2 = a.getToomSlice(k, r, 0, largest);
        a1 = a.getToomSlice(k, r, 1, largest);
        a0 = a.getToomSlice(k, r, 2, largest);
        b2 = b.getToomSlice(k, r, 0, largest);
        b1 = b.getToomSlice(k, r, 1, largest);
        b0 = b.getToomSlice(k, r, 2, largest);

        BigInteger v0, v1, v2, vm1, vinf, t1, t2, tm1, da1, db1;

        v0 = a0.multiply(b0);
        da1 = a2.add(a0);
        db1 = b2.add(b0);
        vm1 = da1.subtract(a1).multiply(db1.subtract(b1));
        da1 = da1.add(a1);
        db1 = db1.add(b1);
        v1 = da1.multiply(db1);
        v2 = da1.add(a2).shiftLeft(1).subtract(a0).multiply(
                db1.add(b2).shiftLeft(1).subtract(b0));
        vinf = a2.multiply(b2);

        /* The algorithm requires two divisions by 2 and one by 3.
           All divisions are known to be exact, that is, they do not produce
           remainders, and all results are positive.  The divisions by 2 are
           implemented as right shifts which are relatively efficient, leaving
           only an exact division by 3, which is done by a specialized
           linear-time algorithm. */
        t2 = v2.subtract(vm1).exactDivideBy3();
        tm1 = v1.subtract(vm1).shiftRight(1);
        t1 = v1.subtract(v0);
        t2 = t2.subtract(t1).shiftRight(1);
        t1 = t1.subtract(tm1).subtract(vinf);
        t2 = t2.subtract(vinf.shiftLeft(1));
        tm1 = tm1.subtract(t2);

        // Number of bits to shift left.
        int ss = k*32;

        BigInteger result = vinf.shiftLeft(ss).add(t2).shiftLeft(ss).add(t1).shiftLeft(ss).add(tm1).shiftLeft(ss).add(v0);

        if (a.signum != b.signum)
            return result.negate();
        else
            return result;
    }


    /** Returns a slice of a BigInteger for use in Toom-Cook multiplication.
     @param lowerSize The size of the lower-order bit slices.
     @param upperSize The size of the higher-order bit slices.
     @param slice The index of which slice is requested, which must be a
     number from 0 to size-1.  Slice 0 is the highest-order bits,
     and slice size-1 are the lowest-order bits.
     Slice 0 may be of different size than the other slices.
     @param fullsize The size of the larger integer array, used to align
     slices to the appropriate position when multiplying different-sized
     numbers.
     */
    private BigInteger getToomSlice(int lowerSize, int upperSize, int slice,
                                    int fullsize)
    {
        int start, end, sliceSize, len, offset;

        len = mag.length;
        offset = fullsize - len;

        if (slice == 0)
        {
            start = 0 - offset;
            end = upperSize - 1 - offset;
        }
        else
        {
            start = upperSize + (slice-1)*lowerSize - offset;
            end = start + lowerSize - 1;
        }

        if (start < 0)
            start = 0;
        if (end < 0)
            return ZERO;

        sliceSize = (end-start) + 1;

        if (sliceSize <= 0)
            return ZERO;

        // While performing Toom-Cook, all slices are positive and
        // the sign is adjusted when the final number is composed.
        if (start==0 && sliceSize >= len)
            return this.abs();

        int intSlice[] = new int[sliceSize];
        System.arraycopy(mag, start, intSlice, 0, sliceSize);

        return new BigInteger(trustedStripLeadingZeroInts(intSlice), 1);
    }

    /** Does an exact division (that is, the remainder is known to be zero)
     of the specified number by 3.  This is used in Toom-Cook
     multiplication.  This is an efficient algorithm that runs in linear
     time.  If the argument is not exactly divisible by 3, results are
     undefined.  Note that this is expected to be called with positive
     arguments only. */
    private BigInteger exactDivideBy3()
    {
        int len = mag.length;
        int[] result = new int[len];
        long x, w, q, borrow;
        borrow = 0L;
        for (int i=len-1; i>=0; i--)
        {
            x = (mag[i] & LONG_MASK);
            w = x - borrow;
            if (borrow > x)       // Did we make the number go negative?
                borrow = 1L;
            else
                borrow = 0L;

            // 0xAAAAAAAB is the modular inverse of 3 (mod 2^32).  Thus,
            // the effect of this is to divide by 3 (mod 2^32).
            // This is much faster than division on most architectures.
            q = (w * 0xAAAAAAABL) & LONG_MASK;
            result[i] = (int) q;

            // Now check the borrow. The second check can of course be
            // eliminated if the first fails.
            if (q >= 0x55555556L)
            {
                borrow++;
                if (q >= 0xAAAAAAABL)
                    borrow++;
            }
        }
        result = trustedStripLeadingZeroInts(result);
        return new BigInteger(result, signum);
    }

    /**
     * Returns a new BigInteger representing n lower ints of the number.
     * This is used by Karatsuba multiplication, Karatsuba squaring,
     * and Burnikel-Ziegler division.
     */
    private BigInteger getLower(int n) {
        int len = mag.length;

        if (len <= n)
            return this;

        int lowerInts[] = new int[n];
        System.arraycopy(mag, len-n, lowerInts, 0, n);

        return new BigInteger(trustedStripLeadingZeroInts(lowerInts), 1);
    }

    /**
     * Returns a new BigInteger representing mag.length-n upper
     * ints of the number.  This is used by Karatsuba multiplication,
     * Karatsuba squaring, and Burnikel-Ziegler division.
     */
    private BigInteger getUpper(int n) {
        int len = mag.length;

        if (len <= n)
            return ZERO;

        int upperLen = len - n;
        int upperInts[] = new int[upperLen];
        System.arraycopy(mag, 0, upperInts, 0, upperLen);

        return new BigInteger(trustedStripLeadingZeroInts(upperInts), 1);
    }

    // Schönhage-Strassen

    /**
     * Multiplies two {@link BigInteger}s using the
     * <a href="http://en.wikipedia.org/wiki/Sch%C3%B6nhage%E2%80%93Strassen_algorithm">
     * Schönhage-Strassen algorithm</a> algorithm.
     * @param a
     * @param b
     * @return a <code>BigInteger</code> equal to <code>a.multiply(b)</code>
     */
    public BigInteger multiplySchönhageStrassen(BigInteger a, BigInteger b) {
        // remove any minus signs, multiply, then fix sign
        int signum = a.signum() * b.signum();
        if (a.signum() < 0)
            a = a.negate();
        if (b.signum() < 0)
            b = b.negate();

        // make reverse-order copies of a.mag and b.mag
        int[] aIntArr = reverse(a.mag);
        int[] bIntArr = reverse(b.mag);

        int[] cIntArr = multiplySchönhageStrassen(aIntArr, a.bitLength(), bIntArr, b.bitLength());

        BigInteger c = new BigInteger(1, reverse(cIntArr));
        if (signum < 0)
            c = c.negate();

        return c;
    }

    /**
     * Squares this number using the
     * <a href="http://en.wikipedia.org/wiki/Sch%C3%B6nhage%E2%80%93Strassen_algorithm">
     * Schönhage-Strassen algorithm</a>.
     * @return a <code>BigInteger</code> equal to <code>this.multiply(this)</code>
     */
    private BigInteger squareSchönhageStrassen() {
        int[] aIntArr;

        // remove any minus sign and make a reverse-order copy of a.mag
        if (signum() >= 0)
            aIntArr = reverse(mag);
        else
            aIntArr = reverse(negate().mag);

        int[] cIntArr = squareSchönhageStrassen(aIntArr, bitLength());
        BigInteger c = new BigInteger(1, reverse(cIntArr));

        return c;
    }

    /**
     * This is the core Schönhage-Strassen method. It multiplies two <b>positive</b> numbers of length
     * <code>aBitLen</code> and </code>bBitLen</code> that are represented as int arrays, i.e. in base 2^32.
     * Positive means an int is always interpreted as an unsigned number, regardless of the sign bit.<br/>
     * The arrays must be ordered least significant to most significant, so the least significant digit
     * must be at index 0.
     * <p/>
     * The Schönhage-Strassen algorithm algorithm works as follows:
     * <ol>
     *   <li>Given numbers a and b, split both numbers into pieces of length 2^(n-1) bits.</li>
     *   <li>Take the low n+2 bits of each piece of a, zero-pad them to 3n+5 bits,
     *       and concatenate them to a new number u.</li>
     *   <li>Do the same for b to obtain v.</li>
     *   <li>Calculate all pieces of z' by multiplying u and v (using Schönhage-Strassen or another
     *       algorithm). The product will contain all pieces of a*b mod n+2.</li>
     *   <li>Pad the pieces of a and b from step 1 to 2^(n+1) bits.</li>
     *   <li>Perform a
     *       <a href="http://en.wikipedia.org/wiki/Discrete_Fourier_transform_%28general%29#Number-theoretic_transform">
     *       Discrete Fourier Transform</a> (DFT) on the padded pieces.</li>
     *   <li>Calculate all pieces of z" by multiplying the i-th piece of a by the i-th piece of b.</li>
     *   <li>Perform an Inverse Discrete Fourier Transform (IDFT) on z". z" will contain all pieces of
     *       a*b mod Fn where Fn=2^2^n+1.</li>
     *   <li>Calculate all pieces of z such that each piece is congruent to z' modulo n+2 and congruent to
     *       z" modulo Fn. This is done using the
     *       <a href="http://en.wikipedia.org/wiki/Chinese_remainder_theorem">Chinese remainder theorem</a>.</li>
     *   <li>Calculate c by adding z_i * 2^(i*2^(n-1)) for all i, where z_i is the i-th piece of z.</li>
     *   <li>Return c reduced modulo 2^2^m+1.</li>
     * </ol>
     *
     * References:
     * <ol>
     *   <li><a href="http://en.wikipedia.org/wiki/Sch%C3%B6nhage%E2%80%93Strassen_algorithm">
     *       Wikipedia articla</a>
     *   <li><a href="http://www.scribd.com/doc/68857222/Schnelle-Multiplikation-gro%C3%9Fer-Zahlen">
     *       Arnold Schönhage und Volker Strassen: Schnelle Multiplikation großer Zahlen, Computing 7, 1971,
     *       Springer-Verlag, S. 281–292</a></li>
     *   <li><a href="http://malte-leip.net/beschreibung_ssa.pdf">Eine verständliche Beschreibung des
     *       Schönhage-Strassen-Algorithmus</a></li>
     * </ol>
     * @param a
     * @param aBitLen
     * @param b
     * @param bBitLen
     * @return a*b
     */
    private int[] multiplySchönhageStrassen(int[] a, int aBitLen, int[] b, int bBitLen) {
        // set M to the number of binary digits in a or b, whichever is greater
        int M = Math.max(aBitLen, bBitLen);

        // find the lowest m such that m>=log2(2M)
        int m = 32 - Integer.numberOfLeadingZeros(2*M-1-1);

        int n = m/2 + 1;

        // split a and b into pieces 1<<(n-1) bits long; assume n>=6 so pieces start and end at int boundaries
        boolean even = m%2 == 0;
        int numPieces = even ? 1<<n : 1<<(n+1);
        int pieceSize = 1 << (n-1-5);   // in ints

        // build u and v from a and b, allocating 3n+5 bits in u and v per n+2 bits from a and b, resp.
        int numPiecesA = (a.length+pieceSize) / pieceSize;
        int[] u = new int[(numPiecesA*(3*n+5)+31)/32];
        int uBitLength = 0;
        for (int i=0; i<numPiecesA && i*pieceSize<a.length; i++) {
            appendBits(u, uBitLength, a, i*pieceSize, n+2);
            uBitLength += 3*n+5;
        }
        int numPiecesB = (b.length+pieceSize) / pieceSize;
        int[] v = new int[(numPiecesB*(3*n+5)+31)/32];
        int vBitLength = 0;
        for (int i=0; i<numPiecesB && i*pieceSize<b.length; i++) {
            appendBits(v, vBitLength, b, i*pieceSize, n+2);
            vBitLength += 3*n+5;
        }

        int[] gamma = multReverse(u, v);
        int[][] gammai = splitBits(gamma, 3*n+5);
        int halfNumPcs = numPieces / 2;

        int[][] zi = new int[gammai.length][];
        for (int i=0; i<gammai.length; i++)
            zi[i] = gammai[i];
        for (int i=0; i<gammai.length-halfNumPcs; i++)
            subModPow2(zi[i], gammai[i+halfNumPcs], n+2);
        for (int i=0; i<gammai.length-2*halfNumPcs; i++)
            addModPow2(zi[i], gammai[i+2*halfNumPcs], n+2);
        for (int i=0; i<gammai.length-3*halfNumPcs; i++)
            subModPow2(zi[i], gammai[i+3*halfNumPcs], n+2);

        // zr mod Fn
        int[][] ai = splitInts(a, halfNumPcs, pieceSize, 1<<(n+1-5));
        int[][] bi = splitInts(b, halfNumPcs, pieceSize, 1<<(n+1-5));
        dft(ai, m, n);
        dft(bi, m, n);
        modFn(ai);
        modFn(bi);
        int[][] c = new int[halfNumPcs][];
        for (int i=0; i<c.length; i++)
            c[i] = multModFn(ai[i], bi[i]);
        idft(c, m, n);
        modFn(c);

        int[] z = new int[1<<(m+1-5)];
        // calculate zr mod Fm from zr mod Fn and zr mod 2^(n+2), then add to z
        for (int i=0; i<halfNumPcs; i++) {
            int[] eta = i>=zi.length ? new int[(n+2+31)/32] : zi[i];

            // zi = delta = (zi-c[i]) % 2^(n+2)
            subModPow2(eta, c[i], n+2);

            // z += zr<<shift = [ci + delta*(2^2^n+1)] << [i*2^(n-1)]
            int shift = i*(1<<(n-1-5));   // assume n>=6
            addShifted(z, c[i], shift);
            addShifted(z, eta, shift);
            addShifted(z, eta, shift+(1<<(n-5)));
        }

        modFn(z);   // assume m>=5
        return z;
    }

    /**
     * Squares a <b>positive</b> number of length <code>aBitLen</code> that is represented as an int
     * array, i.e. in base 2^32.
     * @param a
     * @param aBitLen
     * @return a<sup>2</sup>
     * @see #multiplySchönhageStrassen(int[], int, int[], int)
     */
    private int[] squareSchönhageStrassen(int[] a, int aBitLen) {
        // set M to the number of binary digits in a
        int M = aBitLen;

        // find the lowest m such that m>=log2(2M)
        int m = 32 - Integer.numberOfLeadingZeros(2*M-1-1);

        int n = m/2 + 1;

        // split a into pieces 1<<(n-1) bits long; assume n>=6 so pieces start and end at int boundaries
        boolean even = m%2 == 0;
        int numPieces = even ? 1<<n : 1<<(n+1);
        int pieceSize = 1 << (n-1-5);   // in ints

        // build u from a, allocating 3n+5 bits in u per n+2 bits from a
        int numPiecesA = (a.length+pieceSize) / pieceSize;
        int[] u = new int[(numPiecesA*(3*n+5)+31)/32];
        int uBitLength = 0;
        for (int i=0; i<numPiecesA && i*pieceSize<a.length; i++) {
            appendBits(u, uBitLength, a, i*pieceSize, n+2);
            uBitLength += 3*n+5;
        }

        int[] gamma = squareReverse(u);
        int[][] gammai = splitBits(gamma, 3*n+5);
        int halfNumPcs = numPieces / 2;

        int[][] zi = new int[gammai.length][];
        for (int i=0; i<gammai.length; i++)
            zi[i] = gammai[i];
        for (int i=0; i<gammai.length-halfNumPcs; i++)
            subModPow2(zi[i], gammai[i+halfNumPcs], n+2);
        for (int i=0; i<gammai.length-2*halfNumPcs; i++)
            addModPow2(zi[i], gammai[i+2*halfNumPcs], n+2);
        for (int i=0; i<gammai.length-3*halfNumPcs; i++)
            subModPow2(zi[i], gammai[i+3*halfNumPcs], n+2);

        // zr mod Fn
        int[][] ai = splitInts(a, halfNumPcs, pieceSize, 1<<(n+1-5));
        dft(ai, m, n);
        modFn(ai);
        int[][] c = new int[halfNumPcs][];
        for (int i=0; i<c.length; i++)
            c[i] = squareModFn(ai[i]);
        idft(c, m, n);
        modFn(c);

        int[] z = new int[1<<(m+1-5)];
        // calculate zr mod Fm from zr mod Fn and zr mod 2^(n+2), then add to z
        for (int i=0; i<halfNumPcs; i++) {
            int[] eta = i>=zi.length ? new int[(n+2+31)/32] : zi[i];

            // zi = delta = (zi-c[i]) % 2^(n+2)
            subModPow2(eta, c[i], n+2);

            // z += zr<<shift = [ci + delta*(2^2^n+1)] << [i*2^(n-1)]
            int shift = i*(1<<(n-1-5));   // assume n>=6
            addShifted(z, c[i], shift);
            addShifted(z, eta, shift);
            addShifted(z, eta, shift+(1<<(n-5)));
        }

        modFn(z);   // assume m>=5
        return z;
    }

    /**
     * Multiplies two <b>positive</b> numbers represented as int arrays, i.e. in base <code>2^32</code>.
     * Positive means an int is always interpreted as an unsigned number, regardless of the sign bit.<br/>
     * The arrays must be ordered least significant to most significant, so the least significant digit
     * must be at index 0.
     * @param a
     * @param b
     * @return
     */
    private int[] multReverse(int[] a, int[] b) {
        BigInteger aBigInt = new BigInteger(1, reverse(a));
        BigInteger bBigInt = new BigInteger(1, reverse(b));
        return reverse(aBigInt.multiply(bBigInt).mag);
    }

    /**
     * Squares a <b>positive</b> number represented as int arrays, i.e. in base <code>2^32</code>.
     * @param a
     * @return
     * @see #multReverse(int[], int[])
     */
    private int[] squareReverse(int[] a) {
        BigInteger aBigInt = new BigInteger(1, reverse(a));
        return reverse(aBigInt.square().mag);
    }

    /**
     * Estimates whether SS will be more efficient than the other methods when multiplying two numbers
     * of a given length in bits.
     * @param bitLength the number of bits in each of the two factors
     * @return <code>true</code> if SS is more efficient, <code>false</code> if Toom-Cook is more efficient
     */
    private boolean shouldMultiplySchönhageStrassen(int bitLength) {
        // The following values were determined experimentally on a 32-bit JVM.
        // SS is slower than Toom-Cook below ~247,000 bits (~74000 decimal digits)
        // and faster above ~1249000 bits (~376000 decimal digits).
        // Between those values, it changes several times.
        if (bitLength < 247000)
            return false;
        if (bitLength < 262144)   // 2^18
            return true;
        if (bitLength < 422000)
            return false;
        if (bitLength < 524288)   // 2^19
            return true;
        if (bitLength < 701000)
            return false;
        if (bitLength < 1048576)   // 2^20
            return true;
        if (bitLength < 1249000)
            return false;
        return true;
    }

    /**
     * Estimates whether SS will be more efficient than the other methods when squaring a number
     * of a given length in bits.
     * @param bitLength the number of bits in the number to be squared
     * @return <code>true</code> if SS is more efficient, <code>false</code> if Toom-Cook is more efficient
     * @see #shouldMultiplySchönhageStrassen(int)
     */
    private boolean shouldSquareSchönhageStrassen(int bitLength) {
        if (bitLength < 128000)
            return false;
        if (bitLength < 131072)   // 2^17
            return true;
        if (bitLength < 223000)
            return false;
        if (bitLength < 262144)   // 2^18
            return true;
        if (bitLength < 379000)
            return false;
        if (bitLength < 524288)   // 2^19
            return true;
        if (bitLength < 631000)
            return false;
        if (bitLength < 1048576)   // 2^20
            return true;
        if (bitLength < 1120000)
            return false;
        return true;
    }

    /**
     * Performs a
     * <a href="http://en.wikipedia.org/wiki/Discrete_Fourier_transform_%28general%29#Number-theoretic_transform">
     * Fermat Number Transform</a> on an array whose elements are <code>int</code> arrays.<br/>
     * <code>A</code> is assumed to be the lower half of the full array and the upper half is assumed to be all zeros.
     * The number of subarrays in <code>A</code> must be 2^n if m is even and 2^(n+1) if m is odd.<br/>
     * Each subarray must be ceil(2^(n-1)) bits in length.<br/>
     * n must be equal to m/2-1.
     * @param A
     * @param m
     * @param n
     */
    private void dft(int[][] A, int m, int n) {
        boolean even = m%2 == 0;
        int len = A.length;
        int v = 1;

        for (int slen=len/2; slen>0; slen/=2) {   // slen = #consecutive coefficients for which the sign (add/sub) and x are constant
            for (int j=0; j<len; j+=2*slen) {
                int idx = j;
                int x = getDftExponent(n, v, idx+len, even);

                for (int k=slen-1; k>=0; k--) {
                    int[] d = cyclicShiftLeftBits(A[idx+slen], x);
                    System.arraycopy(A[idx], 0, A[idx+slen], 0, A[idx].length);   // copy A[idx] into A[idx+slen]
                    addModFn(A[idx], d);
                    subModFn(A[idx+slen], d, 1<<n);
                    idx++;
                }
            }

            v++;
        }
    }

    /**
     * Returns the power to which to raise omega in a DFT.<br/>
     * Omega itself is either 2 or 4 depending on m, but when omega=4 this method
     * doubles the exponent so omega can be assumed always to be 2 in a DFT.
     * @param n
     * @param v
     * @param idx
     * @param even
     * @return
     */
    private int getDftExponent(int n, int v, int idx, boolean even) {
        // take bits n-v..n-1 of idx, reverse them, shift left by n-v-1
        int x = Integer.reverse(idx) << (n-v) >>> (31-n);

        // if m is even, divide by two
        if (even)
            x >>>= 1;

        return x;
    }

    /**
     * Performs a modified
     * <a href="http://en.wikipedia.org/wiki/Discrete_Fourier_transform_%28general%29#Number-theoretic_transform">
     * Inverse Fermat Number Transform</a> on an array whose elements are <code>int</code> arrays.
     * The modification is that the last step (the one where the upper half is subtracted from the lower half)
     * is omitted.<br/>
     * <code>A</code> is assumed to be the upper half of the full array and the upper half is assumed to be all zeros.
     * The number of subarrays in <code>A</code> must be 2^n if m is even and 2^(n+1) if m is odd.<br/>
     * Each subarray must be ceil(2^(n-1)) bits in length.<br/>
     * n must be equal to m/2-1.
     * @param A
     * @param m
     * @param n
     */
    private void idft(int[][] A, int m, int n) {
        boolean even = m%2 == 0;
        int len = A.length;
        int v = n - 1;
        int[] c = new int[A[0].length];

        for (int slen=1; slen<=len/2; slen*=2) {   // slen = #consecutive coefficients for which the sign (add/sub) and x are constant
            for (int j=0; j<len; j+=2*slen) {
                int idx = j;
                int idx2 = idx + slen;   // idx2 is always idx+slen
                int x = getIdftExponent(n, v, idx, even);

                for (int k=slen-1; k>=0; k--) {
                    System.arraycopy(A[idx], 0, c, 0, c.length);   // copy A[idx] into c
                    addModFn(A[idx], A[idx2]);
                    A[idx] = cyclicShiftRight(A[idx], 1);

                    subModFn(c, A[idx2], 1<<n);
                    A[idx2] = cyclicShiftRight(c, x);
                    idx++;
                    idx2++;
                }
            }

            v--;
        }
    }

    /**
     * Returns the power to which to raise omega in an IDFT.<br/>
     * Omega itself is either 2 or 4 depending on m, but when omega=4 this method
     * doubles the exponent so omega can be assumed always to be 2 in a IDFT.
     * @param n
     * @param v
     * @param idx
     * @param even
     * @return
     */
    private static int getIdftExponent(int n, int v, int idx, boolean even) {
        int x = Integer.reverse(idx) << (n-v) >>> (32-n);
        x += even ? 1<<(n-v) : 1<<(n-1-v);
        return x + 1;
    }

    /**
     * Adds two <b>positive</b> numbers (meaning they are interpreted as unsigned) modulo 2^2^n+1,
     * where n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * Both input values are given as <code>int</code> arrays; they must be the same length.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     * @param b a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     */
    private static void addModFn(int[] a, int[] b) {
        boolean carry = false;
        for (int i=0; i<a.length; i++) {
            int sum = a[i] + b[i];
            if (carry)
                sum++;
            carry = ((sum>>>31) < (a[i]>>>31)+(b[i]>>>31));   // carry if signBit(sum) < signBit(a)+signBit(b)
            a[i] = sum;
        }

        // take a mod Fn by adding any remaining carry bit to the lowest bit;
        // since Fn ≡ 1 (mod 2^n), it suffices to add 1
        int i = 0;
        while (carry) {
            int sum = a[i] + 1;
            a[i] = sum;
            carry = sum == 0;
            i++;
            if (i >= a.length)
                i = 0;
        }
    }

    /**
     * Subtracts two <b>positive</b> numbers (meaning they are interpreted as unsigned) modulo 2^2^n+1,
     * where n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * Both input values are given as <code>int</code> arrays; they must be the same length.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     * @param b a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     */
    private void subModFn(int[] a, int[] b, int pow2n) {
        addModFn(a, cyclicShiftLeftElements(b, pow2n/32));
    }

    /**
     * Multiplies two <b>positive</b> numbers (meaning they are interpreted as unsigned) modulo Fn
     * where Fn=2^2^n+1, and returns the result in a new array.<br/>
     * <code>a</code> and <code>b</code> are assumed to be reduced mod Fn, i.e. 0<=a<Fn and 0<=b<Fn,
     * where n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * Both input values are given as <code>int</code> arrays; they must be the same length.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     * @param b a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     */
    private int[] multModFn(int[] a, int[] b) {
        int[] a0 = Arrays.copyOf(a, a.length/2);
        int[] b0 = Arrays.copyOf(b, b.length/2);
        int[] c = multReverse(a0, b0);
        c = Arrays.copyOf(c, a.length);   // make sure c is the same length as a and b so subModFn uses the right n
        int n = a.length/2;
        // special case: if a=Fn-1, add b*2^2^n which is the same as subtracting b
        if (a[n] == 1)
            subModFn(c, Arrays.copyOf(b0, c.length), n*32);
        if (b[n] == 1)
            subModFn(c, Arrays.copyOf(a0, c.length), n*32);
        return c;
    }

    /**
     * Squares a <b>positive</b> number modulo Fn where Fn=2^2^n+1, and returns the result in a
     * new array.<br/>
     * <code>a</code> is assumed to be reduced mod Fn, i.e. 0<=a<Fn,
     * where n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * The input value are given as an <code>int</code> array.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     */
    private int[] squareModFn(int[] a) {
        int[] a0 = Arrays.copyOf(a, a.length/2);
        int[] c = squareReverse(a0);
        c = Arrays.copyOf(c, a.length);   // make sure c is the same length as a so subModFn uses the right n
        int n = a.length/2;
        // special case: if a=Fn-1, c=1
        if (a[n] == 1)
            c[0] = 1;
        return c;
    }

    private void modFn(int[] a) {
        int len = a.length;
        boolean carry = false;
        for (int i=0; i<len/2; i++) {
            int bi = a[len/2+i];
            int diff = a[i] - bi;
            if (carry)
                diff--;
            carry = ((diff>>>31) > (a[i]>>>31)-(bi>>>31));   // carry if signBit(diff) > signBit(a)-signBit(b)
            a[i] = diff;
        }
        for (int i=len/2; i<len; i++)
            a[i] = 0;
        // if result is negative, add Fn; since Fn ≡ 1 (mod 2^n), it suffices to add 1
        if (carry) {
            int j = 0;
            do {
                int sum = a[j] + 1;
                a[j] = sum;
                carry = sum == 0;
                j++;
                if (j >= a.length)
                    j = 0;
            } while (carry);
        }
    }

    /**
     * Reduces all subarrays modulo 2^2^n+1 where n=<code>a[i].length*32/2</code> for all i;
     * in other words, n is half the number of bits in the subarray.
     * @param a int arrays whose length is a power of 2
     */
    private void modFn(int[][] a) {
        for (int i=0; i<a.length; i++)
            modFn(a[i]);
    }

    /**
     * Cyclicly shifts a number to the right modulo 2^2^n+1 and returns the result in a new array.
     * "Right" means towards the lower array indices and the lower bits; this is equivalent to
     * a multiplication by 2^(-numBits) modulo 2^2^n+1.<br/>
     * The number n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * Both input values are given as <code>int</code> arrays; they must be the same length.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     * @param numBits the shift amount in bits
     * @return the shifted number
     */
    private int[] cyclicShiftRight(int[] a, int numBits) {
        int[] b = new int[a.length];
        int numElements = numBits / 32;
        System.arraycopy(a, numElements, b, 0, a.length-numElements);
        System.arraycopy(a, 0, b, a.length-numElements, numElements);

        numBits = numBits % 32;
        if (numBits != 0) {
            int b0 = b[0];
            b[0] = b[0] >>> numBits;
            for (int i=1; i<b.length; i++) {
                b[i-1] |= b[i] << (32-numBits);
                b[i] = b[i] >>> numBits;
            }
            b[b.length-1] |= b0 << (32-numBits);
        }
        return b;
    }

    /**
     * Shifts a number to the left modulo 2^2^n+1 and returns the result in a new array.
     * "Left" means towards the lower array indices and the lower bits; this is equivalent to
     * a multiplication by 2^numBits modulo 2^2^n+1.<br/>
     * The number n is <code>a.length*32/2</code>; in other words, n is half the number of bits in
     * <code>a</code>.<br/>
     * Both input values are given as <code>int</code> arrays; they must be the same length.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit; the length must be a power of 2
     * @param numBits the shift amount in bits
     * @return the shifted number
     */
    private int[] cyclicShiftLeftBits(int[] a, int numBits) {
        int[] b = cyclicShiftLeftElements(a, numBits/32);

        numBits = numBits % 32;
        if (numBits != 0) {
            int bhi = b[b.length-1];
            b[b.length-1] <<= numBits;
            for (int i=b.length-1; i>0; i--) {
                b[i] |= b[i-1] >>> (32-numBits);
                b[i-1] <<= numBits;
            }
            b[0] |= bhi >>> (32-numBits);
        }
        return b;
    }

    /**
     * Cyclicly shifts an array towards the higher indices by <code>numElements</code>
     * elements and returns the result in a new array.
     * @param a
     * @param numElements
     * @return
     */
    private int[] cyclicShiftLeftElements(int[] a, int numElements) {
        int[] b = new int[a.length];
        System.arraycopy(a, 0, b, numElements, a.length-numElements);
        System.arraycopy(a, a.length-numElements, b, 0, numElements);
        return b;
    }

    /**
     * Adds two numbers, <code>a</code> and <code>b</code>, after shifting <code>b</code> by
     * <code>numElements</code> elements.<br/>
     * Both numbers are given as <code>int</code> arrays and must be <b>positive</b> numbers
     * (meaning they are interpreted as unsigned).</br> The result is returned in the first
     * argument.
     * If any elements of b are shifted outside the valid range for <code>a</code>, they are dropped.
     * @param a a number in base 2^32 starting with the lowest digit
     * @param b a number in base 2^32 starting with the lowest digit
     * @param numElements
     */
    private void addShifted(int[] a, int[] b, int numElements) {
        boolean carry = false;
        int i = 0;
        while (i < Math.min(b.length, a.length-numElements)) {
            int ai = a[i+numElements];
            int sum = ai + b[i];
            if (carry)
                sum++;
            carry = ((sum>>>31) < (ai>>>31)+(b[i]>>>31));   // carry if signBit(sum) < signBit(a)+signBit(b)
            a[i+numElements] = sum;
            i++;
        }
        while (carry) {
            a[i+numElements]++;
            carry = a[i+numElements] == 0;
            i++;
        }
    }

    /**
     * Adds two <b>positive</b> numbers (meaning they are interpreted as unsigned) modulo 2^numBits.
     * Both input values are given as <code>int</code> arrays.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit
     * @param b a number in base 2^32 starting with the lowest digit
     */
    private void addModPow2(int[] a, int[] b, int numBits) {
        int numElements = (numBits+31) / 32;
        boolean carry = false;
        int i;
        for (i=0; i<numElements; i++) {
            int sum = a[i] + b[i];
            if (carry)
                sum++;
            carry = ((sum>>>31) < (a[i]>>>31)+(b[i]>>>31));   // carry if signBit(sum) < signBit(a)+signBit(b)
            a[i] = sum;
        }
        a[i-1] &= -1 >>> (32-(numBits%32));
        for (; i<a.length; i++)
            a[i] = 0;
    }

    /**
     * Subtracts two <b>positive</b> numbers (meaning they are interpreted as unsigned) modulo 2^numBits.
     * Both input values are given as <code>int</code> arrays.
     * The result is returned in the first argument.
     * @param a a number in base 2^32 starting with the lowest digit
     * @param b a number in base 2^32 starting with the lowest digit
     */
    private void subModPow2(int[] a, int[] b, int numBits) {
        int numElements = (numBits+31) / 32;
        boolean carry = false;
        int i;
        for (i=0; i<numElements; i++) {
            int diff = a[i] - b[i];
            if (carry)
                diff--;
            carry = ((diff>>>31) > (a[i]>>>31)-(b[i]>>>31));   // carry if signBit(diff) > signBit(a)-signBit(b)
            a[i] = diff;
        }
        a[i-1] &= -1 >>> (32-(numBits%32));
        for (; i<a.length; i++)
            a[i] = 0;
    }

    /**
     * Reads <code>bBitLength</code> bits from <code>b</code>, starting at array index
     * <code>bStart</code>, and copies them into <code>a</code>, starting at bit
     * <code>aBitLength</code>. The result is returned in <code>a</code>.
     * @param a
     * @param aBitLength
     * @param b
     * @param bStart
     * @param bBitLength
     */
    private void appendBits(int[] a, int aBitLength, int[] b, int bStart, int bBitLength) {
        int aIdx = aBitLength / 32;
        int bit32 = aBitLength % 32;

        for (int i=bStart; i<bStart+bBitLength/32; i++) {
            if (bit32 > 0) {
                a[aIdx] |= b[i] << bit32;
                aIdx++;
                a[aIdx] = b[i] >>> (32-bit32);
            }
            else {
                a[aIdx] = b[i];
                aIdx++;
            }
        }

        if (bBitLength%32 > 0) {
            int bIdx = bBitLength / 32;
            int bi = b[bStart+bIdx];
            bi &= -1 >>> (32-bBitLength);
            a[aIdx] |= bi << bit32;
            if (bit32+(bBitLength%32) > 32)
                a[aIdx+1] = bi >>> (32-bit32);
        }
    }

    /**
     * Divides an <code>int</code> array into pieces <code>bitLength</code> bits long.
     * @param a
     * @param bitLength
     * @return a new array containing <code>bitLength</code> bits from <code>a</code> in each subarray
     */
    private int[][] splitBits(int[] a, int bitLength) {
        int aIntIdx = 0;
        int aBitIdx = 0;
        int numPieces = (a.length*32+bitLength-1) / bitLength;
        int pieceLength = (bitLength+31) / 32;   // in ints
        int[][] b = new int[numPieces][pieceLength];
        for (int i=0; i<b.length; i++) {
            int bitsRemaining = Math.min(bitLength, a.length*32-i*bitLength);
            int bIntIdx = 0;
            int bBitIdx = 0;
            while (bitsRemaining > 0) {
                int bitsToCopy = Math.min(32-aBitIdx, 32-bBitIdx);
                bitsToCopy = Math.min(bitsRemaining, bitsToCopy);
                int mask = a[aIntIdx] >>> aBitIdx;
                mask &= -1 >>> (32-bitsToCopy);
                mask <<= bBitIdx;
                b[i][bIntIdx] |= mask;
                bitsRemaining -= bitsToCopy;
                aBitIdx += bitsToCopy;
                if (aBitIdx >= 32) {
                    aBitIdx -= 32;
                    aIntIdx++;
                }
                bBitIdx += bitsToCopy;
                if (bBitIdx >= 32) {
                    bBitIdx -= 32;
                    bIntIdx++;
                }
            }
        }
        return b;
    }

    /**
     * Splits an <code>int</code> array into pieces of <code>pieceSize ints</code> each, and
     * pads each piece to <code>targetPieceSize ints</code>.
     * @param a the input array
     * @param numPieces the number of pieces to split the array into
     * @param pieceSize the size of each piece in the input array in <code>ints</code>
     * @param targetPieceSize the size of each piece in the output array in <code>ints</code>
     * @return an array of length <code>numPieces</code> containing subarrays of length <code>targetPieceSize</code>
     */
    private int[][] splitInts(int[] a, int numPieces, int pieceSize, int targetPieceSize) {
        int[][] ai = new int[numPieces][targetPieceSize];
        for (int i=0; i<a.length/pieceSize; i++)
            System.arraycopy(a, i*pieceSize, ai[i], 0, pieceSize);
        System.arraycopy(a, a.length/pieceSize*pieceSize, ai[a.length/pieceSize], 0, a.length%pieceSize);
        return ai;
    }

    private int[] reverse(int[] a) {
        int[] b = new int[a.length];
        for (int i=0; i<a.length; i++)
            b[i] = a[a.length-1-i];
        return b;
    }

    // Squaring

    /**
     * Returns a BigInteger whose value is {@code (this<sup>2</sup>)}.
     *
     * @return {@code this<sup>2</sup>}
     */
    private BigInteger square() {
        if (signum == 0)
            return ZERO;
        int len = mag.length;

        if (len < KARATSUBA_SQUARE_THRESHOLD)
        {
            int[] z = squareToLen(mag, len, null);
            return new BigInteger(trustedStripLeadingZeroInts(z), 1);
        }
        else
        if (len < TOOM_COOK_SQUARE_THRESHOLD)
            return squareKaratsuba();
        else
        if (!shouldSquareSchönhageStrassen(len*32))
            return squareToomCook3();
        else
            return squareSchönhageStrassen();
    }

    /**
     * Squares the contents of the int array x. The result is placed into the
     * int array z.  The contents of x are not changed.
     */
    private static final int[] squareToLen(int[] x, int len, int[] z) {
        /*
         * The algorithm used here is adapted from Colin Plumb's C library.
         * Technique: Consider the partial products in the multiplication
         * of "abcde" by itself:
         *
         *               a  b  c  d  e
         *            *  a  b  c  d  e
         *          ==================
         *              ae be ce de ee
         *           ad bd cd dd de
         *        ac bc cc cd ce
         *     ab bb bc bd be
         *  aa ab ac ad ae
         *
         * Note that everything above the main diagonal:
         *              ae be ce de = (abcd) * e
         *           ad bd cd       = (abc) * d
         *        ac bc             = (ab) * c
         *     ab                   = (a) * b
         *
         * is a copy of everything below the main diagonal:
         *                       de
         *                 cd ce
         *           bc bd be
         *     ab ac ad ae
         *
         * Thus, the sum is 2 * (off the diagonal) + diagonal.
         *
         * This is accumulated beginning with the diagonal (which
         * consist of the squares of the digits of the input), which is then
         * divided by two, the off-diagonal added, and multiplied by two
         * again.  The low bit is simply a copy of the low bit of the
         * input, so it doesn't need special care.
         */
        int zlen = len << 1;
        if (z == null || z.length < zlen)
            z = new int[zlen];

        // Store the squares, right shifted one bit (i.e., divided by 2)
        int lastProductLowWord = 0;
        for (int j=0, i=0; j<len; j++) {
            long piece = (x[j] & LONG_MASK);
            long product = piece * piece;
            z[i++] = (lastProductLowWord << 31) | (int)(product >>> 33);
            z[i++] = (int)(product >>> 1);
            lastProductLowWord = (int)product;
        }

        // Add in off-diagonal sums
        for (int i=len, offset=1; i>0; i--, offset+=2) {
            int t = x[i-1];
            t = mulAdd(z, x, offset, i-1, t);
            addOne(z, offset-1, i, t);
        }

        // Shift back up and set low bit
        primitiveLeftShift(z, zlen, 1);
        z[zlen-1] |= x[len-1] & 1;

        return z;
    }

    /**
     * Squares a BigInteger using the Karatsuba squaring algorithm.  It should
     * be used when both numbers are larger than a certain threshold (found
     * experimentally).  It is a recursive divide-and-conquer algorithm that
     * has better asymptotic performance than the algorithm used in
     * squareToLen.
     */
    private BigInteger squareKaratsuba()
    {
        int half = (mag.length+1) / 2;

        BigInteger xl = getLower(half);
        BigInteger xh = getUpper(half);

        BigInteger xhs = xh.square();  // xhs = xh^2
        BigInteger xls = xl.square();  // xls = xl^2

        // xh^2 << 64  +  (((xl+xh)^2 - (xh^2 + xl^2)) << 32) + xl^2
        return xhs.shiftLeft(half*32).add(xl.add(xh).square().subtract(xhs.add(xls))).shiftLeft(half*32).add(xls);
    }

    /**
     * Squares a BigInteger using the 3-way Toom-Cook squaring algorithm.  It
     * should be used when both numbers are larger than a certain threshold
     * (found experimentally).  It is a recursive divide-and-conquer algorithm
     * that has better asymptotic performance than the algorithm used in
     * squareToLen or squareKaratsuba.
     */
    private BigInteger squareToomCook3()
    {
        int len = mag.length;

        // k is the size (in ints) of the lower-order slices.
        int k = (len+2)/3;   // Equal to ceil(largest/3)

        // r is the size (in ints) of the highest-order slice.
        int r = len - 2*k;

        // Obtain slices of the numbers. a2 is the most significant
        // bits of the number, and a0 the least significant.
        BigInteger a0, a1, a2;
        a2 = getToomSlice(k, r, 0, len);
        a1 = getToomSlice(k, r, 1, len);
        a0 = getToomSlice(k, r, 2, len);
        BigInteger v0, v1, v2, vm1, vinf, t1, t2, tm1, da1;

        v0 = a0.square();
        da1 = a2.add(a0);
        vm1 = da1.subtract(a1).square();
        da1 = da1.add(a1);
        v1 = da1.square();
        vinf = a2.square();
        v2 = da1.add(a2).shiftLeft(1).subtract(a0).square();

        /* The algorithm requires two divisions by 2 and one by 3.
           All divisions are known to be exact, that is, they do not produce
           remainders, and all results are positive.  The divisions by 2 are
           implemented as right shifts which are relatively efficient, leaving
           only a division by 3.
           The division by 3 is done by an optimized algorithm for this case.
        */
        t2 = v2.subtract(vm1).exactDivideBy3();
        tm1 = v1.subtract(vm1).shiftRight(1);
        t1 = v1.subtract(v0);
        t2 = t2.subtract(t1).shiftRight(1);
        t1 = t1.subtract(tm1).subtract(vinf);
        t2 = t2.subtract(vinf.shiftLeft(1));
        tm1 = tm1.subtract(t2);

        // Number of bits to shift left.
        int ss = k*32;

        return vinf.shiftLeft(ss).add(t2).shiftLeft(ss).add(t1).shiftLeft(ss).add(tm1).shiftLeft(ss).add(v0);
    }

    /**
     * Returns a BigInteger whose value is {@code (this / val)}.
     *
     * @param  val value by which this BigInteger is to be divided.
     * @return {@code this / val}
     * @throws ArithmeticException if {@code val} is zero.
     */
    public BigInteger divide(BigInteger val) {
        if (mag.length<BURNIKEL_ZIEGLER_THRESHOLD || val.mag.length<BURNIKEL_ZIEGLER_THRESHOLD)
            return divideLong(val);
        else if (!shouldDivideBarrett(mag.length*32) || !shouldDivideBarrett(val.mag.length*32))
            return divideBurnikelZiegler(val);
        else
            return divideBarrett(val);
    }

    /** Long division */
    private BigInteger divideLong(BigInteger val) {
        MutableBigInteger q = new MutableBigInteger(),
                a = new MutableBigInteger(this.mag),
                b = new MutableBigInteger(val.mag);

        a.divide(b, q, false);
        return q.toBigInteger(this.signum * val.signum);
    }

    /**
     * Returns an array of two BigIntegers containing {@code (this / val)}
     * followed by {@code (this % val)}.
     *
     * @param  val value by which this BigInteger is to be divided, and the
     *         remainder computed.
     * @return an array of two BigIntegers: the quotient {@code (this / val)}
     *         is the initial element, and the remainder {@code (this % val)}
     *         is the final element.
     * @throws ArithmeticException if {@code val} is zero.
     */
    public BigInteger[] divideAndRemainder(BigInteger val) {
        if (mag.length<BURNIKEL_ZIEGLER_THRESHOLD || val.mag.length<BURNIKEL_ZIEGLER_THRESHOLD)
            return divideAndRemainderLong(val);
        else if (!shouldDivideBarrett(mag.length*32) || !shouldDivideBarrett(val.mag.length*32))
            return divideAndRemainderBurnikelZiegler(val);
        else
            return divideAndRemainderBarrett(val);
    }

    /**
     * Estimates whether Barrett Division will be more efficient than Burnikel-Ziegler when
     * dividing two numbers of a given length in bits.
     * @param bitLength the number of bits in each of the two inputs
     * @return <code>true</code> if Barrett is more efficient, <code>false</code> if Burnikel-Ziegler is more efficient
     */
    private boolean shouldDivideBarrett(int bitLength) {
        if (bitLength < 3300000)
            return false;
        if (bitLength < 4100000)
            return true;
        if (bitLength < 5900000)
            return false;
        if (bitLength < 8300000)
            return true;
        if (bitLength < 9700000)
            return false;
        if (bitLength < 16000000)
            return true;
        if (bitLength < 19000000)
            return false;
        return true;
    }

    /** Long division */
    private BigInteger[] divideAndRemainderLong(BigInteger val) {
        BigInteger[] result = new BigInteger[2];
        MutableBigInteger q = new MutableBigInteger(),
                a = new MutableBigInteger(this.mag),
                b = new MutableBigInteger(val.mag);
        MutableBigInteger r = a.divide(b, q);
        result[0] = q.toBigInteger(this.signum == val.signum ? 1 : -1);
        result[1] = r.toBigInteger(this.signum);
        return result;
    }

    /**
     * Returns a BigInteger whose value is {@code (this % val)}.
     *
     * @param  val value by which this BigInteger is to be divided, and the
     *         remainder computed.
     * @return {@code this % val}
     * @throws ArithmeticException if {@code val} is zero.
     */
    public BigInteger remainder(BigInteger val) {
        if (mag.length<BURNIKEL_ZIEGLER_THRESHOLD || val.mag.length<BURNIKEL_ZIEGLER_THRESHOLD)
            return remainderLong(val);
        else if (!shouldDivideBarrett(mag.length*32) || !shouldDivideBarrett(val.mag.length*32))
            return remainderBurnikelZiegler(val);
        else
            return remainderBarrett(val);
    }

    /** Long division */
    private BigInteger remainderLong(BigInteger val) {
        MutableBigInteger q = new MutableBigInteger(),
                a = new MutableBigInteger(this.mag),
                b = new MutableBigInteger(val.mag);

        return a.divide(b, q).toBigInteger(this.signum);
    }

    /**
     * Calculates <code>this / val</code> using the Burnikel-Ziegler algorithm.
     * @param  val the divisor
     * @return <code>this / val</code>
     */
    private BigInteger divideBurnikelZiegler(BigInteger val) {
        return divideAndRemainderBurnikelZiegler(val)[0];
    }

    /**
     * Calculates <code>this % val</code> using the Burnikel-Ziegler algorithm.
     * @param val the divisor
     * @return <code>this % val</code>
     */
    private BigInteger remainderBurnikelZiegler(BigInteger val) {
        return divideAndRemainderBurnikelZiegler(val)[1];
    }

    /**
     * Computes <code>this / val</code> and <code>this % val</code> using the
     * Burnikel-Ziegler algorithm.
     * @param val the divisor
     * @return an array containing the quotient and remainder
     */
    private BigInteger[] divideAndRemainderBurnikelZiegler(BigInteger val) {
        BigInteger[] c = divideAndRemainderBurnikelZieglerPositive(abs(), val.abs());

        // fix signs
        if (signum*val.signum < 0)
            c[0] = c[0].negate();
        if (signum < 0)
            c[1] = c[1].negate();
        return c;
    }

    /**
     * Computes <code>a/b</code> and <code>a%b</code> using the
     * <a href="http://cr.yp.to/bib/1998/burnikel.ps"> Burnikel-Ziegler algorithm</a>.
     * This method implements algorithm 3 from pg. 9 of the Burnikel-Ziegler paper.
     * The parameter β is 2^32 so all shifts are multiples of 32 bits.<br/>
     * <code>a</code> and <code>b</code> must be nonnegative.
     * @param a the dividend
     * @param b the divisor
     * @return an array containing the quotient and remainder
     */
    private BigInteger[] divideAndRemainderBurnikelZieglerPositive(BigInteger a, BigInteger b) {
        int r = a.mag.length;
        int s = b.mag.length;

        if (r < s)
            return new BigInteger[] {ZERO, a};
        else {
            // let m = min{2^k | (2^k)*BURNIKEL_ZIEGLER_THRESHOLD > s}
            int m = 1 << (32-Integer.numberOfLeadingZeros(s/BURNIKEL_ZIEGLER_THRESHOLD));

            int j = (s+m-1) / m;      // j = ceil(s/m)
            int n = j * m;            // block length in 32-bit units
            int n32 = 32 * n;         // block length in bits
            int sigma = Math.max(0, n32 - b.bitLength());
            b = b.shiftLeft(sigma);   // shift b so its length is a multiple of n
            a = a.shiftLeft(sigma);   // shift a by the same amount

            // t is the number of blocks needed to accommodate 'a' plus one additional bit
            int t = (a.bitLength()+n32) / n32;
            if (t < 2)
                t = 2;
            BigInteger a1 = a.getBlock(t-1, t, n);   // the most significant block of a
            BigInteger a2 = a.getBlock(t-2, t, n);   // the second to most significant block

            // do schoolbook division on blocks, dividing 2-block numbers by 1-block numbers
            BigInteger z = a1.shiftLeftInts(n).add(a2);   // Z[t-2]
            BigInteger quotient = ZERO;
            BigInteger[] c;
            for (int i=t-2; i>0; i--) {
                c = divide2n1n(z, b);
                z = a.getBlock(i-1, t, n);
                z = z.add(c[1].shiftLeftInts(n));
                quotient = quotient.add(c[0]).shiftLeftInts(n);
            }
            // do the loop one more time for i=0 but leave z unchanged
            c = divide2n1n(z, b);
            quotient = quotient.add(c[0]);

            BigInteger remainder = c[1].shiftRight(sigma);   // a and b were shifted, so shift back
            return new BigInteger[] {quotient, remainder};
        }
    }

    /**
     * Returns a <code>BigInteger</code> containing <code>blockLength</code> ints from
     * <code>this</code> number, starting at <code>index*blockLength</code>.<br/>
     * Used by Burnikel-Ziegler division.
     * @param index the block index
     * @param numBlocks the total number of blocks in <code>this</code> number
     * @param blockLength length of one block in units of 32 bits
     * @return
     */
    private BigInteger getBlock(int index, int numBlocks, int blockLength) {
        int blockStart = index * blockLength;
        if (blockStart >= mag.length)
            return ZERO;

        int blockEnd;
        if (index == numBlocks-1)
            blockEnd = (bitLength()+31) / 32;
        else
            blockEnd = (index+1) * blockLength;
        if (blockEnd > mag.length)
            return ZERO;

        int[] newMag = trustedStripLeadingZeroInts(Arrays.copyOfRange(mag, mag.length-blockEnd, mag.length-blockStart));
        return new BigInteger(newMag, signum);
    }

    /**
     * This method implements algorithm 1 from pg. 4 of the Burnikel-Ziegler paper.
     * It divides a 2n-digit number by a n-digit number.<br/>
     * The parameter β is 2^32 so all shifts are multiples of 32 bits.
     * @param a a nonnegative number such that <code>a.bitLength() <= 2*b.bitLength()</code>
     * @param b a positive number such that <code>b.bitLength()</code> is even
     * @return <code>a/b</code> and <code>a%b</code>
     */
    private BigInteger[] divide2n1n(BigInteger a, BigInteger b) {
        int n = b.mag.length;
        if (n%2!=0 || n<BURNIKEL_ZIEGLER_THRESHOLD)
            return a.divideAndRemainderLong(b);

        // view a as [a1,a2,a3,a4] and divide [a1,a2,a3] by b
        BigInteger[] c1 = divide3n2n(a.shiftRightInts(n/2), b);

        // divide the concatenation of c1[1] and a4 by b
        BigInteger a4 = a.getLower(n/2);
        BigInteger[] c2 = divide3n2n(c1[1].shiftLeftInts(n/2).add(a4), b);

        // quotient = the concatentation of the two above quotients
        return new BigInteger[] {c1[0].shiftLeftInts(n/2).add(c2[0]), c2[1]};
    }

    /**
     * This method implements algorithm 2 from pg. 5 of the Burnikel-Ziegler paper.
     * It divides a 3n-digit number by a 2n-digit number.<br/>
     * The parameter β is 2^32 so all shifts are multiples of 32 bits.<br/>
     * @param a a nonnegative number such that <code>2*a.bitLength() <= 3*b.bitLength()</code>
     * @param b a positive number such that <code>b.bitLength()</code> is even
     * @return <code>a/b</code> and <code>a%b</code>
     */
    private BigInteger[] divide3n2n(BigInteger a, BigInteger b) {
        int n = b.mag.length / 2;   // half the length of b in ints

        // split a in 3 parts of length n or less
        BigInteger a1 = a.shiftRightInts(2*n);
        BigInteger a2 = a.shiftAndTruncate(n);
        BigInteger a3 = a.getLower(n);

        // split a in 2 parts of length n or less
        BigInteger b1 = b.shiftRightInts(n);
        BigInteger b2 = b.getLower(n);

        BigInteger q, r1;
        BigInteger a12 = a1.shiftLeftInts(n).add(a2);   // concatenation of a1 and a2
        if (a1.compareTo(b1) < 0) {
            // q=a12/b1, r=a12%b1
            BigInteger[] c = divide2n1n(a12, b1);
            q = c[0];
            r1 = c[1];
        }
        else {
            // q=β^n-1, r=a12-b1*2^n+b1
            q = ones(n);
            r1 = a12.subtract(b1.shiftLeftInts(n)).add(b1);
        }

        BigInteger d = q.multiply(b2);
        BigInteger r = r1.shiftLeftInts(n).add(a3).subtract(d);   // r = r1*β^n + a3 - d (paper says a4)

        // add b until r>=0
        while (r.signum() < 0) {
            r = r.add(b);
            q = q.subtract(ONE);
        }

        return new BigInteger[] {q, r};
    }

    /**
     * Returns a number equal to <code>this.shiftRightInts(n).getLower(n)</code>.<br/>
     * Used by Burnikel-Ziegler division.
     * @param n a non-negative number
     * @return <code>n</code> bits of <code>this</code> starting at bit <code>n</code>
     */
    private BigInteger shiftAndTruncate(int n) {
        if (mag.length <= n)
            return ZERO;
        if (mag.length <= 2*n) {
            int[] newMag = trustedStripLeadingZeroInts(Arrays.copyOfRange(mag, 0, mag.length-n));
            return new BigInteger(newMag, signum);
        }
        else {
            int[] newMag = trustedStripLeadingZeroInts(Arrays.copyOfRange(mag, mag.length-2*n, mag.length-n));
            return new BigInteger(newMag, signum);
        }
    }

    /** Barrett division */
    private BigInteger divideBarrett(BigInteger val) {
        return divideAndRemainderBarrett(val)[0];
    }

    /** Barrett division */
    private BigInteger remainderBarrett(BigInteger val) {
        return divideAndRemainderBarrett(val)[1];
    }

    /**
     * Computes <code>this/val</code> and <code>this%val</code> using Barrett division.
     * @param val the divisor
     * @return an array containing the quotient and remainder
     */
    private BigInteger[] divideAndRemainderBarrett(BigInteger val) {
        BigInteger[] c = abs().divideAndRemainderBarrettPositive(val.abs());
        if (signum*val.signum < 0)
            c[0] = c[0].negate();
        if (signum < 0)
            c[1] = c[1].negate();
        return c;
    }

    /**
     * Computes <code>this/val</code> and <code>this%val</code> using Barrett division.
     * <code>val</code> must be positive.
     * @param val the divisor
     * @return an array containing the quotient and remainder
     */
    private BigInteger[] divideAndRemainderBarrettPositive(BigInteger val) {
        int m = bitLength();
        int n = val.bitLength();

        if (m < n)
            return new BigInteger[] {ZERO, this};
        else if (m <= 2*n) {
            // this case is handled by Barrett directly
            BigInteger mu = val.inverse(m-n);
            return barrettBase(val, mu);
        }
        else {
            // treat each n-bit piece of a as a digit and do long division by val
            // (which is also n bits), reusing the inverse
            BigInteger mu2n = val.inverse(n);
            int startBit = m / n * n;   // the bit at which the current n-bit piece starts
            BigInteger quotient = ZERO;
            BigInteger remainder = shiftRight(startBit);
            BigInteger mask = ONE.shiftLeft(n).subtract(ONE);   // n ones
            while (startBit > 0) {
                startBit -= n;
                BigInteger ai = shiftRight(startBit).and(mask);
                remainder = remainder.shiftLeft(n).add(ai);
                BigInteger mu = mu2n.shiftRightRounded(2*n-remainder.bitLength());   // mu = 2^(remainder.length-n)/val
                BigInteger[] c = remainder.barrettBase(val, mu);
                quotient = quotient.shiftLeft(n).add(c[0]);
                remainder = c[1];
            }
            return new BigInteger[] {quotient, remainder};
        }
    }

    /**
     * Computes <code>this/b</code> and <code>this%b</code>.
     * The binary representation of <code>b</code> must be at least half as
     * long, and no longer than, the binary representation of <code>a</code>.<br/>
     * This method uses the Barrett algorithm as described in
     * <a href="http://treskal.com/kalle/exjobb/original-report.pdf">
     * Fast Division of Large Integers</a>, pg 17.
     * @param b
     * @param mu 2<sup>n</sup>/b where <code>n</code> is the number of binary digits of <code>this</code>
     * @return an array containing the quotient and remainder
     */
    private BigInteger[] barrettBase(BigInteger b, BigInteger mu) {
        int m = bitLength();
        int n = b.bitLength();

        BigInteger a1 = shiftRight(n-1);
        BigInteger q = a1.multiply(mu).shiftRight(m-n+1);
        BigInteger r = subtract(b.multiply(q));
        while (r.signum()<0 || r.compareTo(b)>=0)
            if (r.signum() < 0) {
                r = r.add(b);
                q = q.subtract(ONE);
            }
            else {
                r = r.subtract(b);
                q = q.add(ONE);
            }
        return new BigInteger[] {q, r};
    }

    /**
     * Computes 2<sup>bitLength()+n</sup>/this.<br/>
     * Uses the
     * <a href="http://en.wikipedia.org/wiki/Division_%28digital%29#Newton.E2.80.93Raphson_division">
     * Newton algorithm</a> as described in
     * <a href="http://treskal.com/kalle/exjobb/original-report.pdf">
     * Fast Division of Large Integers</a>, pg 23.
     * @param n precision in bits
     * @return <code>1/this</code>, shifted to the left by <code>bitLength()+n</code> bits
     */
    private BigInteger inverse(int n) {
        int m = bitLength();
        if (n <= NEWTON_THRESHOLD)
            return ONE.shiftLeft(n*2).divideLong(shiftRightRounded(m-n));

        // let numSteps = ceil(log2(n/NEWTON_THRESHOLD)) and initialize k
        int numSteps = bitLengthForInt((n+NEWTON_THRESHOLD-1)/NEWTON_THRESHOLD);
        int[] k = new int[numSteps];
        int ki = n;
        for (int i=numSteps-1; i>=0; i--) {
            ki = (ki+1) / 2;
            k[i] = ki<NEWTON_THRESHOLD ? NEWTON_THRESHOLD : ki;
        }

        // calculate 1/this truncated to k0 fraction digits
        BigInteger z = ONE.shiftLeft(k[0]*2).divideLong(shiftRightRounded(m-k[0]));   // exp=k0 because exp(this)=m

        for (int i=0; i<numSteps; i++) {
            ki = k[i];
            // the following BigIntegers represent numbers of the form a*2^(-exponent)
            BigInteger s = z.square();   // exponent = 2ki
            BigInteger t = shiftRightRounded(m-2*ki-3);   // exponent = 2ki+3
            BigInteger u = t.multiply(s);   // exponent = 4ki+3 > 2ki+1
            BigInteger w = z.add(z);   // exponent = ki
            w = w.shiftLeft(3*ki+3);   // increase #fraction digits to 4ki+3 to match u
            z = w.subtract(u);   // exponent = 4ki+3
            if (i < numSteps-1)
                z = z.shiftRightRounded(4*ki+3-k[i+1]);   // reduce #fraction digits to k[i+1]
            else
                z = z.shiftRightRounded(4*ki+3-n);   // final step: reduce #fraction digits to n
        }
        return z;
    }

    /**
     * Same as {@link BigInteger#shiftRight(int)} but rounds to the
     * nearest integer.
     * @param n shift distance, in bits.
     * @return ⌊this*2<sup>-n</sup>⌉
     */
    private BigInteger shiftRightRounded(int n) {
        BigInteger b = shiftRight(n);
        if (n>0 && testBit(n-1))
            b = b.add(ONE);
        return b;
    }

    /**
     * Returns a BigInteger whose value is <tt>(this<sup>exponent</sup>)</tt>.
     * Note that {@code exponent} is an integer rather than a BigInteger.
     *
     * @param  exponent exponent to which this BigInteger is to be raised.
     * @return <tt>this<sup>exponent</sup></tt>
     * @throws ArithmeticException {@code exponent} is negative.  (This would
     *         cause the operation to yield a non-integer value.)
     */
    public BigInteger pow(int exponent) {
        if (exponent < 0)
            throw new ArithmeticException("Negative exponent");
        if (signum==0)
            return (exponent==0 ? ONE : this);

        BigInteger partToSquare = this.abs();

        // Factor out powers of two from the base, as the exponentiation of
        // these can be done by left shifts only.
        // The remaining part can then be exponentiated faster.  The
        // powers of two will be multiplied back at the end.
        int powersOfTwo = partToSquare.getLowestSetBit();

        int remainingBits;

        // Factor the powers of two out quickly by shifting right, if needed.
        if (powersOfTwo > 0)
        {
            partToSquare = partToSquare.shiftRight(powersOfTwo);
            remainingBits = partToSquare.bitLength();
            if (remainingBits == 1)  // Nothing left but +/- 1?
                if (signum<0 && (exponent&1)==1)
                    return NEGATIVE_ONE.shiftLeft(powersOfTwo*exponent);
                else
                    return ONE.shiftLeft(powersOfTwo*exponent);
        }
        else
        {
            remainingBits = partToSquare.bitLength();
            if (remainingBits == 1)  // Nothing left but +/- 1?
                if (signum<0 && (exponent&1)==1)
                    return NEGATIVE_ONE;
                else
                    return ONE;
        }

        // This is a quick way to approximate the size of the result,
        // similar to doing log2[n] * exponent.  This will give an upper bound
        // of how big the result can be, and which algorithm to use.
        int scaleFactor = remainingBits * exponent;

        // Use slightly different algorithms for small and large operands.
        // See if the result will safely fit into a long. (Largest 2^63-1)
        if (partToSquare.mag.length==1 && scaleFactor <= 62)
        {
            // Small number algorithm.  Everything fits into a long.
            int newSign = (signum<0 && (exponent&1)==1 ? -1 : 1);
            long result = 1;
            long baseToPow2 = partToSquare.mag[0] & LONG_MASK;

            int workingExponent = exponent;

            // Perform exponentiation using repeated squaring trick
            while (workingExponent != 0) {
                if ((workingExponent & 1)==1)
                    result = result * baseToPow2;

                if ((workingExponent >>>= 1) != 0)
                    baseToPow2 = baseToPow2 * baseToPow2;
            }

            // Multiply back the powers of two (quickly, by shifting left)
            if (powersOfTwo > 0)
            {
                int bitsToShift = powersOfTwo*exponent;
                if (bitsToShift + scaleFactor <= 62) // Fits in long?
                    return valueOf((result << bitsToShift) * newSign);
                else
                    return valueOf(result*newSign).shiftLeft(bitsToShift);
            }
            else
                return valueOf(result*newSign);
        }
        else
        {
            // Large number algorithm.  This is basically identical to
            // the algorithm above, but calls multiply() and square()
            // which may use more efficient algorithms for large numbers.
            BigInteger answer = ONE;

            int workingExponent = exponent;
            // Perform exponentiation using repeated squaring trick
            while (workingExponent != 0) {
                if ((workingExponent & 1)==1)
                    answer = answer.multiply(partToSquare);

                if ((workingExponent >>>= 1) != 0)
                    partToSquare = partToSquare.square();
            }
            // Multiply back the (exponentiated) powers of two (quickly,
            // by shifting left)
            if (powersOfTwo > 0)
                answer = answer.shiftLeft(powersOfTwo*exponent);

            if (signum<0 && (exponent&1)==1)
                return answer.negate();
            else
                return answer;
        }
    }

    /**
     * Returns a BigInteger whose value is the greatest common divisor of
     * {@code abs(this)} and {@code abs(val)}.  Returns 0 if
     * {@code this==0 && val==0}.
     *
     * @param  val value with which the GCD is to be computed.
     * @return {@code GCD(abs(this), abs(val))}
     */
    public BigInteger gcd(BigInteger val) {
        if (val.signum == 0)
            return this.abs();
        else if (this.signum == 0)
            return val.abs();

        MutableBigInteger a = new MutableBigInteger(this);
        MutableBigInteger b = new MutableBigInteger(val);

        MutableBigInteger result = a.hybridGCD(b);

        return result.toBigInteger(1);
    }

    /**
     * Package private method to return bit length for an integer.
     */
    static int bitLengthForInt(int n) {
        return 32 - Integer.numberOfLeadingZeros(n);
    }

    /**
     * Left shift int array a up to len by n bits. Returns the array that
     * results from the shift since space may have to be reallocated.
     */
    private static int[] leftShift(int[] a, int len, int n) {
        int nInts = n >>> 5;
        int nBits = n&0x1F;
        int bitsInHighWord = bitLengthForInt(a[0]);

        // If shift can be done without recopy, do so
        if (n <= (32-bitsInHighWord)) {
            primitiveLeftShift(a, len, nBits);
            return a;
        } else { // Array must be resized
            if (nBits <= (32-bitsInHighWord)) {
                int result[] = new int[nInts+len];
                System.arraycopy(a, 0, result, 0, len);
                primitiveLeftShift(result, result.length, nBits);
                return result;
            } else {
                int result[] = new int[nInts+len+1];
                System.arraycopy(a, 0, result, 0, len);
                primitiveRightShift(result, result.length, 32 - nBits);
                return result;
            }
        }
    }

    // shifts a up to len right n bits assumes no leading zeros, 0<n<32
    static void primitiveRightShift(int[] a, int len, int n) {
        int n2 = 32 - n;
        for (int i=len-1, c=a[i]; i>0; i--) {
            int b = c;
            c = a[i-1];
            a[i] = (c << n2) | (b >>> n);
        }
        a[0] >>>= n;
    }

    // shifts a up to len left n bits assumes no leading zeros, 0<=n<32
    static void primitiveLeftShift(int[] a, int len, int n) {
        if (len == 0 || n == 0)
            return;

        int n2 = 32 - n;
        for (int i=0, c=a[i], m=i+len-1; i<m; i++) {
            int b = c;
            c = a[i+1];
            a[i] = (b << n) | (c >>> n2);
        }
        a[len-1] <<= n;
    }

    /**
     * Calculate bitlength of contents of the first len elements an int array,
     * assuming there are no leading zero ints.
     */
    private static int bitLength(int[] val, int len) {
        if (len == 0)
            return 0;
        return ((len - 1) << 5) + bitLengthForInt(val[0]);
    }

    /**
     * Returns a BigInteger whose value is the absolute value of this
     * BigInteger.
     *
     * @return {@code abs(this)}
     */
    public BigInteger abs() {
        return (signum >= 0 ? this : this.negate());
    }

    /**
     * Returns a BigInteger whose value is {@code (-this)}.
     *
     * @return {@code -this}
     */
    public BigInteger negate() {
        return new BigInteger(this.mag, -this.signum);
    }

    /**
     * Returns the signum function of this BigInteger.
     *
     * @return -1, 0 or 1 as the value of this BigInteger is negative, zero or
     *         positive.
     */
    public int signum() {
        return this.signum;
    }

    // Modular Arithmetic Operations

    /**
     * Returns a BigInteger whose value is {@code (this mod m}).  This method
     * differs from {@code remainder} in that it always returns a
     * <i>non-negative</i> BigInteger.
     *
     * @param  m the modulus.
     * @return {@code this mod m}
     * @throws ArithmeticException {@code m} &le; 0
     * @see    #remainder
     */
    public BigInteger mod(BigInteger m) {
        if (m.signum <= 0)
            throw new ArithmeticException("BigInteger: modulus not positive");

        BigInteger result = this.remainder(m);
        return (result.signum >= 0 ? result : result.add(m));
    }

    /**
     * Returns a BigInteger whose value is
     * <tt>(this<sup>exponent</sup> mod m)</tt>.  (Unlike {@code pow}, this
     * method permits negative exponents.)
     *
     * @param  exponent the exponent.
     * @param  m the modulus.
     * @return <tt>this<sup>exponent</sup> mod m</tt>
     * @throws ArithmeticException {@code m} &le; 0 or the exponent is
     *         negative and this BigInteger is not <i>relatively
     *         prime</i> to {@code m}.
     * @see    #modInverse
     */
    public BigInteger modPow(BigInteger exponent, BigInteger m) {
        if (m.signum <= 0)
            throw new ArithmeticException("BigInteger: modulus not positive");

        // Trivial cases
        if (exponent.signum == 0)
            return (m.equals(ONE) ? ZERO : ONE);

        if (this.equals(ONE))
            return (m.equals(ONE) ? ZERO : ONE);

        if (this.equals(ZERO) && exponent.signum >= 0)
            return ZERO;

        if (this.equals(negConst[1]) && (!exponent.testBit(0)))
            return (m.equals(ONE) ? ZERO : ONE);

        boolean invertResult;
        if ((invertResult = (exponent.signum < 0)))
            exponent = exponent.negate();

        BigInteger base = (this.signum < 0 || this.compareTo(m) >= 0
                ? this.mod(m) : this);
        BigInteger result;
        if (m.testBit(0)) { // odd modulus
            result = base.oddModPow(exponent, m);
        } else {
            /*
             * Even modulus.  Tear it into an "odd part" (m1) and power of two
             * (m2), exponentiate mod m1, manually exponentiate mod m2, and
             * use Chinese Remainder Theorem to combine results.
             */

            // Tear m apart into odd part (m1) and power of 2 (m2)
            int p = m.getLowestSetBit();   // Max pow of 2 that divides m

            BigInteger m1 = m.shiftRight(p);  // m/2**p
            BigInteger m2 = ONE.shiftLeft(p); // 2**p

            // Calculate new base from m1
            BigInteger base2 = (this.signum < 0 || this.compareTo(m1) >= 0
                    ? this.mod(m1) : this);

            // Caculate (base ** exponent) mod m1.
            BigInteger a1 = (m1.equals(ONE) ? ZERO :
                    base2.oddModPow(exponent, m1));

            // Calculate (this ** exponent) mod m2
            BigInteger a2 = base.modPow2(exponent, p);

            // Combine results using Chinese Remainder Theorem
            BigInteger y1 = m2.modInverse(m1);
            BigInteger y2 = m1.modInverse(m2);

            result = a1.multiply(m2).multiply(y1).add
                    (a2.multiply(m1).multiply(y2)).mod(m);
        }

        return (invertResult ? result.modInverse(m) : result);
    }

    static int[] bnExpModThreshTable = {7, 25, 81, 241, 673, 1793,
            Integer.MAX_VALUE}; // Sentinel

    /**
     * Returns a BigInteger whose value is x to the power of y mod z.
     * Assumes: z is odd && x < z.
     */
    private BigInteger oddModPow(BigInteger y, BigInteger z) {
    /*
     * The algorithm is adapted from Colin Plumb's C library.
     *
     * The window algorithm:
     * The idea is to keep a running product of b1 = n^(high-order bits of exp)
     * and then keep appending exponent bits to it.  The following patterns
     * apply to a 3-bit window (k = 3):
     * To append   0: square
     * To append   1: square, multiply by n^1
     * To append  10: square, multiply by n^1, square
     * To append  11: square, square, multiply by n^3
     * To append 100: square, multiply by n^1, square, square
     * To append 101: square, square, square, multiply by n^5
     * To append 110: square, square, multiply by n^3, square
     * To append 111: square, square, square, multiply by n^7
     *
     * Since each pattern involves only one multiply, the longer the pattern
     * the better, except that a 0 (no multiplies) can be appended directly.
     * We precompute a table of odd powers of n, up to 2^k, and can then
     * multiply k bits of exponent at a time.  Actually, assuming random
     * exponents, there is on average one zero bit between needs to
     * multiply (1/2 of the time there's none, 1/4 of the time there's 1,
     * 1/8 of the time, there's 2, 1/32 of the time, there's 3, etc.), so
     * you have to do one multiply per k+1 bits of exponent.
     *
     * The loop walks down the exponent, squaring the result buffer as
     * it goes.  There is a wbits+1 bit lookahead buffer, buf, that is
     * filled with the upcoming exponent bits.  (What is read after the
     * end of the exponent is unimportant, but it is filled with zero here.)
     * When the most-significant bit of this buffer becomes set, i.e.
     * (buf & tblmask) != 0, we have to decide what pattern to multiply
     * by, and when to do it.  We decide, remember to do it in future
     * after a suitable number of squarings have passed (e.g. a pattern
     * of "100" in the buffer requires that we multiply by n^1 immediately;
     * a pattern of "110" calls for multiplying by n^3 after one more
     * squaring), clear the buffer, and continue.
     *
     * When we start, there is one more optimization: the result buffer
     * is implcitly one, so squaring it or multiplying by it can be
     * optimized away.  Further, if we start with a pattern like "100"
     * in the lookahead window, rather than placing n into the buffer
     * and then starting to square it, we have already computed n^2
     * to compute the odd-powers table, so we can place that into
     * the buffer and save a squaring.
     *
     * This means that if you have a k-bit window, to compute n^z,
     * where z is the high k bits of the exponent, 1/2 of the time
     * it requires no squarings.  1/4 of the time, it requires 1
     * squaring, ... 1/2^(k-1) of the time, it reqires k-2 squarings.
     * And the remaining 1/2^(k-1) of the time, the top k bits are a
     * 1 followed by k-1 0 bits, so it again only requires k-2
     * squarings, not k-1.  The average of these is 1.  Add that
     * to the one squaring we have to do to compute the table,
     * and you'll see that a k-bit window saves k-2 squarings
     * as well as reducing the multiplies.  (It actually doesn't
     * hurt in the case k = 1, either.)
     */
        // Special case for exponent of one
        if (y.equals(ONE))
            return this;

        // Special case for base of zero
        if (signum==0)
            return ZERO;

        int[] base = mag.clone();
        int[] exp = y.mag;
        int[] mod = z.mag;
        int modLen = mod.length;

        // Select an appropriate window size
        int wbits = 0;
        int ebits = bitLength(exp, exp.length);
        // if exponent is 65537 (0x10001), use minimum window size
        if ((ebits != 17) || (exp[0] != 65537)) {
            while (ebits > bnExpModThreshTable[wbits]) {
                wbits++;
            }
        }

        // Calculate appropriate table size
        int tblmask = 1 << wbits;

        // Allocate table for precomputed odd powers of base in Montgomery form
        int[][] table = new int[tblmask][];
        for (int i=0; i<tblmask; i++)
            table[i] = new int[modLen];

        // Compute the modular inverse
        int inv = -MutableBigInteger.inverseMod32(mod[modLen-1]);

        // Convert base to Montgomery form
        int[] a = leftShift(base, base.length, modLen << 5);

        MutableBigInteger q = new MutableBigInteger(),
                a2 = new MutableBigInteger(a),
                b2 = new MutableBigInteger(mod);

        MutableBigInteger r= a2.divide(b2, q);
        table[0] = r.toIntArray();

        // Pad table[0] with leading zeros so its length is at least modLen
        if (table[0].length < modLen) {
            int offset = modLen - table[0].length;
            int[] t2 = new int[modLen];
            for (int i=0; i<table[0].length; i++)
                t2[i+offset] = table[0][i];
            table[0] = t2;
        }

        // Set b to the square of the base
        int[] b = squareToLen(table[0], modLen, null);
        b = montReduce(b, mod, modLen, inv);

        // Set t to high half of b
        int[] t = Arrays.copyOf(b, modLen);

        // Fill in the table with odd powers of the base
        for (int i=1; i<tblmask; i++) {
            int[] prod = multiplyToLen(t, modLen, table[i-1], modLen, null);
            table[i] = montReduce(prod, mod, modLen, inv);
        }

        // Pre load the window that slides over the exponent
        int bitpos = 1 << ((ebits-1) & (32-1));

        int buf = 0;
        int elen = exp.length;
        int eIndex = 0;
        for (int i = 0; i <= wbits; i++) {
            buf = (buf << 1) | (((exp[eIndex] & bitpos) != 0)?1:0);
            bitpos >>>= 1;
            if (bitpos == 0) {
                eIndex++;
                bitpos = 1 << (32-1);
                elen--;
            }
        }

        int multpos = ebits;

        // The first iteration, which is hoisted out of the main loop
        ebits--;
        boolean isone = true;

        multpos = ebits - wbits;
        while ((buf & 1) == 0) {
            buf >>>= 1;
            multpos++;
        }

        int[] mult = table[buf >>> 1];

        buf = 0;
        if (multpos == ebits)
            isone = false;

        // The main loop
        while(true) {
            ebits--;
            // Advance the window
            buf <<= 1;

            if (elen != 0) {
                buf |= ((exp[eIndex] & bitpos) != 0) ? 1 : 0;
                bitpos >>>= 1;
                if (bitpos == 0) {
                    eIndex++;
                    bitpos = 1 << (32-1);
                    elen--;
                }
            }

            // Examine the window for pending multiplies
            if ((buf & tblmask) != 0) {
                multpos = ebits - wbits;
                while ((buf & 1) == 0) {
                    buf >>>= 1;
                    multpos++;
                }
                mult = table[buf >>> 1];
                buf = 0;
            }

            // Perform multiply
            if (ebits == multpos) {
                if (isone) {
                    b = mult.clone();
                    isone = false;
                } else {
                    t = b;
                    a = multiplyToLen(t, modLen, mult, modLen, a);
                    a = montReduce(a, mod, modLen, inv);
                    t = a; a = b; b = t;
                }
            }

            // Check if done
            if (ebits == 0)
                break;

            // Square the input
            if (!isone) {
                t = b;
                a = squareToLen(t, modLen, a);
                a = montReduce(a, mod, modLen, inv);
                t = a; a = b; b = t;
            }
        }

        // Convert result out of Montgomery form and return
        int[] t2 = new int[2*modLen];
        System.arraycopy(b, 0, t2, modLen, modLen);

        b = montReduce(t2, mod, modLen, inv);

        t2 = Arrays.copyOf(b, modLen);

        return new BigInteger(1, t2);
    }

    /**
     * Montgomery reduce n, modulo mod.  This reduces modulo mod and divides
     * by 2^(32*mlen). Adapted from Colin Plumb's C library.
     */
    private static int[] montReduce(int[] n, int[] mod, int mlen, int inv) {
        int c=0;
        int len = mlen;
        int offset=0;

        do {
            int nEnd = n[n.length-1-offset];
            int carry = mulAdd(n, mod, offset, mlen, inv * nEnd);
            c += addOne(n, offset, mlen, carry);
            offset++;
        } while(--len > 0);

        while(c>0)
            c += subN(n, mod, mlen);

        while (intArrayCmpToLen(n, mod, mlen) >= 0)
            subN(n, mod, mlen);

        return n;
    }


    /*
     * Returns -1, 0 or +1 as big-endian unsigned int array arg1 is less than,
     * equal to, or greater than arg2 up to length len.
     */
    private static int intArrayCmpToLen(int[] arg1, int[] arg2, int len) {
        for (int i=0; i<len; i++) {
            long b1 = arg1[i] & LONG_MASK;
            long b2 = arg2[i] & LONG_MASK;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    /**
     * Subtracts two numbers of same length, returning borrow.
     */
    private static int subN(int[] a, int[] b, int len) {
        long sum = 0;

        while(--len >= 0) {
            sum = (a[len] & LONG_MASK) -
                    (b[len] & LONG_MASK) + (sum >> 32);
            a[len] = (int)sum;
        }

        return (int)(sum >> 32);
    }

    /**
     * Multiply an array by one word k and add to result, return the carry
     */
    static int mulAdd(int[] out, int[] in, int offset, int len, int k) {
        long kLong = k & LONG_MASK;
        long carry = 0;

        offset = out.length-offset - 1;
        for (int j=len-1; j >= 0; j--) {
            long product = (in[j] & LONG_MASK) * kLong +
                    (out[offset] & LONG_MASK) + carry;
            out[offset--] = (int)product;
            carry = product >>> 32;
        }
        return (int)carry;
    }

    /**
     * Add one word to the number a mlen words into a. Return the resulting
     * carry.
     */
    static int addOne(int[] a, int offset, int mlen, int carry) {
        offset = a.length-1-mlen-offset;
        long t = (a[offset] & LONG_MASK) + (carry & LONG_MASK);

        a[offset] = (int)t;
        if ((t >>> 32) == 0)
            return 0;
        while (--mlen >= 0) {
            if (--offset < 0) { // Carry out of number
                return 1;
            } else {
                a[offset]++;
                if (a[offset] != 0)
                    return 0;
            }
        }
        return 1;
    }

    /**
     * Returns a BigInteger whose value is (this ** exponent) mod (2**p)
     */
    private BigInteger modPow2(BigInteger exponent, int p) {
        /*
         * Perform exponentiation using repeated squaring trick, chopping off
         * high order bits as indicated by modulus.
         */
        BigInteger result = ONE;
        BigInteger baseToPow2 = this.mod2(p);
        int expOffset = 0;

        int limit = exponent.bitLength();

        if (this.testBit(0))
            limit = (p-1) < limit ? (p-1) : limit;

        while (expOffset < limit) {
            if (exponent.testBit(expOffset))
                result = result.multiply(baseToPow2).mod2(p);
            expOffset++;
            if (expOffset < limit)
                baseToPow2 = baseToPow2.square().mod2(p);
        }

        return result;
    }

    /**
     * Returns a BigInteger whose value is this mod(2**p).
     * Assumes that this {@code BigInteger >= 0} and {@code p > 0}.
     */
    private BigInteger mod2(int p) {
        if (bitLength() <= p)
            return this;

        // Copy remaining ints of mag
        int numInts = (p + 31) >>> 5;
        int[] mag = new int[numInts];
        System.arraycopy(this.mag, (this.mag.length - numInts), mag, 0, numInts);

        // Mask out any excess bits
        int excessBits = (numInts << 5) - p;
        mag[0] &= (1L << (32-excessBits)) - 1;

        return (mag[0]==0 ? new BigInteger(1, mag) : new BigInteger(mag, 1));
    }

    /**
     * Returns a BigInteger whose value is {@code (this}<sup>-1</sup> {@code mod m)}.
     *
     * @param  m the modulus.
     * @return {@code this}<sup>-1</sup> {@code mod m}.
     * @throws ArithmeticException {@code  m} &le; 0, or this BigInteger
     *         has no multiplicative inverse mod m (that is, this BigInteger
     *         is not <i>relatively prime</i> to m).
     */
    public BigInteger modInverse(BigInteger m) {
        if (m.signum != 1)
            throw new ArithmeticException("BigInteger: modulus not positive");

        if (m.equals(ONE))
            return ZERO;

        // Calculate (this mod m)
        BigInteger modVal = this;
        if (signum < 0 || (this.compareMagnitude(m) >= 0))
            modVal = this.mod(m);

        if (modVal.equals(ONE))
            return ONE;

        MutableBigInteger a = new MutableBigInteger(modVal);
        MutableBigInteger b = new MutableBigInteger(m);

        MutableBigInteger result = a.mutableModInverse(b);
        return result.toBigInteger(1);
    }

    // Shift Operations

    /**
     * Returns a BigInteger whose value is {@code (this << n)}.
     * The shift distance, {@code n}, may be negative, in which case
     * this method performs a right shift.
     * (Computes <tt>floor(this * 2<sup>n</sup>)</tt>.)
     *
     * @param  n shift distance, in bits.
     * @return {@code this << n}
     * @throws ArithmeticException if the shift distance is {@code
     *         Integer.MIN_VALUE}.
     * @see #shiftRight
     */
    public BigInteger shiftLeft(int n) {
        if (signum == 0)
            return ZERO;
        if (n==0)
            return this;
        if (n<0) {
            if (n == Integer.MIN_VALUE) {
                throw new ArithmeticException("Shift distance of Integer.MIN_VALUE not supported.");
            } else {
                return shiftRight(-n);
            }
        }
        int[] newMag = shiftLeft(mag, n);

        return new BigInteger(newMag, signum);
    }

    private static int[] shiftLeft(int[] mag, int n) {
        int nInts = n >>> 5;
        int nBits = n & 0x1f;
        int magLen = mag.length;
        int newMag[] = null;

        if (nBits == 0) {
            newMag = new int[magLen + nInts];
            System.arraycopy(mag, 0, newMag, 0, magLen);
        } else {
            int i = 0;
            int nBits2 = 32 - nBits;
            int highBits = mag[0] >>> nBits2;
            if (highBits != 0) {
                newMag = new int[magLen + nInts + 1];
                newMag[i++] = highBits;
            } else {
                newMag = new int[magLen + nInts];
            }
            int j=0;
            while (j < magLen-1)
                newMag[i++] = mag[j++] << nBits | mag[j] >>> nBits2;
            newMag[i] = mag[j] << nBits;
        }
        return newMag;
    }

    /**
     * Returns a BigInteger whose value is {@code (this >> n)}.  Sign
     * extension is performed.  The shift distance, {@code n}, may be
     * negative, in which case this method performs a left shift.
     * (Computes <tt>floor(this / 2<sup>n</sup>)</tt>.)
     *
     * @param  n shift distance, in bits.
     * @return {@code this >> n}
     * @throws ArithmeticException if the shift distance is {@code
     *         Integer.MIN_VALUE}.
     * @see #shiftLeft
     */
    public BigInteger shiftRight(int n) {
        if (n==0)
            return this;
        if (n<0) {
            if (n == Integer.MIN_VALUE) {
                throw new ArithmeticException("Shift distance of Integer.MIN_VALUE not supported.");
            } else {
                return shiftLeft(-n);
            }
        }

        int nInts = n >>> 5;
        int nBits = n & 0x1f;
        int magLen = mag.length;
        int newMag[] = null;

        // Special case: entire contents shifted off the end
        if (nInts >= magLen)
            return (signum >= 0 ? ZERO : negConst[1]);

        if (nBits == 0) {
            int newMagLen = magLen - nInts;
            newMag = Arrays.copyOf(mag, newMagLen);
        } else {
            int i = 0;
            int highBits = mag[0] >>> nBits;
            if (highBits != 0) {
                newMag = new int[magLen - nInts];
                newMag[i++] = highBits;
            } else {
                newMag = new int[magLen - nInts -1];
            }

            int nBits2 = 32 - nBits;
            int j=0;
            while (j < magLen - nInts - 1)
                newMag[i++] = (mag[j++] << nBits2) | (mag[j] >>> nBits);
        }

        if (signum < 0) {
            // Find out whether any one-bits were shifted off the end.
            boolean onesLost = false;
            for (int i=magLen-1, j=magLen-nInts; i>=j && !onesLost; i--)
                onesLost = (mag[i] != 0);
            if (!onesLost && nBits != 0)
                onesLost = (mag[magLen - nInts - 1] << (32 - nBits) != 0);

            if (onesLost)
                newMag = javaIncrement(newMag);
        }

        return new BigInteger(newMag, signum);
    }

    int[] javaIncrement(int[] val) {
        int lastSum = 0;
        for (int i=val.length-1;  i >= 0 && lastSum == 0; i--)
            lastSum = (val[i] += 1);
        if (lastSum == 0) {
            val = new int[val.length+1];
            val[0] = 1;
        }
        return val;
    }

    /**
     * Shifts a number to the left by a multiple of 32.
     * @param n a non-negative number
     * @return <code>this.shiftLeft(32*n)</code>
     */
    private BigInteger shiftLeftInts(int n) {
        int[] newMag = trustedStripLeadingZeroInts(Arrays.copyOf(mag, mag.length+n));
        return new BigInteger(newMag, signum);
    }

    /**
     * Shifts a number to the right by a multiple of 32.
     * @param n a non-negative number
     * @return <code>this.shiftRight(32*n)</code>
     */
    private BigInteger shiftRightInts(int n) {
        if (n >= mag.length)
            return ZERO;
        else
            return new BigInteger(Arrays.copyOf(mag, mag.length-n), signum);
    }

    // Bitwise Operations

    /**
     * Returns a BigInteger whose value is {@code (this & val)}.  (This
     * method returns a negative BigInteger if and only if this and val are
     * both negative.)
     *
     * @param val value to be AND'ed with this BigInteger.
     * @return {@code this & val}
     */
    public BigInteger and(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i=0; i<result.length; i++)
            result[i] = (getInt(result.length-i-1)
                    & val.getInt(result.length-i-1));

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is {@code (this | val)}.  (This method
     * returns a negative BigInteger if and only if either this or val is
     * negative.)
     *
     * @param val value to be OR'ed with this BigInteger.
     * @return {@code this | val}
     */
    public BigInteger or(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i=0; i<result.length; i++)
            result[i] = (getInt(result.length-i-1)
                    | val.getInt(result.length-i-1));

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is {@code (this ^ val)}.  (This method
     * returns a negative BigInteger if and only if exactly one of this and
     * val are negative.)
     *
     * @param val value to be XOR'ed with this BigInteger.
     * @return {@code this ^ val}
     */
    public BigInteger xor(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i=0; i<result.length; i++)
            result[i] = (getInt(result.length-i-1)
                    ^ val.getInt(result.length-i-1));

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is {@code (~this)}.  (This method
     * returns a negative value if and only if this BigInteger is
     * non-negative.)
     *
     * @return {@code ~this}
     */
    public BigInteger not() {
        int[] result = new int[intLength()];
        for (int i=0; i<result.length; i++)
            result[i] = ~getInt(result.length-i-1);

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is {@code (this & ~val)}.  This
     * method, which is equivalent to {@code and(val.not())}, is provided as
     * a convenience for masking operations.  (This method returns a negative
     * BigInteger if and only if {@code this} is negative and {@code val} is
     * positive.)
     *
     * @param val value to be complemented and AND'ed with this BigInteger.
     * @return {@code this & ~val}
     */
    public BigInteger andNot(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i=0; i<result.length; i++)
            result[i] = (getInt(result.length-i-1)
                    & ~val.getInt(result.length-i-1));

        return valueOf(result);
    }


    // Single Bit Operations

    /**
     * Returns {@code true} if and only if the designated bit is set.
     * (Computes {@code ((this & (1<<n)) != 0)}.)
     *
     * @param  n index of bit to test.
     * @return {@code true} if and only if the designated bit is set.
     * @throws ArithmeticException {@code n} is negative.
     */
    public boolean testBit(int n) {
        if (n<0)
            throw new ArithmeticException("Negative bit address");

        return (getInt(n >>> 5) & (1 << (n & 31))) != 0;
    }

    /**
     * Returns a BigInteger whose value is equivalent to this BigInteger
     * with the designated bit set.  (Computes {@code (this | (1<<n))}.)
     *
     * @param  n index of bit to set.
     * @return {@code this | (1<<n)}
     * @throws ArithmeticException {@code n} is negative.
     */
    public BigInteger setBit(int n) {
        if (n<0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), intNum+2)];

        for (int i=0; i<result.length; i++)
            result[result.length-i-1] = getInt(i);

        result[result.length-intNum-1] |= (1 << (n & 31));

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is equivalent to this BigInteger
     * with the designated bit cleared.
     * (Computes {@code (this & ~(1<<n))}.)
     *
     * @param  n index of bit to clear.
     * @return {@code this & ~(1<<n)}
     * @throws ArithmeticException {@code n} is negative.
     */
    public BigInteger clearBit(int n) {
        if (n<0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), ((n + 1) >>> 5) + 1)];

        for (int i=0; i<result.length; i++)
            result[result.length-i-1] = getInt(i);

        result[result.length-intNum-1] &= ~(1 << (n & 31));

        return valueOf(result);
    }

    /**
     * Returns a BigInteger whose value is equivalent to this BigInteger
     * with the designated bit flipped.
     * (Computes {@code (this ^ (1<<n))}.)
     *
     * @param  n index of bit to flip.
     * @return {@code this ^ (1<<n)}
     * @throws ArithmeticException {@code n} is negative.
     */
    public BigInteger flipBit(int n) {
        if (n<0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), intNum+2)];

        for (int i=0; i<result.length; i++)
            result[result.length-i-1] = getInt(i);

        result[result.length-intNum-1] ^= (1 << (n & 31));

        return valueOf(result);
    }

    /**
     * Returns the index of the rightmost (lowest-order) one bit in this
     * BigInteger (the number of zero bits to the right of the rightmost
     * one bit).  Returns -1 if this BigInteger contains no one bits.
     * (Computes {@code (this==0? -1 : log2(this & -this))}.)
     *
     * @return index of the rightmost one bit in this BigInteger.
     */
    public int getLowestSetBit() {
        @SuppressWarnings("deprecation") int lsb = lowestSetBit - 2;
        if (lsb == -2) {  // lowestSetBit not initialized yet
            lsb = 0;
            if (signum == 0) {
                lsb -= 1;
            } else {
                // Search for lowest order nonzero int
                int i,b;
                for (i=0; (b = getInt(i))==0; i++)
                    ;
                lsb += (i << 5) + Integer.numberOfTrailingZeros(b);
            }
            lowestSetBit = lsb + 2;
        }
        return lsb;
    }


    // Miscellaneous Bit Operations

    /**
     * Returns the number of bits in the minimal two's-complement
     * representation of this BigInteger, <i>excluding</i> a sign bit.
     * For positive BigIntegers, this is equivalent to the number of bits in
     * the ordinary binary representation.  (Computes
     * {@code (ceil(log2(this < 0 ? -this : this+1)))}.)
     *
     * @return number of bits in the minimal two's-complement
     *         representation of this BigInteger, <i>excluding</i> a sign bit.
     */
    public int bitLength() {
        @SuppressWarnings("deprecation") int n = bitLength - 1;
        if (n == -1) { // bitLength not initialized yet
            int[] m = mag;
            int len = m.length;
            if (len == 0) {
                n = 0; // offset by one to initialize
            }  else {
                // Calculate the bit length of the magnitude
                int magBitLength = ((len - 1) << 5) + bitLengthForInt(mag[0]);
                if (signum < 0) {
                    // Check if magnitude is a power of two
                    boolean pow2 = (Integer.bitCount(mag[0]) == 1);
                    for (int i=1; i< len && pow2; i++)
                        pow2 = (mag[i] == 0);

                    n = (pow2 ? magBitLength -1 : magBitLength);
                } else {
                    n = magBitLength;
                }
            }
            bitLength = n + 1;
        }
        return n;
    }

    /**
     * Returns the number of bits in the two's complement representation
     * of this BigInteger that differ from its sign bit.  This method is
     * useful when implementing bit-vector style sets atop BigIntegers.
     *
     * @return number of bits in the two's complement representation
     *         of this BigInteger that differ from its sign bit.
     */
    public int bitCount() {
        @SuppressWarnings("deprecation") int bc = bitCount - 1;
        if (bc == -1) {  // bitCount not initialized yet
            bc = 0;      // offset by one to initialize
            // Count the bits in the magnitude
            for (int i=0; i<mag.length; i++)
                bc += Integer.bitCount(mag[i]);
            if (signum < 0) {
                // Count the trailing zeros in the magnitude
                int magTrailingZeroCount = 0, j;
                for (j=mag.length-1; mag[j]==0; j--)
                    magTrailingZeroCount += 32;
                magTrailingZeroCount += Integer.numberOfTrailingZeros(mag[j]);
                bc += magTrailingZeroCount - 1;
            }
            bitCount = bc + 1;
        }
        return bc;
    }

    // Primality Testing

    /**
     * Returns {@code true} if this BigInteger is probably prime,
     * {@code false} if it's definitely composite.  If
     * {@code certainty} is &le; 0, {@code true} is
     * returned.
     *
     * @param  certainty a measure of the uncertainty that the caller is
     *         willing to tolerate: if the call returns {@code true}
     *         the probability that this BigInteger is prime exceeds
     *         (1 - 1/2<sup>{@code certainty}</sup>).  The execution time of
     *         this method is proportional to the value of this parameter.
     * @return {@code true} if this BigInteger is probably prime,
     *         {@code false} if it's definitely composite.
     */
    public boolean isProbablePrime(int certainty) {
        if (certainty <= 0)
            return true;
        BigInteger w = this.abs();
        if (w.equals(TWO))
            return true;
        if (!w.testBit(0) || w.equals(ONE))
            return false;

        return w.primeToCertainty(certainty, null);
    }

    // Comparison Operations

    /**
     * Compares this BigInteger with the specified BigInteger.  This
     * method is provided in preference to individual methods for each
     * of the six boolean comparison operators ({@literal <}, ==,
     * {@literal >}, {@literal >=}, !=, {@literal <=}).  The suggested
     * idiom for performing these comparisons is: {@code
     * (x.compareTo(y)} &lt;<i>op</i>&gt; {@code 0)}, where
     * &lt;<i>op</i>&gt; is one of the six comparison operators.
     *
     * @param  val BigInteger to which this BigInteger is to be compared.
     * @return -1, 0 or 1 as this BigInteger is numerically less than, equal
     *         to, or greater than {@code val}.
     */
    public int compareTo(BigInteger val) {
        if (signum == val.signum) {
            switch (signum) {
                case 1:
                    return compareMagnitude(val);
                case -1:
                    return val.compareMagnitude(this);
                default:
                    return 0;
            }
        }
        return signum > val.signum ? 1 : -1;
    }

    /**
     * Compares the magnitude array of this BigInteger with the specified
     * BigInteger's. This is the version of compareTo ignoring sign.
     *
     * @param val BigInteger whose magnitude array to be compared.
     * @return -1, 0 or 1 as this magnitude array is less than, equal to or
     *         greater than the magnitude aray for the specified BigInteger's.
     */
    final int compareMagnitude(BigInteger val) {
        int[] m1 = mag;
        int len1 = m1.length;
        int[] m2 = val.mag;
        int len2 = m2.length;
        if (len1 < len2)
            return -1;
        if (len1 > len2)
            return 1;
        for (int i = 0; i < len1; i++) {
            int a = m1[i];
            int b = m2[i];
            if (a != b)
                return ((a & LONG_MASK) < (b & LONG_MASK)) ? -1 : 1;
        }
        return 0;
    }

    /**
     * Version of compareMagnitude that compares magnitude with long value.
     * val can't be Long.MIN_VALUE.
     */
    final int compareMagnitude(long val) {
        assert val != Long.MIN_VALUE;
        int[] m1 = mag;
        int len = m1.length;
        if(len > 2) {
            return 1;
        }
        if (val < 0) {
            val = -val;
        }
        int highWord = (int)(val >>> 32);
        if (highWord==0) {
            if (len < 1)
                return -1;
            if (len > 1)
                return 1;
            int a = m1[0];
            int b = (int)val;
            if (a != b) {
                return ((a & LONG_MASK) < (b & LONG_MASK))? -1 : 1;
            }
            return 0;
        } else {
            if (len < 2)
                return -1;
            int a = m1[0];
            int b = highWord;
            if (a != b) {
                return ((a & LONG_MASK) < (b & LONG_MASK))? -1 : 1;
            }
            a = m1[1];
            b = (int)val;
            if (a != b) {
                return ((a & LONG_MASK) < (b & LONG_MASK))? -1 : 1;
            }
            return 0;
        }
    }

    /**
     * Compares this BigInteger with the specified Object for equality.
     *
     * @param  x Object to which this BigInteger is to be compared.
     * @return {@code true} if and only if the specified Object is a
     *         BigInteger whose value is numerically equal to this BigInteger.
     */
    public boolean equals(Object x) {
        // This test is just an optimization, which may or may not help
        if (x == this)
            return true;

        if (!(x instanceof BigInteger))
            return false;

        BigInteger xInt = (BigInteger) x;
        if (xInt.signum != signum)
            return false;

        int[] m = mag;
        int len = m.length;
        int[] xm = xInt.mag;
        if (len != xm.length)
            return false;

        for (int i = 0; i < len; i++)
            if (xm[i] != m[i])
                return false;

        return true;
    }

    /**
     * Returns the minimum of this BigInteger and {@code val}.
     *
     * @param  val value with which the minimum is to be computed.
     * @return the BigInteger whose value is the lesser of this BigInteger and
     *         {@code val}.  If they are equal, either may be returned.
     */
    public BigInteger min(BigInteger val) {
        return (compareTo(val)<0 ? this : val);
    }

    /**
     * Returns the maximum of this BigInteger and {@code val}.
     *
     * @param  val value with which the maximum is to be computed.
     * @return the BigInteger whose value is the greater of this and
     *         {@code val}.  If they are equal, either may be returned.
     */
    public BigInteger max(BigInteger val) {
        return (compareTo(val)>0 ? this : val);
    }


    // Hash Function

    /**
     * Returns the hash code for this BigInteger.
     *
     * @return hash code for this BigInteger.
     */
    public int hashCode() {
        int hashCode = 0;

        for (int i=0; i<mag.length; i++)
            hashCode = (int)(31*hashCode + (mag[i] & LONG_MASK));

        return hashCode * signum;
    }

    /**
     * Returns the String representation of this BigInteger in the
     * given radix.  If the radix is outside the range from {@link
     * Character#MIN_RADIX} to {@link Character#MAX_RADIX} inclusive,
     * it will default to 10 (as is the case for
     * {@code Integer.toString}).  The digit-to-character mapping
     * provided by {@code Character.forDigit} is used, and a minus
     * sign is prepended if appropriate.  (This representation is
     * compatible with the {@link #BigInteger(String, int) (String,
     * int)} constructor.)
     *
     * @param  radix  radix of the String representation.
     * @return String representation of this BigInteger in the given radix.
     * @see    Integer#toString
     * @see    Character#forDigit
     * @see    #BigInteger(java.lang.String, int)
     */
    public String toString(int radix) {
        if (signum == 0)
            return "0";
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;

        // Compute upper bound on number of digit groups and allocate space
        int maxNumDigitGroups = (4*mag.length + 6)/7;
        String digitGroup[] = new String[maxNumDigitGroups];

        // Translate number to string, a digit group at a time
        BigInteger tmp = this.abs();
        int numGroups = 0;
        while (tmp.signum != 0) {
            BigInteger d = longRadix[radix];

            MutableBigInteger q = new MutableBigInteger(),
                    a = new MutableBigInteger(tmp.mag),
                    b = new MutableBigInteger(d.mag);
            MutableBigInteger r = a.divide(b, q);
            BigInteger q2 = q.toBigInteger(tmp.signum * d.signum);
            BigInteger r2 = r.toBigInteger(tmp.signum * d.signum);

            digitGroup[numGroups++] = Long.toString(r2.longValue(), radix);
            tmp = q2;
        }

        // Put sign (if any) and first digit group into result buffer
        StringBuilder buf = new StringBuilder(numGroups*digitsPerLong[radix]+1);
        if (signum<0)
            buf.append('-');
        buf.append(digitGroup[numGroups-1]);

        // Append remaining digit groups padded with leading zeros
        for (int i=numGroups-2; i>=0; i--) {
            // Prepend (any) leading zeros for this digit group
            int numLeadingZeros = digitsPerLong[radix]-digitGroup[i].length();
            if (numLeadingZeros != 0)
                buf.append(zeros[numLeadingZeros]);
            buf.append(digitGroup[i]);
        }
        return buf.toString();
    }

    /* zero[i] is a string of i consecutive zeros. */
    private static String zeros[] = new String[64];
    static {
        zeros[63] =
                "000000000000000000000000000000000000000000000000000000000000000";
        for (int i=0; i<63; i++)
            zeros[i] = zeros[63].substring(0, i);
    }

    /**
     * Returns the decimal String representation of this BigInteger.
     * The digit-to-character mapping provided by
     * {@code Character.forDigit} is used, and a minus sign is
     * prepended if appropriate.  (This representation is compatible
     * with the {@link #BigInteger(String) (String)} constructor, and
     * allows for String concatenation with Java's + operator.)
     *
     * @return decimal String representation of this BigInteger.
     * @see    Character#forDigit
     * @see    #BigInteger(java.lang.String)
     */
    public String toString() {
        return toString(10);
    }

    /**
     * Returns a byte array containing the two's-complement
     * representation of this BigInteger.  The byte array will be in
     * <i>big-endian</i> byte-order: the most significant byte is in
     * the zeroth element.  The array will contain the minimum number
     * of bytes required to represent this BigInteger, including at
     * least one sign bit, which is {@code (ceil((this.bitLength() +
     * 1)/8))}.  (This representation is compatible with the
     * {@link #BigInteger(byte[]) (byte[])} constructor.)
     *
     * @return a byte array containing the two's-complement representation of
     *         this BigInteger.
     * @see    #BigInteger(byte[])
     */
    public byte[] toByteArray() {
        int byteLen = bitLength()/8 + 1;
        byte[] byteArray = new byte[byteLen];

        for (int i=byteLen-1, bytesCopied=4, nextInt=0, intIndex=0; i>=0; i--) {
            if (bytesCopied == 4) {
                nextInt = getInt(intIndex++);
                bytesCopied = 1;
            } else {
                nextInt >>>= 8;
                bytesCopied++;
            }
            byteArray[i] = (byte)nextInt;
        }
        return byteArray;
    }

    /**
     * Converts this BigInteger to an {@code int}.  This
     * conversion is analogous to a
     * <i>narrowing primitive conversion</i> from {@code long} to
     * {@code int} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this BigInteger is too big to fit in an
     * {@code int}, only the low-order 32 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude of the BigInteger value as well as return a
     * result with the opposite sign.
     *
     * @return this BigInteger converted to an {@code int}.
     * @see #intValueExact()
     */
    public int intValue() {
        int result = 0;
        result = getInt(0);
        return result;
    }

    /**
     * Converts this BigInteger to a {@code long}.  This
     * conversion is analogous to a
     * <i>narrowing primitive conversion</i> from {@code long} to
     * {@code int} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this BigInteger is too big to fit in a
     * {@code long}, only the low-order 64 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude of the BigInteger value as well as return a
     * result with the opposite sign.
     *
     * @return this BigInteger converted to a {@code long}.
     * @see #longValueExact()
     */
    public long longValue() {
        long result = 0;

        for (int i=1; i>=0; i--)
            result = (result << 32) + (getInt(i) & LONG_MASK);
        return result;
    }

    /**
     * Converts this BigInteger to a {@code float}.  This
     * conversion is similar to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code float} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this BigInteger has too great a magnitude
     * to represent as a {@code float}, it will be converted to
     * {@link Float#NEGATIVE_INFINITY} or {@link
     * Float#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the BigInteger value.
     *
     * @return this BigInteger converted to a {@code float}.
     */
    public float floatValue() {
        // Somewhat inefficient, but guaranteed to work.
        return Float.parseFloat(this.toString());
    }

    /**
     * Converts this BigInteger to a {@code double}.  This
     * conversion is similar to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code float} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this BigInteger has too great a magnitude
     * to represent as a {@code double}, it will be converted to
     * {@link Double#NEGATIVE_INFINITY} or {@link
     * Double#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the BigInteger value.
     *
     * @return this BigInteger converted to a {@code double}.
     */
    public double doubleValue() {
        // Somewhat inefficient, but guaranteed to work.
        return Double.parseDouble(this.toString());
    }

    /**
     * Returns a copy of the input array stripped of any leading zero bytes.
     */
    private static int[] stripLeadingZeroInts(int val[]) {
        int vlen = val.length;
        int keep;

        // Find first nonzero byte
        for (keep = 0; keep < vlen && val[keep] == 0; keep++)
            ;
        return java.util.Arrays.copyOfRange(val, keep, vlen);
    }

    /**
     * Returns the input array stripped of any leading zero bytes.
     * Since the source is trusted the copying may be skipped.
     */
    private static int[] trustedStripLeadingZeroInts(int val[]) {
        int vlen = val.length;
        int keep;

        // Find first nonzero byte
        for (keep = 0; keep < vlen && val[keep] == 0; keep++)
            ;
        return keep == 0 ? val : java.util.Arrays.copyOfRange(val, keep, vlen);
    }

    /**
     * Returns a copy of the input array stripped of any leading zero bytes.
     */
    private static int[] stripLeadingZeroBytes(byte a[]) {
        int byteLength = a.length;
        int keep;

        // Find first nonzero byte
        for (keep = 0; keep < byteLength && a[keep]==0; keep++)
            ;

        // Allocate new array and copy relevant part of input array
        int intLength = ((byteLength - keep) + 3) >>> 2;
        int[] result = new int[intLength];
        int b = byteLength - 1;
        for (int i = intLength-1; i >= 0; i--) {
            result[i] = a[b--] & 0xff;
            int bytesRemaining = b - keep + 1;
            int bytesToTransfer = Math.min(3, bytesRemaining);
            for (int j=8; j <= (bytesToTransfer << 3); j += 8)
                result[i] |= ((a[b--] & 0xff) << j);
        }
        return result;
    }

    /**
     * Takes an array a representing a negative 2's-complement number and
     * returns the minimal (no leading zero bytes) unsigned whose value is -a.
     */
    private static int[] makePositive(byte a[]) {
        int keep, k;
        int byteLength = a.length;

        // Find first non-sign (0xff) byte of input
        for (keep=0; keep<byteLength && a[keep]==-1; keep++)
            ;


        /* Allocate output array.  If all non-sign bytes are 0x00, we must
         * allocate space for one extra output byte. */
        for (k=keep; k<byteLength && a[k]==0; k++)
            ;

        int extraByte = (k==byteLength) ? 1 : 0;
        int intLength = ((byteLength - keep + extraByte) + 3)/4;
        int result[] = new int[intLength];

        /* Copy one's complement of input into output, leaving extra
         * byte (if it exists) == 0x00 */
        int b = byteLength - 1;
        for (int i = intLength-1; i >= 0; i--) {
            result[i] = a[b--] & 0xff;
            int numBytesToTransfer = Math.min(3, b-keep+1);
            if (numBytesToTransfer < 0)
                numBytesToTransfer = 0;
            for (int j=8; j <= 8*numBytesToTransfer; j += 8)
                result[i] |= ((a[b--] & 0xff) << j);

            // Mask indicates which bits must be complemented
            int mask = -1 >>> (8*(3-numBytesToTransfer));
            result[i] = ~result[i] & mask;
        }

        // Add one to one's complement to generate two's complement
        for (int i=result.length-1; i>=0; i--) {
            result[i] = (int)((result[i] & LONG_MASK) + 1);
            if (result[i] != 0)
                break;
        }

        return result;
    }

    /**
     * Takes an array a representing a negative 2's-complement number and
     * returns the minimal (no leading zero ints) unsigned whose value is -a.
     */
    private static int[] makePositive(int a[]) {
        int keep, j;

        // Find first non-sign (0xffffffff) int of input
        for (keep=0; keep<a.length && a[keep]==-1; keep++)
            ;

        /* Allocate output array.  If all non-sign ints are 0x00, we must
         * allocate space for one extra output int. */
        for (j=keep; j<a.length && a[j]==0; j++)
            ;
        int extraInt = (j==a.length ? 1 : 0);
        int result[] = new int[a.length - keep + extraInt];

        /* Copy one's complement of input into output, leaving extra
         * int (if it exists) == 0x00 */
        for (int i = keep; i<a.length; i++)
            result[i - keep + extraInt] = ~a[i];

        // Add one to one's complement to generate two's complement
        for (int i=result.length-1; ++result[i]==0; i--)
            ;

        return result;
    }

    /*
     * The following two arrays are used for fast String conversions.  Both
     * are indexed by radix.  The first is the number of digits of the given
     * radix that can fit in a Java long without "going negative", i.e., the
     * highest integer n such that radix**n < 2**63.  The second is the
     * "long radix" that tears each number into "long digits", each of which
     * consists of the number of digits in the corresponding element in
     * digitsPerLong (longRadix[i] = i**digitPerLong[i]).  Both arrays have
     * nonsense values in their 0 and 1 elements, as radixes 0 and 1 are not
     * used.
     */
    private static int digitsPerLong[] = {0, 0,
            62, 39, 31, 27, 24, 22, 20, 19, 18, 18, 17, 17, 16, 16, 15, 15, 15, 14,
            14, 14, 14, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12};

    private static BigInteger longRadix[] = {null, null,
            valueOf(0x4000000000000000L), valueOf(0x383d9170b85ff80bL),
            valueOf(0x4000000000000000L), valueOf(0x6765c793fa10079dL),
            valueOf(0x41c21cb8e1000000L), valueOf(0x3642798750226111L),
            valueOf(0x1000000000000000L), valueOf(0x12bf307ae81ffd59L),
            valueOf( 0xde0b6b3a7640000L), valueOf(0x4d28cb56c33fa539L),
            valueOf(0x1eca170c00000000L), valueOf(0x780c7372621bd74dL),
            valueOf(0x1e39a5057d810000L), valueOf(0x5b27ac993df97701L),
            valueOf(0x1000000000000000L), valueOf(0x27b95e997e21d9f1L),
            valueOf(0x5da0e1e53c5c8000L), valueOf( 0xb16a458ef403f19L),
            valueOf(0x16bcc41e90000000L), valueOf(0x2d04b7fdd9c0ef49L),
            valueOf(0x5658597bcaa24000L), valueOf( 0x6feb266931a75b7L),
            valueOf( 0xc29e98000000000L), valueOf(0x14adf4b7320334b9L),
            valueOf(0x226ed36478bfa000L), valueOf(0x383d9170b85ff80bL),
            valueOf(0x5a3c23e39c000000L), valueOf( 0x4e900abb53e6b71L),
            valueOf( 0x7600ec618141000L), valueOf( 0xaee5720ee830681L),
            valueOf(0x1000000000000000L), valueOf(0x172588ad4f5f0981L),
            valueOf(0x211e44f7d02c1000L), valueOf(0x2ee56725f06e5c71L),
            valueOf(0x41c21cb8e1000000L)};

    /*
     * These two arrays are the integer analogue of above.
     */
    private static int digitsPerInt[] = {0, 0, 30, 19, 15, 13, 11,
            11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5};

    private static int intRadix[] = {0, 0,
            0x40000000, 0x4546b3db, 0x40000000, 0x48c27395, 0x159fd800,
            0x75db9c97, 0x40000000, 0x17179149, 0x3b9aca00, 0xcc6db61,
            0x19a10000, 0x309f1021, 0x57f6c100, 0xa2f1b6f,  0x10000000,
            0x18754571, 0x247dbc80, 0x3547667b, 0x4c4b4000, 0x6b5a6e1d,
            0x6c20a40,  0x8d2d931,  0xb640000,  0xe8d4a51,  0x1269ae40,
            0x17179149, 0x1cb91000, 0x23744899, 0x2b73a840, 0x34e63b41,
            0x40000000, 0x4cfa3cc1, 0x5c13d840, 0x6d91b519, 0x39aa400
    };

    /**
     * These routines provide access to the two's complement representation
     * of BigIntegers.
     */

    /**
     * Returns the length of the two's complement representation in ints,
     * including space for at least one sign bit.
     */
    private int intLength() {
        return (bitLength() >>> 5) + 1;
    }

    /* Returns sign bit */
    private int signBit() {
        return signum < 0 ? 1 : 0;
    }

    /* Returns an int of sign bits */
    private int signInt() {
        return signum < 0 ? -1 : 0;
    }

    /**
     * Returns the specified int of the little-endian two's complement
     * representation (int 0 is the least significant).  The int number can
     * be arbitrarily high (values are logically preceded by infinitely many
     * sign ints).
     */
    private int getInt(int n) {
        if (n < 0)
            return 0;
        if (n >= mag.length)
            return signInt();

        int magInt = mag[mag.length-n-1];

        return (signum >= 0 ? magInt :
                (n <= firstNonzeroIntNum() ? -magInt : ~magInt));
    }

    /**
     * Returns the index of the int that contains the first nonzero int in the
     * little-endian binary representation of the magnitude (int 0 is the
     * least significant). If the magnitude is zero, return value is undefined.
     */
    private int firstNonzeroIntNum() {
        int fn = firstNonzeroIntNum - 2;
        if (fn == -2) { // firstNonzeroIntNum not initialized yet
            fn = 0;

            // Search for the first nonzero int
            int i;
            int mlen = mag.length;
            for (i = mlen - 1; i >= 0 && mag[i] == 0; i--)
                ;
            fn = mlen - i - 1;
            firstNonzeroIntNum = fn + 2; // offset by two to initialize
        }
        return fn;
    }

    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = -8287574255936472291L;

    /**
     * Serializable fields for BigInteger.
     *
     * @serialField signum  int
     *              signum of this BigInteger.
     * @serialField magnitude int[]
     *              magnitude array of this BigInteger.
     * @serialField bitCount  int
     *              number of bits in this BigInteger
     * @serialField bitLength int
     *              the number of bits in the minimal two's-complement
     *              representation of this BigInteger
     * @serialField lowestSetBit int
     *              lowest set bit in the twos complement representation
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("signum", Integer.TYPE),
            new ObjectStreamField("magnitude", byte[].class),
            new ObjectStreamField("bitCount", Integer.TYPE),
            new ObjectStreamField("bitLength", Integer.TYPE),
            new ObjectStreamField("firstNonzeroByteNum", Integer.TYPE),
            new ObjectStreamField("lowestSetBit", Integer.TYPE)
    };

    /**
     * Reconstitute the {@code BigInteger} instance from a stream (that is,
     * deserialize it). The magnitude is read in as an array of bytes
     * for historical reasons, but it is converted to an array of ints
     * and the byte array is discarded.
     * Note:
     * The current convention is to initialize the cache fields, bitCount,
     * bitLength and lowestSetBit, to 0 rather than some other marker value.
     * Therefore, no explicit action to set these fields needs to be taken in
     * readObject because those fields already have a 0 value be default since
     * defaultReadObject is not being used.
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        /*
         * In order to maintain compatibility with previous serialized forms,
         * the magnitude of a BigInteger is serialized as an array of bytes.
         * The magnitude field is used as a temporary store for the byte array
         * that is deserialized. The cached computation fields should be
         * transient but are serialized for compatibility reasons.
         */

        // prepare to read the alternate persistent fields
        ObjectInputStream.GetField fields = s.readFields();

        // Read the alternate persistent fields that we care about
        int sign = fields.get("signum", -2);
        byte[] magnitude = (byte[])fields.get("magnitude", null);

        // Validate signum
        if (sign < -1 || sign > 1) {
            String message = "BigInteger: Invalid signum value";
            if (fields.defaulted("signum"))
                message = "BigInteger: Signum not present in stream";
            throw new java.io.StreamCorruptedException(message);
        }
        if ((magnitude.length == 0) != (sign == 0)) {
            String message = "BigInteger: signum-magnitude mismatch";
            if (fields.defaulted("magnitude"))
                message = "BigInteger: Magnitude not present in stream";
            throw new java.io.StreamCorruptedException(message);
        }

        // Commit final fields via Unsafe
        UnsafeHolder.putSign(this, sign);

        // Calculate mag field from magnitude and discard magnitude
        UnsafeHolder.putMag(this, stripLeadingZeroBytes(magnitude));
    }

    // Support for resetting final fields while deserializing
    private static class UnsafeHolder {
        private static final sun.misc.Unsafe unsafe;
        private static final long signumOffset;
        private static final long magOffset;
        static {
            try {
                unsafe = sun.misc.Unsafe.getUnsafe();
                signumOffset = unsafe.objectFieldOffset
                        (BigInteger.class.getDeclaredField("signum"));
                magOffset = unsafe.objectFieldOffset
                        (BigInteger.class.getDeclaredField("mag"));
            } catch (Exception ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }

        static void putSign(BigInteger bi, int sign) {
            unsafe.putIntVolatile(bi, signumOffset, sign);
        }

        static void putMag(BigInteger bi, int[] magnitude) {
            unsafe.putObjectVolatile(bi, magOffset, magnitude);
        }
    }

    /**
     * Save the {@code BigInteger} instance to a stream.
     * The magnitude of a BigInteger is serialized as a byte array for
     * historical reasons.
     *
     * @serialData two necessary fields are written as well as obsolete
     *             fields for compatibility with older versions.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        // set the values of the Serializable fields
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("signum", signum);
        fields.put("magnitude", magSerializedForm());
        // The values written for cached fields are compatible with older
        // versions, but are ignored in readObject so don't otherwise matter.
        fields.put("bitCount", -1);
        fields.put("bitLength", -1);
        fields.put("lowestSetBit", -2);
        fields.put("firstNonzeroByteNum", -2);

        // save them
        s.writeFields();
    }

    /**
     * Returns the mag array as an array of bytes.
     */
    private byte[] magSerializedForm() {
        int len = mag.length;

        int bitLen = (len == 0 ? 0 : ((len - 1) << 5) + bitLengthForInt(mag[0]));
        int byteLen = (bitLen + 7) >>> 3;
        byte[] result = new byte[byteLen];

        for (int i = byteLen - 1, bytesCopied = 4, intIndex = len - 1, nextInt = 0;
             i>=0; i--) {
            if (bytesCopied == 4) {
                nextInt = mag[intIndex--];
                bytesCopied = 1;
            } else {
                nextInt >>>= 8;
                bytesCopied++;
            }
            result[i] = (byte)nextInt;
        }
        return result;
    }

    /**
     * Converts this {@code BigInteger} to a {@code long}, checking
     * for lost information.  If the value of this {@code BigInteger}
     * is out of the range of the {@code long} type, then an
     * {@code ArithmeticException} is thrown.
     *
     * @return this {@code BigInteger} converted to a {@code long}.
     * @throws ArithmeticException if the value of {@code this} will
     * not exactly fit in a {@code long}.
     * @see BigInteger#longValue
     * @since  1.8
     */
    public long longValueExact() {
        if (mag.length <= 2 && bitLength() <= 63)
            return longValue();
        else
            throw new ArithmeticException("BigInteger out of long range");
    }

    /**
     * Converts this {@code BigInteger} to an {@code int}, checking
     * for lost information.  If the value of this {@code BigInteger}
     * is out of the range of the {@code int} type, then an
     * {@code ArithmeticException} is thrown.
     *
     * @return this {@code BigInteger} converted to an {@code int}.
     * @throws ArithmeticException if the value of {@code this} will
     * not exactly fit in a {@code int}.
     * @see BigInteger#intValue
     * @since  1.8
     */
    public int intValueExact() {
        if (mag.length <= 1 && bitLength() <= 31)
            return intValue();
        else
            throw new ArithmeticException("BigInteger out of int range");
    }

    /**
     * Converts this {@code BigInteger} to a {@code short}, checking
     * for lost information.  If the value of this {@code BigInteger}
     * is out of the range of the {@code short} type, then an
     * {@code ArithmeticException} is thrown.
     *
     * @return this {@code BigInteger} converted to a {@code short}.
     * @throws ArithmeticException if the value of {@code this} will
     * not exactly fit in a {@code short}.
     * @see BigInteger#shortValue
     * @since  1.8
     */
    public short shortValueExact() {
        if (mag.length <= 1 && bitLength() <= 31) {
            int value = intValue();
            if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
                return shortValue();
        }
        throw new ArithmeticException("BigInteger out of short range");
    }

    /**
     * Converts this {@code BigInteger} to a {@code byte}, checking
     * for lost information.  If the value of this {@code BigInteger}
     * is out of the range of the {@code byte} type, then an
     * {@code ArithmeticException} is thrown.
     *
     * @return this {@code BigInteger} converted to a {@code byte}.
     * @throws ArithmeticException if the value of {@code this} will
     * not exactly fit in a {@code byte}.
     * @see BigInteger#byteValue
     * @since  1.8
     */
    public byte byteValueExact() {
        if (mag.length <= 1 && bitLength() <= 31) {
            int value = intValue();
            if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
                return byteValue();
        }
        throw new ArithmeticException("BigInteger out of byte range");
    }
}