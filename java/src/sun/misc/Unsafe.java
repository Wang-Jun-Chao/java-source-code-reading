/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.misc;

import java.security.*;
import java.lang.reflect.*;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;


/**
 * <pre>
 * https://www.cnblogs.com/mickole/articles/3757278.html
 * java不能直接访问操作系统底层，而是通过本地方法来访问。Unsafe类提供了硬件级别的原子操作，主要提供了以下功能：
 *
 * 1、通过Unsafe类可以分配内存，可以释放内存；
 * 类中提供的3个本地方法allocateMemory、reallocateMemory、freeMemory分别用于分配内存，扩充内存和释放内存，
 * 与C语言中的3个方法对应。
 *
 * 2、可以定位对象某字段的内存位置，也可以修改对象的字段值，即使它是私有的；
 *
 * 1、Unsafe有可能在未来的Jdk版本移除或者不允许Java应用代码使用，这一点可能导致使用了Unsafe的应用无法运行在高
 * 版本的Jdk。
 * 2、Unsafe的不少方法中必须提供原始地址(内存地址)和被替换对象的地址，偏移量要自己计算，一旦出现问题就是JVM崩溃
 * 级别的异常，会导致整个JVM实例崩溃，表现为应用程序直接crash掉。
 * 3、Unsafe提供的直接内存访问的方法中使用的内存不受JVM管理(无法被GC)，需要手动管理，一旦出现疏忽很有可能成为内
 * 存泄漏的源头。
 * </pre>
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * @author John R. Rose
 * @see #getUnsafe
 */

public final class Unsafe {

    private static native void registerNatives();
    static {
        registerNatives();
        Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }

    private Unsafe() {}

    private static final Unsafe theUnsafe = new Unsafe();

