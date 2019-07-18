/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * A small toolkit of classes that support lock-free thread-safe
 * programming on single variables.  In essence, the classes in this
 * package extend the notion of {@code volatile} values, fields, and
 * array elements to those that also provide an atomic conditional update
 * operation of the form:
 *
 * 类的小工具包，支持在单个变量上解除锁的线程安全编程。事实上，此包中的类可将 volatile 值、
 * 字段和数组元素的概念扩展到那些也提供原子条件更新操作的类，其形式如下：
 *
 *  <pre> {@code boolean compareAndSet(expectedValue, updateValue);}</pre>
 *
 * <p>This method (which varies in argument types across different
 * classes) atomically sets a variable to the {@code updateValue} if it
 * currently holds the {@code expectedValue}, reporting {@code true} on
 * success.  The classes in this package also contain methods to get and
 * unconditionally set values, as well as a weaker conditional atomic
 * update operation {@code weakCompareAndSet} described below.
 * 如果此方法（在不同的类间参数类型也不同）当前保持 expectedValue，则以原子方式将变量设置为
 * updateValue，并在成功时报告 true。此包中的类还包含获取并无条件设置值的方法，以及以下描述
 * 的较弱条件的原子更新操作 weakCompareAndSet。
 *
 * <p>The specifications of these methods enable implementations to
 * employ efficient machine-level atomic instructions that are available
 * on contemporary processors.  However on some platforms, support may
 * entail some form of internal locking.  Thus the methods are not
 * strictly guaranteed to be non-blocking --
 * a thread may block transiently before performing the operation.
 * 这些方法的规范使实现能够使用当代处理器上提供的高效机器级别原子指令。但是在某些平台上，
 * 该支持可能需要某种形式的内部锁。因而，该方法不能严格保证不被阻塞 - 执行操作之前可能暂时阻塞线程。
 *
 * <p>Instances of classes
 * {@link java.util.concurrent.atomic.AtomicBoolean},
 * {@link java.util.concurrent.atomic.AtomicInteger},
 * {@link java.util.concurrent.atomic.AtomicLong}, and
 * {@link java.util.concurrent.atomic.AtomicReference}
 * each provide access and updates to a single variable of the
 * corresponding type.  Each class also provides appropriate utility
 * methods for that type.  For example, classes {@code AtomicLong} and
 * {@code AtomicInteger} provide atomic increment methods.  One
 * application is to generate sequence numbers, as in:
 * 类 AtomicBoolean、AtomicInteger、AtomicLong 和 AtomicReference 的实例各自提供对相应类型
 * 单个变量的访问和更新。每个类也为该类型提供适当的实用工具方法。例如，类 AtomicLong 和 AtomicInteger
 * 提供了原子增量方法。一个应用程序将按以下方式生成序列号：
 *  <pre> {@code
 * class Sequencer {
 *   private final AtomicLong sequenceNumber
 *     = new AtomicLong(0);
 *   public long next() {
 *     return sequenceNumber.getAndIncrement();
 *   }
 * }}</pre>
 *
 * <p>It is straightforward to define new utility functions that, like
 * {@code getAndIncrement}, apply a function to a value atomically.
 * For example, given some transformation
 * 可以直接定义新的实用程序函数，例如{@code getAndIncrement}，将函数原子应用于值。
 * 例如，给定一些转变
 * <pre> {@code long transform(long input)}</pre>
 *
 * write your utility method as follows:
 * 编写工具程序方法如下：
 *  <pre> {@code
 * long getAndTransform(AtomicLong var) {
 *   long prev, next;
 *   do {
 *     prev = var.get();
 *     next = transform(prev);
 *   } while (!var.compareAndSet(prev, next));
 *   return prev; // return next; for transformAndGet
 * }}</pre>
 *
 * <p>The memory effects for accesses and updates of atomics generally
 * follow the rules for volatiles, as stated in
 * 原子访问和更新的内存效果一般遵循以下可变规则，，正如
 * The Java Language Specification, Third Edition (17.4 Memory Model) 中的声明：
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * The Java Language Specification (17.4 Memory Model)</a>:
 *
 * <ul>
 *
 *   <li> {@code get} has the memory effects of reading a
 * {@code volatile} variable.
 * get 具有读取 volatile 变量的内存效果。
 *
 *   <li> {@code set} has the memory effects of writing (assigning) a
 * {@code volatile} variable.
 * set 具有写入（分配）volatile 变量的内存效果。
 *
 *   <li> {@code lazySet} has the memory effects of writing (assigning)
 *   a {@code volatile} variable except that it permits reorderings with
 *   subsequent (but not previous) memory actions that do not themselves
 *   impose reordering constraints with ordinary non-{@code volatile}
 *   writes.  Among other usage contexts, {@code lazySet} may apply when
 *   nulling out, for the sake of garbage collection, a reference that is
 *   never accessed again.
 * {@code lazySet}具有写入（赋值）{@code volatile}变量的存储效应，除了允许对后续
 * （但不是先前的）内存操作进行重新排序，这些操作本身不会对普通非{@code volatile}写入
 * 施加重新排序约束。在其他用法上下文中，{@code lazySet}可能适用于为了垃圾收集而归零，
 * 从不再次访问的引用。
 *
 *   <li>{@code weakCompareAndSet} atomically reads and conditionally
 *   writes a variable but does <em>not</em>
 *   create any happens-before orderings, so provides no guarantees
 *   with respect to previous or subsequent reads and writes of any
 *   variables other than the target of the {@code weakCompareAndSet}.
 * weakCompareAndSet 以原子方式读取和有条件地写入变量但不 创建任何 happen-before 排序，
 * 因此不提供与除 weakCompareAndSet 目标外任何变量以前或后续读取或写入操作有关的任何保证。
 *
 *   <li> {@code compareAndSet}
 *   and all other read-and-update operations such as {@code getAndIncrement}
 *   have the memory effects of both reading and
 *   writing {@code volatile} variables.
 * compareAndSet 和所有其他的读取和更新操作（如 getAndIncrement）都有读取和写入 volatile
 * 变量的内存效果。
 * </ul>
 *
 * <p>In addition to classes representing single values, this package
 * contains <em>Updater</em> classes that can be used to obtain
 * {@code compareAndSet} operations on any selected {@code volatile}
 * field of any selected class.
 * 除了包含表示单个值的类之外，此包还包含<em> Updater </em>类，该类可用于获取任意选定类的任意
 * 选定{@code volatile}字段上的{@code compareAndSet}操作。
 *
 * {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater},
 * {@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater}, and
 * {@link java.util.concurrent.atomic.AtomicLongFieldUpdater} are
 * reflection-based utilities that provide access to the associated
 * field types.  These are mainly of use in atomic data structures in
 * which several {@code volatile} fields of the same node (for
 * example, the links of a tree node) are independently subject to
 * atomic updates.  These classes enable greater flexibility in how
 * and when to use atomic updates, at the expense of more awkward
 * reflection-based setup, less convenient usage, and weaker
 * guarantees.
 * AtomicReferenceFieldUpdater、AtomicIntegerFieldUpdater 和 AtomicLongFieldUpdater
 * 是基于反射的实用工具，可以提供对关联字段类型的访问。它们主要用于原子数据结构中，该结构中同一
 * 节点（例如，树节点的链接）的几个 volatile 字段都独立受原子更新控制。这些类在如何以及何时使
 * 用原子更新方面具有更大的灵活性，但相应的弊端是基于映射的设置较为拙笨、使用不太方便，而且在保
 * 证方面也较差。
 *
 * <p>The
 * {@link java.util.concurrent.atomic.AtomicIntegerArray},
 * {@link java.util.concurrent.atomic.AtomicLongArray}, and
 * {@link java.util.concurrent.atomic.AtomicReferenceArray} classes
 * further extend atomic operation support to arrays of these types.
 * These classes are also notable in providing {@code volatile} access
 * semantics for their array elements, which is not supported for
 * ordinary arrays.
 * AtomicIntegerArray、AtomicLongArray 和 AtomicReferenceArray 类进一步扩展了原子操作，
 * 对这些类型的数组提供了支持。这些类在为其数组元素提供 volatile 访问语义方面也引人注目，这对
 * 于普通数组来说是不受支持的。
 *
 * <p id="weakCompareAndSet">The atomic classes also support method
 * {@code weakCompareAndSet}, which has limited applicability.  On some
 * platforms, the weak version may be more efficient than {@code
 * compareAndSet} in the normal case, but differs in that any given
 * invocation of the {@code weakCompareAndSet} method may return {@code
 * false} <em>spuriously</em> (that is, for no apparent reason).  A
 * {@code false} return means only that the operation may be retried if
 * desired, relying on the guarantee that repeated invocation when the
 * variable holds {@code expectedValue} and no other thread is also
 * attempting to set the variable will eventually succeed.  (Such
 * spurious failures may for example be due to memory contention effects
 * that are unrelated to whether the expected and current values are
 * equal.)  Additionally {@code weakCompareAndSet} does not provide
 * ordering guarantees that are usually needed for synchronization
 * control.  However, the method may be useful for updating counters and
 * statistics when such updates are unrelated to the other
 * happens-before orderings of a program.  When a thread sees an update
 * to an atomic variable caused by a {@code weakCompareAndSet}, it does
 * not necessarily see updates to any <em>other</em> variables that
 * occurred before the {@code weakCompareAndSet}.  This may be
 * acceptable when, for example, updating performance statistics, but
 * rarely otherwise.
 * 原子类也支持 weakCompareAndSet 方法，该方法具有受限制的适用性。在某些平台上，
 * 弱版本在正常情况下可能比 compareAndSet 更有效，但不同的是 weakCompareAndSet
 * 方法的任何给定调用可能意外 返回 false（即没有明确的原因）。返回 false 仅意味着
 * 可以在需要时重新尝试操作，具体取决于重复执行调用的保证，当该变量保持 expectedValue
 * 并且没有其他线程也在尝试设置该变量时，最终将获得成功。（例如，这样的虚假失败可能是由
 * 于内存争用的结果，该争用与期望值和当前值是否相等无关）。 此外，weakCompareAndSet
 * 不提供通常需要同步控制的排序保证。但是，在这样的更新与程序的其他 happen-before 排序
 * 不相关时，该方法可用于更新计数器和统计数据。当一个线程看到对 weakCompareAndSet 导致
 * 的原子变量的更新时，它不一定能看到在 weakCompareAndSet 之前发生的对任何其他 变量的
 * 更新。例如，在更新性能统计数据时，这也许可以接受，但其他情况几乎不可以
 *
 *
 * <p>The {@link java.util.concurrent.atomic.AtomicMarkableReference}
 * class associates a single boolean with a reference.  For example, this
 * bit might be used inside a data structure to mean that the object
 * being referenced has logically been deleted.
 * AtomicMarkableReference 类将单个布尔值与引用关联起来。例如，可以在数据结构内部使
 * 用此位，这意味着引用的对象在逻辑上已被删除。
 *
 * The {@link java.util.concurrent.atomic.AtomicStampedReference}
 * class associates an integer value with a reference.  This may be
 * used for example, to represent version numbers corresponding to
 * series of updates.
 * AtomicStampedReference 类将整数值与引用关联起来。例如，这可用于表示与更新系列
 * 对应的版本号。
 *
 * <p>Atomic classes are designed primarily as building blocks for
 * implementing non-blocking data structures and related infrastructure
 * classes.  The {@code compareAndSet} method is not a general
 * replacement for locking.  It applies only when critical updates for an
 * object are confined to a <em>single</em> variable.
 * 设计原子类主要用作各种构造块，用于实现非阻塞数据结构和相关的基础结构类。
 * compareAndSet 方法不是锁的常规替换方法。仅当对象的重要更新限定于单个 变量时才应用它。
 *
 * <p>Atomic classes are not general purpose replacements for
 * {@code java.lang.Integer} and related classes.  They do <em>not</em>
 * define methods such as {@code equals}, {@code hashCode} and
 * {@code compareTo}.  (Because atomic variables are expected to be
 * mutated, they are poor choices for hash table keys.)  Additionally,
 * classes are provided only for those types that are commonly useful in
 * intended applications.  For example, there is no atomic class for
 * representing {@code byte}.  In those infrequent cases where you would
 * like to do so, you can use an {@code AtomicInteger} to hold
 * {@code byte} values, and cast appropriately.
 * 原子类不是 java.lang.Integer 和相关类的通用替换方法。它们不 定义诸如 hashCode 和 compareTo
 * 之类的方法。（因为原子变量是可变的，所以对于哈希表键来说，它们不是好的选择。）另外，
 * 仅为那些通常在预期应用程序中使用的类型提供类。例如，没有表示 byte 的原子类。这种情况不常见，
 * 如果要这样做，可以使用 AtomicInteger 来保持 byte 值，并进行适当的强制转换。也可以使用
 * Float.floatToIntBits 和 Float.intBitstoFloat 转换来保持 float 值，使用
 * Double.doubleToLongBits 和 Double.longBitsToDouble 转换来保持 double 值。
 *
 * You can also hold floats using
 * {@link java.lang.Float#floatToRawIntBits} and
 * {@link java.lang.Float#intBitsToFloat} conversions, and doubles using
 * {@link java.lang.Double#doubleToRawLongBits} and
 * {@link java.lang.Double#longBitsToDouble} conversions.
 *
 * @since 1.5
 */
package java.util.concurrent.atomic;