    /**
     * 为调用者提供执行不安全操作的能力。
     *
     * 返回的<code>Unsafe</code>对象应该由调用者小心保护，因为它可以用于在任意内
     * 存地址读取和写入数据。 绝不能将其传递给不受信任的代码。
     *
     * 此类中的大多数方法都是非常低级的，并且对应于少量硬件指令（在典型的机器上）。
     * 鼓励编译器相应地优化这些方法。
     *
     * Provides the caller with the capability of performing unsafe
     * operations.
     *
     * <p> The returned <code>Unsafe</code> object should be carefully guarded
     * by the caller, since it can be used to read and write data at arbitrary
     * memory addresses.  It must never be passed to untrusted code.
     *
     * <p> Most methods in this class are very low-level, and correspond to a
     * small number of hardware instructions (on typical machines).  Compilers
     * are encouraged to optimize these methods accordingly.
     *
     * <p> Here is a suggested idiom for using unsafe operations:
     *
     * <blockquote><pre>
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }
     * </pre></blockquote>
     *
     * 它可以帮助编译器创建局部变量为final类型
     * (It may assist compilers to make the local variable be
     * <code>final</code>.)
     *
     * 如果存在安全管理器且其<code>checkPropertiesAccess </code>方法不允许访问系统属性
     * 会抛出SecurityException
     * @exception  SecurityException  if a security manager exists and its
     *             <code>checkPropertiesAccess</code> method doesn't allow
     *             access to the system properties.
     */
    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        // 只有由启动类加载器(BootStrap classLoader)加载的类才能调用这个类中的方法
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }

    /// peek and poke operations
    // （peek和poke操作见：https://en.wikipedia.org/wiki/PEEK_and_POKE）
    /// (compilers should optimize these to memory ops)
    // （编译器应该优化这些内存操作）

    // These work on object fields in the Java heap.
    // 这些工作在Java堆中的对象字段上
    // They will not work on elements of packed arrays.
    // 它们不适用于压缩数组的元素

    /**
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object <code>o</code> at the given offset, or (if <code>o</code> is
     * null) from the memory address whose numerical value is the given
     * offset.
     * 从给定的Java变量中获取值。 更具体地说，从给定偏移量的给定对象<code>o</code>
     * 中获取字段或数组元素，或者（如果<code> o </ code>为空）从数据值为给定的内存
     * 地址中获取偏移。
     *
     * <p>
     * The results are undefined unless one of the following cases is true:
     * 除非下列情况之一成立，否则结果未定义
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link Field} of some Java field and the object
     * referred to by <code>o</code> is of a class compatible with that
     * field's class.
     * 偏移量是从某些Java字段的{@link Field}上的{@link #objectFieldOffset}获得的，
     * 而<code>o</code>引用的对象是与该字段的类兼容的类。
     *
     * <li>The offset and object reference <code>o</code> (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     * 偏移量和对象引用<code>o</ code>（null或非null）都是通过
     * {@link #staticFieldOffset}和{@link #staticFieldBase}（分别）
     * 从反射{@link Field}表示获得的 一些Java领域。
     *
     *
     * <li>The object referred to by <code>o</code> is an array, and the offset
     * is an integer of the form <code>B+N*S</code>, where <code>N</code> is
     * a valid index into the array, and <code>B</code> and <code>S</code> are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the <code>N</code><em>th</em> element of the array.
     *
     * <code>o</ code>引用的对象是一个数组，偏移量是<code>B+N*S</ code>形式的
     * 整数，其中<code>N</code>是一个数组的有效索引，<code>B</ code>和
     * <code>S</ code>是数组类中{@link #arrayBaseOffset}和{@link #arrayIndexScale}
     * 分别获得的值。 引用的值是数组的<code>N</code><em>th</em>个元素。
     * </ul>
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     * 如果上述情况之一为真，则调用引用特定的Java变量（字段或数组元素）。 但是，如果该变
     * 量实际上不是此方法返回的类型，则结果是未定义的。
     *
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     * 此方法通过两个参数引用变量，因此它为Java变量提供（实际上）<em>双寄存器</em>寻址模式。
     * 当对象引用为null时，此方法将其偏移量用作绝对地址。 这在操作上类似于{@link #getInt(long)}
     * 等方法，它为非Java变量提供（实际上）<em>单寄存器</em>寻址模式。但是，因为Java变量
     * 在内存中可能与非Java变量具有不同的布局，所以程序员不应该假设这两种寻址模式是等价的。
     * 此外，程序员应该记住，双寄存器寻址模式的偏移不能与单寄存器寻址模式中使用的long混淆。
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     *          变量所在的Java堆对象（如果有），否则为null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     *               指示变量驻留在Java堆对象中的位置（如果有），否则是静态定位变量的内存地址
     * @return the value fetched from the indicated Java variable
     *          从指示的Java变量获取的值
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    public native int getInt(Object o, long offset);

    /**
     * 将值存储到给定的Java变量中。
     *
     * Stores a value into a given Java variable.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     * <p>
     * The variable must be of the same type as the method
     * parameter <code>x</code>.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @param x the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    public native void putInt(Object o, long offset, int x);

    /**
     * 通过给定的Java变量获取引用值。这里实际上是获取一个Java对象o中，获取偏移地址为offset的属性的值，
     * 此方法可以突破修饰符的抑制，也就是无视private、protected和default修饰符
     * Fetches a reference value from a given Java variable.
     * @see #getInt(Object, long)
     */
    public native Object getObject(Object o, long offset);

    /**
     * 将引用值存储到给定的Java变量中。这里实际上是设置一个Java对象o中偏移地址为offset的属性的值为x，
     * 此方法可以突破修饰符的抑制，也就是无视private、protected和default修饰符
     *
     * 将引用值存储到给定的Java变量中。 除非存储的引用<code>x</code>为null或与字段类型匹配，
     * 否则结果是未定义的。 如果引用<code>o</code>为非null，则更新该对象的car标记或其他存
     * 储屏障（如果VM需要它们）。
     *
     * Stores a reference value into a given Java variable.
     * <p>
     * Unless the reference <code>x</code> being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference <code>o</code> is non-null, car marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     * @see #putInt(Object, int, int)
     */
    public native void putObject(Object o, long offset, Object x);

    /** @see #getInt(Object, long) */
    public native boolean getBoolean(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putBoolean(Object o, long offset, boolean x);
    /** @see #getInt(Object, long) */
    public native byte    getByte(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putByte(Object o, long offset, byte x);
    /** @see #getInt(Object, long) */
    public native short   getShort(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putShort(Object o, long offset, short x);
    /** @see #getInt(Object, long) */
    public native char    getChar(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putChar(Object o, long offset, char x);
    /** @see #getInt(Object, long) */
    public native long    getLong(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putLong(Object o, long offset, long x);
    /** @see #getInt(Object, long) */
    public native float   getFloat(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putFloat(Object o, long offset, float x);
    /** @see #getInt(Object, long) */
    public native double  getDouble(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putDouble(Object o, long offset, double x);

    /**
     * This method, like all others with 32-bit offsets, was native
     * in a previous release but is now a wrapper which simply casts
     * the offset to a long value.  It provides backward compatibility
     * with bytecodes compiled against 1.4.
     * 与所有具有32位偏移的其他方法一样，此方法在先前版本中是原生的，但现在是一个包装器，
     * 它只是将偏移量转换为long值。 它提供了与1.4编译的字节码的向后兼容性。
     *
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public int getInt(Object o, int offset) {
        return getInt(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putInt(Object o, int offset, int x) {
        putInt(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public Object getObject(Object o, int offset) {
        return getObject(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putObject(Object o, int offset, Object x) {
        putObject(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public boolean getBoolean(Object o, int offset) {
        return getBoolean(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putBoolean(Object o, int offset, boolean x) {
        putBoolean(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public byte getByte(Object o, int offset) {
        return getByte(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putByte(Object o, int offset, byte x) {
        putByte(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public short getShort(Object o, int offset) {
        return getShort(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putShort(Object o, int offset, short x) {
        putShort(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public char getChar(Object o, int offset) {
        return getChar(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putChar(Object o, int offset, char x) {
        putChar(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public long getLong(Object o, int offset) {
        return getLong(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putLong(Object o, int offset, long x) {
        putLong(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public float getFloat(Object o, int offset) {
        return getFloat(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putFloat(Object o, int offset, float x) {
        putFloat(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public double getDouble(Object o, int offset) {
        return getDouble(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * 从1.4.1开始，将32位偏移量参数转换为long。
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putDouble(Object o, int offset, double x) {
        putDouble(o, (long)offset, x);
    }

    // These work on values in the C heap.
    // 这些对C堆中的值有效。

    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     * 从给定的内存地址中获取值。 如果地址为零，或者没有指向从{@link #allocateMemory}获得的块，
     * 则结果是未定义的。
     *
     * @see #allocateMemory
     */
    public native byte    getByte(long address);

    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     * 将值存储到给定的内存地址中。 如果地址为零，或者没有指向从{@link #allocateMemory}获得
     * 的块，则结果是未定义的。
     *
     * @see #getByte(long)
     */
    public native void    putByte(long address, byte x);

    /** @see #getByte(long) */
    public native short   getShort(long address);
    /** @see #putByte(long, byte) */
    public native void    putShort(long address, short x);
    /** @see #getByte(long) */
    public native char    getChar(long address);
    /** @see #putByte(long, byte) */
    public native void    putChar(long address, char x);
    /** @see #getByte(long) */
    public native int     getInt(long address);
    /** @see #putByte(long, byte) */
    public native void    putInt(long address, int x);
    /** @see #getByte(long) */
    public native long    getLong(long address);
    /** @see #putByte(long, byte) */
    public native void    putLong(long address, long x);
    /** @see #getByte(long) */
    public native float   getFloat(long address);
    /** @see #putByte(long, byte) */
    public native void    putFloat(long address, float x);
    /** @see #getByte(long) */
    public native double  getDouble(long address);
    /** @see #putByte(long, byte) */
    public native void    putDouble(long address, double x);

    /**
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     * 从给定的内存地址获取本机指针。 如果地址为零，或者没有指向从{@link #allocateMemory}
     * 获得的块，则结果是未定义的。
     *
     * <p> If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address maybe determined by consulting {@link
     * #addressSize}.
     *
     * 如果本机指针的宽度小于64位，则将其作为无符号数扩展为Java long。 指针可以由任何给定的字节
     * 偏移索引，简单地通过将该偏移（作为简单整数）添加到long表示指针。 实际从目标地址读取的字节
     * 数可以通过查询{@link #addressSize}来确定
     *
     * @see #allocateMemory
     */
    public native long getAddress(long address);

    /**
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     * 将本机指针存储到给定的内存地址中。 如果地址为零，或者没有指向从{@link #allocateMemory}
     * 获得的块，则结果是未定义的。
     *
     * <p> The number of bytes actually written at the target address maybe
     * determined by consulting {@link #addressSize}.
     * 实际写入目标地址的字节数可以通过查询{@link #addressSize}来确定。
     *
     * @see #getAddress(long)
     */
    public native void putAddress(long address, long x);

    /// wrappers for malloc, realloc, free:
    /// malloc，realloc，free的包装器：

    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     * 分配给定大小的新的本机内存块（以字节为单位）。 内存的内容是未初始化的; 他们通常会是垃圾。
     * 生成的本机指针永远不会为零，并且将针对所有值类型进行对齐。 通过调用{@link #freeMemory}
     * 释放此内存，或使用{@link #reallocateMemory}调整其大小
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *         如果原始size_t类型的大小为负或太大
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *         如果分配被系统拒绝
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    public native long allocateMemory(long bytes);

    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     * 将新的本机内存块调整为给定大小（以字节为单位）。 超过旧块大小的新块的内容未初始化;
     * 他们通常会是垃圾。 当且仅当请求的大小为零时，生成的本机指针将为零。 生成的本机指针
     * 将与所有值类型对齐。 通过调用{@link #freeMemory}释放此内存，或使用{@link
     * #reallocateMemory}调整其大小。 传递给此方法的地址可以为null，在这种情况下将执行分配。
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *         如果原始size_t类型的大小为负或太大
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *         如果分配被系统拒绝
     * @see #allocateMemory
     */
    public native long reallocateMemory(long address, long bytes);

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     * 将给定内存块中的所有字节设置为固定值（通常为零）。
     *
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     * 此方法通过两个参数确定块的基址，因此它提供（实际上）<em>双寄存器</ em>寻址模式，
     * 如{@link #getInt(Object, long)}中所述。 当对象引用为null时，偏移量提供绝对基址。
     *
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * 存储是由地址和长度参数确定的大小的相干（原子）单元。 如果有效地址和长度均为模8，
     * 则存储以“long”为单位进行。 如果有效地址和长度（即相等）甚至模4或2，则存储以
     * “int”或“short”为单位进行。
     *
     * @since 1.7
     */
    public native void setMemory(Object o, long offset, long bytes, byte value);

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     * 将给定内存块中的所有字节设置为固定值（通常为零）。 这提供了<em>单寄存器</em>寻址模式，
     * 如{@link #getInt(Object,long)}中所述。
     *
     * <p>Equivalent to <code>setMemory(null, address, bytes, value)</code>.
     */
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     * 将给定内存块中的所有字节设置为另一个块的副本。进行内存复制
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     * 此方法通过两个参数确定每个块的基址，因此它提供（实际上）<em>双寄存器</em>寻址模式，
     * 如{@link #getInt(Object,long)}中所述。 当对象引用为null时，偏移量提供绝对基址。
     *
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     * 存储是由地址和长度参数确定的大小的相干（原子）单元。 如果有效地址和长度均为模8，
     * 则存储以“long”为单位进行。 如果有效地址和长度（即相等）甚至模4或2，则存储以
     * “int”或“short”为单位进行。
     * @since 1.7
     */
    public native void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes);
    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     * 将给定内存块中的所有字节设置为另一个块的副本值。 这提供了<em>单寄存器</em>寻址模式，
     * 如{@link #getInt(Object,long)}中所述。
     *
     * Equivalent to <code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>.
     */
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        copyMemory(null, srcAddress, null, destAddress, bytes);
    }

    /**
     * Disposes of a block of native memory, as obtained from {@link
     * #allocateMemory} or {@link #reallocateMemory}.  The address passed to
     * this method may be null, in which case no action is taken.
     * 释放从{@link #allocateMemory}或{@link #reallocateMemory}获得的本机内存块。
     * 传递给此方法的地址可能为null，在这种情况下不执行任何操作。
     * @see #allocateMemory
     */
    public native void freeMemory(long address);

    /// random queries
    /// 随机查询

    /**
     * This constant differs from all results that will ever be returned from
     * {@link #staticFieldOffset}, {@link #objectFieldOffset},
     * or {@link #arrayBaseOffset}.
     *
     * 此常量不同于从{@link #staticFieldOffset}，{@link #objectFieldOffset}或
     * {@link #arrayBaseOffset}返回的所有结果。
     * 用于标记非法的字段偏移地址
     *
     */
    public static final int INVALID_FIELD_OFFSET   = -1;

    /**
     * Returns the offset of a field, truncated to 32 bits.
     * This method is implemented as follows:
     * 返回字段的偏移量，截断为32位。 该方法实现如下：
     *
     * <blockquote><pre>
     * public int fieldOffset(Field f) {
     *     if (Modifier.isStatic(f.getModifiers()))
     *         return (int) staticFieldOffset(f);
     *     else
     *         return (int) objectFieldOffset(f);
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldOffset} for static
     * fields and {@link #objectFieldOffset} for non-static fields.
     * 从1.4.1开始，对静态字段使用{@link #staticFieldOffset}，对非静态字段使用
     * {@link #objectFieldOffset}
     *
     */
    @Deprecated
    public int fieldOffset(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return (int) staticFieldOffset(f);
        else
            return (int) objectFieldOffset(f);
    }

    /**
     * Returns the base address for accessing some static field
     * in the given class.  This method is implemented as follows:
     * 返回用于访问给定类中的某些静态字段的基址。 该方法实现如下：
     * <blockquote><pre>
     * public Object staticFieldBase(Class c) {
     *     Field[] fields = c.getDeclaredFields();
     *     for (int i = 0; i < fields.length; i++) {
     *         if (Modifier.isStatic(fields[i].getModifiers())) {
     *             return staticFieldBase(fields[i]);
     *         }
     *     }
     *     return null;
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldBase(Field)}
     * to obtain the base pertaining to a specific {@link Field}.
     * This method works only for JVMs which store all statics
     * for a given class in one place.
     * 从1.4.1开始，使用{@link #staticFieldBase（Field）}获取与特定
     * {@link Field}相关的基址。 此方法仅适用于在一个位置存储给定类的所
     * 有静态属性的JVM。
     */
    @Deprecated
    public Object staticFieldBase(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers())) {
                return staticFieldBase(fields[i]);
            }
        }
        return null;
    }

    /**
     * Report the location of a given field in the storage allocation of its
     * class.  Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     * 返回给定字段在其类的存储分配中的位置。 不要指望对此偏移量执行任何算术运算;
     * 它只是一个传递给不安全堆内存访问器的方式（cookie）。
     *
     * <p>Any given field will always have the same offset and base, and no
     * two distinct fields of the same class will ever have the same offset
     * and base.
     * 任何给定的字段将始终具有相同的偏移量和基数，并且同一类的两个不同字段将不会具有相同
     * 的偏移量和基数。
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * However, JVM implementations which store static fields at absolute
     * addresses can use long offsets and null base pointers to express
     * the field locations in a form usable by {@link #getInt(Object,long)}.
     * Therefore, code which will be ported to such JVMs on 64-bit platforms
     * must preserve all bits of static field offsets.
     * @see #getInt(Object, long)
     *
     * 从1.4.1开始，字段的偏移量使用long值表示，但是Sun JVM不使用最有影响的32位。
     * 但是，对使用绝对地址存储静态字段的JVM，可以使用长偏移量和空基指针以{@link
     * #getInt(Object,long)}可用的形式表示字段位置。 因此，被移植到64位JVM的代码
     * 必须保留所有静态字段偏移量。
     */

    public native long staticFieldOffset(Field f);

    /**
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldBase}.
     * 结合{@link #staticFieldBase}，返回给定静态字段的位置。
     *
     * <p>Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     * 不要指望对此偏移量执行任何算术运算; 它只是一个传递给不安全堆内存访问器的方式（cookie）。
     *
     * <p>Any given field will always have the same offset, and no two distinct
     * fields of the same class will ever have the same offset.
     * 任何给定字段将始终具有相同的偏移量，并且同一类的两个不同字段将不会具有相同的偏移量。
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * It is hard to imagine a JVM technology which needs more than
     * a few bits to encode an offset within a non-array object,
     * However, for consistency with other methods in this class,
     * this method reports its result as a long value.
     * 从1.4.1开始，字段的偏移量表示为long值，尽管Sun JVM不使用最重要的32位。很难想象一种
     * JVM技术需要多几位来编码非数组对象内的偏移量，但是，为了与此类中的其他方法保持一致，
     * 此方法将其结果返回为long值。
     * @see #getInt(Object, long)
     */
    public native long objectFieldOffset(Field f);

    /**
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldOffset}.
     * 结合{@link #staticFieldOffset}，报告给定静态字段的位置
     *
     * <p>Fetch the base "Object", if any, with which static fields of the
     * given class can be accessed via methods like {@link #getInt(Object,
     * long)}.  This value may be null.  This value may refer to an object
     * which is a "cookie", not guaranteed to be a real Object, and it should
     * not be used in any way except as argument to the get and put routines in
     * this class.
     * 获取基址“Object”（如果有），可以通过{@link #getInt（Object，long）}等方法访问
     * 给定类的静态字段。 该值可以为null。 这个值可能指的是一个“cookie”的对象，不能保证
     * 是一个真正的Object，除了作为此类中get和put例程的参数之外，它不应该以任何方式使用。
     *
     */
    public native Object staticFieldBase(Field f);

    /**
     * Detect if the given class may need to be initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     * 检测是否需要初始化给定的类。 这通常需要与获得类的静态字段库一起使用。
     *
     * @return false only if a call to {@code ensureClassInitialized} would have no effect
     *      仅当对{@code ensureClassInitialized}的调用无效时才为false
     */
    public native boolean shouldBeInitialized(Class<?> c);

    /**
     * Ensure the given class has been initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     * 擦除已初始化的给定类。 这通常需要与获得类的静态字段库一起使用。
     */
    public native void ensureClassInitialized(Class<?> c);

    /**
     * Report the offset of the first element in the storage allocation of a
     * given array class.  If {@link #arrayIndexScale} returns a non-zero value
     * for the same class, you may use that scale factor, together with this
     * base offset, to form new offsets to access elements of arrays of the
     * given class.
     * 返回数组第一个元素的偏移地址，如果{@link #arrayIndexScale}为同一个类返回非零值，
     * 则可以使用该比例因子以及此基本偏移量来形成新的偏移量，以访问给定类的数组元素。
     *
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    public native int arrayBaseOffset(Class<?> arrayClass);

    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /**
     * Report the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, int)}, so the scale factor for such classes is reported
     * as zero.
     * 返回在给定数组类的存储分配中寻址元素的比例因子。 但是，“narrow”类型的数组通常无法与
     * {@link #getByte（Object，int）}等访问器一起正常工作，因此这类的比例因子返回为零。
     * 注narrow类型一般指不小于4字节宽度的类型，比较boolean, short, byte, char等
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    public native int arrayIndexScale(Class<?> arrayClass);

    // 基本数组类型的偏移量

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

    /**
     * Report the size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the sizes of
     * other primitive types (as stored in native memory blocks) is determined
     * fully by their information content.
     * 返回通过{@link #putAddress}存储的本机指针的大小（以字节为单位）。 此值将为4或8.
     * 请注意，其他基本类型的大小（存储在本机内存块中）完全由其信息内容决定。
     */
    public native int addressSize();

    // 地址大小
    /** The value of {@code addressSize()} */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    /**
     * Report the size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     * 报告本机内存页面的大小（以字节为单位）（无论是什么）。 该值始终为2的幂。
     */
    public native int pageSize();


    /// random trusted operations from JNI:
    /// 来自JNI的随机可信操作

    /**
     * Tell the VM to define a class, without security checks.  By default, the
     * class loader and protection domain come from the caller's class.
     * 告诉VM定义一个类，没有安全检查。 默认情况下，类加载器和保护域来自调用者的类。
     */
    public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                       ClassLoader loader,
                                       ProtectionDomain protectionDomain);

    /**
     * Define a class but do not make it known to the class loader or system dictionary.
     * 定义一个类，但不要让类加载器或系统字典知道它。
     * <p>
     * For each CP entry, the corresponding CP patch must either be null or have
     * the a format that matches its tag:
     * 对于每个CP条目，相应的CP补丁必须为null或具有与其标记匹配的格式
     * <ul>
     * <li>Integer, Long, Float, Double: the corresponding wrapper object type from java.lang
     * Integer，Long，Float，Double：来自java.lang的相应包装器对象类型
     * <li>Utf8: a string (must have suitable syntax if used as signature or name)
     * Utf8：一个字符串（如果用作签名或名称，必须具有合适的语法）
     * <li>Class: any java.lang.Class object
     * Class：任何java.lang.Class对象
     * <li>String: any object (not just a java.lang.String)
     * 字符串(String)：任何对象（不仅仅是java.lang.String）
     * <li>InterfaceMethodRef: (NYI) a method handle to invoke on that call site's arguments
     * InterfaceMethodRef：（NYI）一个方法句柄，用于调用该调用点的参数
     * </ul>
     * @params hostClass context for linkage, access control, protection domain, and class loader
     *         链接，访问控制，保护域和类加载器的上下文
     * @params data      bytes of a class file
     *         类文件的字节
     * @params cpPatches where non-null entries exist, they replace corresponding CP entries in data
     *         在存在非空条目的情况下，它们替换数据中的相应CP条目
     */
    public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);


    /** Allocate an instance but do not run any constructor.
     *  Initializes the class if it has not yet been.
     * 分配实例但不运行任何构造函数。 如果尚未进行，则初始化该类。
     */
    public native Object allocateInstance(Class<?> cls)
        throws InstantiationException;

    /**
     * Lock the object.  It must get unlocked via {@link #monitorExit}.
     * 锁定对象。 它必须通过{@link #monitorExit}解锁。
     */

    @Deprecated
    public native void monitorEnter(Object o);

    /**
     * Unlock the object.  It must have been locked via {@link #monitorEnter}.
     * 解锁对象。 它必须已通过{@link #monitorEnter}锁定。
     */
    @Deprecated
    public native void monitorExit(Object o);

    /**
     * Tries to lock the object.  Returns true or false to indicate
     * whether the lock succeeded.  If it did, the object must be
     * unlocked via {@link #monitorExit}.
     * 试图锁定对象。 返回true或false以指示锁是否成功。 如果是，则必须通过
     * {@link #monitorExit}解锁对象。
     */
    @Deprecated
    public native boolean tryMonitorEnter(Object o);

    /**
     * Throw the exception without telling the verifier.
     * 抛出异常而不告诉验证者。
     */
    public native void throwException(Throwable ee);


    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * 如果Java变量当前持有<tt>expected</tt>，则将其原子地更新为<tt>x</tt>。
     * @return <tt>true</tt> if successful
     *         更新成功返回true
     */
    public final native boolean compareAndSwapObject(Object o, long offset,
                                                     Object expected,
                                                     Object x);

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * 如果Java变量当前持有<tt>expected</tt>，则将其原子地更新为<tt>x</tt>。
     * @return <tt>true</tt> if successful
     *         更新成功返回true
     */
    public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * 如果Java变量当前持有<tt>expected</tt>，则将其原子地更新为<tt>x</tt>。
     * @return <tt>true</tt> if successful
     *         更新成功返回true
     */
    public final native boolean compareAndSwapLong(Object o, long offset,
                                                   long expected,
                                                   long x);

    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     * 从给定的Java变量中获取具有volatile加载语义的引用值。 否则与{@link
     * #getObject(Object,long)}相同
     */
    public native Object getObjectVolatile(Object o, long offset);

    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     * 使用volatile存储语义将引用值存储到给定的Java变量中。 否则与{@link
     * #putObject(Object,long,Object)}相同
     */
    public native void    putObjectVolatile(Object o, long offset, Object x);

    // 基本类型的volatile版本

    /** Volatile version of {@link #getInt(Object, long)}  */
    public native int     getIntVolatile(Object o, long offset);

    /** Volatile version of {@link #putInt(Object, long, int)}  */
    public native void    putIntVolatile(Object o, long offset, int x);

    /** Volatile version of {@link #getBoolean(Object, long)}  */
    public native boolean getBooleanVolatile(Object o, long offset);

    /** Volatile version of {@link #putBoolean(Object, long, boolean)}  */
    public native void    putBooleanVolatile(Object o, long offset, boolean x);

    /** Volatile version of {@link #getByte(Object, long)}  */
    public native byte    getByteVolatile(Object o, long offset);

    /** Volatile version of {@link #putByte(Object, long, byte)}  */
    public native void    putByteVolatile(Object o, long offset, byte x);

    /** Volatile version of {@link #getShort(Object, long)}  */
    public native short   getShortVolatile(Object o, long offset);

    /** Volatile version of {@link #putShort(Object, long, short)}  */
    public native void    putShortVolatile(Object o, long offset, short x);

    /** Volatile version of {@link #getChar(Object, long)}  */
    public native char    getCharVolatile(Object o, long offset);

    /** Volatile version of {@link #putChar(Object, long, char)}  */
    public native void    putCharVolatile(Object o, long offset, char x);

    /** Volatile version of {@link #getLong(Object, long)}  */
    public native long    getLongVolatile(Object o, long offset);

    /** Volatile version of {@link #putLong(Object, long, long)}  */
    public native void    putLongVolatile(Object o, long offset, long x);

    /** Volatile version of {@link #getFloat(Object, long)}  */
    public native float   getFloatVolatile(Object o, long offset);

    /** Volatile version of {@link #putFloat(Object, long, float)}  */
    public native void    putFloatVolatile(Object o, long offset, float x);

    /** Volatile version of {@link #getDouble(Object, long)}  */
    public native double  getDoubleVolatile(Object o, long offset);

    /** Volatile version of {@link #putDouble(Object, long, double)}  */
    public native void    putDoubleVolatile(Object o, long offset, double x);

    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     * {@link #putObjectVolatile（Object，long，Object）}的版本，不保证存储立即
     * 对其他线程可见。 此方法通常仅在底层字段是Java volatile时（或者如果是数组单元，
     * 否则仅使用volatile修饰进行访问）才有用。
     */
    public native void    putOrderedObject(Object o, long offset, Object x);

    /** Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}  */
    public native void    putOrderedInt(Object o, long offset, int x);

    /** Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)} */
    public native void    putOrderedLong(Object o, long offset, long x);

    /**
     * Unblock the given thread blocked on <tt>park</tt>, or, if it is
     * not blocked, cause the subsequent call to <tt>park</tt> not to
     * block.  Note: this operation is "unsafe" solely because the
     * caller must somehow ensure that the thread has not been
     * destroyed. Nothing special is usually required to ensure this
     * when called from Java (in which there will ordinarily be a live
     * reference to the thread) but this is not nearly-automatically
     * so when calling from native code.
     * 取消阻塞在<tt> park </ tt>上的给定线程，或者，如果它未被阻塞，则导致对<tt>park</tt>
     * 的后续调用不阻塞。 注意：此操作“不安全”仅仅是因为调用者必须以某种方式确保线程未被销毁。
     * 从Java调用时通常不需要特殊的东西来确保这一点（通常会有一个对线程的实时引用）但是当从
     * 本机代码调用时，这几乎不是自动的。
     * @param thread the thread to unpark.
     *               取消阻塞的线程
     *
     */
    public native void unpark(Object thread);

    /**
     * Block current thread, returning when a balancing
     * <tt>unpark</tt> occurs, or a balancing <tt>unpark</tt> has
     * already occurred, or the thread is interrupted, or, if not
     * absolute and time is not zero, the given time nanoseconds have
     * elapsed, or if absolute, the given deadline in milliseconds
     * since Epoch has passed, or spuriously (i.e., returning for no
     * "reason"). Note: This operation is in the Unsafe class only
     * because <tt>unpark</tt> is, so it would be strange to place it
     * elsewhere.
     * 阻塞当前线程，在以下情况会返回：
     * 1、对应的unpark发生了
     * 2、对应的unpark已经发生
     * 3、当前线程被中断
     * 4、如果不是绝对的并且时间不为零，给定时间纳秒已经过去
     * 5、如果是绝对地，以毫秒表的时间戳截止时间已经过去
     * 6、虚假地（即，没有“理由”返回）？
     * 注意：此操作仅在Unsafe类中，因为<tt> unpark </ tt>在Unsafe中，将其放在其他位置会很奇怪。
     */
    public native void park(boolean isAbsolute, long time);

    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given <tt>nelem</tt> samples and
     * assigns to the elements of the given <tt>loadavg</tt> array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     * 获取分配给在不同时间段内的可用处理器的系统运行队列中的平均负载。 此方法检索给定的
     * <tt>nelem</tt>样本并分配给定<tt>loadavg</tt>数组的元素。系统最多应用3个样本，
     * 代表过去1,5,15分钟的平均值。
     *
     * @params loadavg an array of double of size nelems
     *         一个双倍大小的nelems数组
     * @params nelems the number of samples to be retrieved and
     *         must be 1 to 3.
     *         要检索的样本数量必须为1到3。
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     *         实际检索的样本数量; 如果无法获得平均负载，则返回-1。
     */
    public native int getLoadAverage(double[] loadavg, int nelems);

    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions
    // 以下部分包含基于CAS的Java实现，可用于不支持本机指令的平台

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     * 原子地将给定值相加到到给定对象的字段或数组元素。
     *
     * @param o object/array to update the field/element in
     *          用于更新字段/元素的对象/数组
     * @param offset field/element offset
     *               字段/元素偏移量
     * @param delta the value to add
     *             要添加的值
     * @return the previous value
     *         修改前的值
     * @since 1.8
     */
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     * 原子地将给定值相加到给定对象的字段或数组元素。
     *
     * @param o object/array to update the field/element in
     *          用于更新字段/元素的对象/数组
     * @param offset field/element offset
     *               字段/元素偏移量
     * @param delta the value to add
     *              要添加的值
     * @return the previous value
     *          修改前的值
     * @since 1.8
     */
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     * 原子地将给定值设置到给定对象的字段或数组元素。
     *
     * @param o object/array to update the field/element in
     *          用于更新字段/元素的对象/数组
     * @param offset field/element offset
     *               字段/元素偏移量
     * @param newValue new value
     *                 要更新的值
     * @return the previous value
     *         修改前的值
     * @since 1.8
     */
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     * 原子地将给定值设置到给定对象的字段或数组元素。
     *
     * @param o object/array to update the field/element in
     *          用于更新字段/元素的对象/数组
     * @param offset field/element offset
     *               字段/元素偏移量
     * @param newValue new value
     *                 要更新的值
     * @return the previous value
     *          修改前的值
     * @since 1.8
     */
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object <code>o</code> at the given <code>offset</code>.
     * 原子地将给定值设置到给定对象的字段或数组元素。
     *
     * @param o object/array to update the field/element in
     *          用于更新字段/元素的对象/数组
     * @param offset field/element offset
     *               字段/元素偏移量
     * @param newValue new value
     *                 要更新的值
     * @return the previous value
     *          修改前的值
     * @since 1.8
     */
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getObjectVolatile(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }


    /**
     * Ensures lack of reordering of loads before the fence
     * with loads or stores after the fence.
     * 确保栅栏前的存储没有重新排序，栅栏后的存储没有重新排序。
     * @since 1.8
     */
    public native void loadFence();

    /**
     * Ensures lack of reordering of stores before the fence
     * with loads or stores after the fence.
     * 确保栅栏前的存储没有重新排序，栅栏后的存储没有重新排序。
     * @since 1.8
     */
    public native void storeFence();

    /**
     * Ensures lack of reordering of loads or stores before the fence
     * with loads or stores after the fence.
     * 确保栅栏前的加载或存储没有重新排序，栅栏后的加载或存储没有重新排序。
     * @since 1.8
     */
    public native void fullFence();

    /**
     * Throws IllegalAccessError; for use by the VM.
     * 引发IllegalAccessError; 供VM使用。
     * @since 1.8
     */
    private static void throwIllegalAccessError() {
       throw new IllegalAccessError();
    }

}
