package edu.hbuas.library;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 借阅库存（规格 SPEC.md 第 2 节，I1～I5），必须线程安全。
 *
 * <p>线程安全设计（I5）：
 * <ul>
 *   <li>每个 ISBN 对应一个 {@link CopyCounter}，其全部方法以计数器实例为监视器锁
 *       （{@code synchronized}）。"检查条件 → 修改计数"的复合操作（check-then-act）
 *       完整包裹在同一临界区内原子完成，同一 ISBN 的并发调用被串行化，因此：
 *       不会超借（{@code available} 减到 0 后 {@code borrow} 只能返回 false），
 *       且不变量 0 ≤ 可借数 ≤ 馆藏总数由构造保持（{@code borrow} 仅在 &gt;0 时递减，
 *       {@code giveBack} 仅在 &lt;总数 时递增，{@code addCopies} 同增两值）；</li>
 *   <li>计数器实例经 {@link ConcurrentHashMap} 存取，其 put/get 建立 happens-before
 *       关系，保证安全发布（不会看到半构造对象或过期字段）；字段只在锁内访问，
 *       无需 {@code volatile}；</li>
 *   <li>{@code computeIfAbsent} 保证每个键至多安装一个实例；即使映射函数因竞争被
 *       多次调用，新建空计数器无副作用，被丢弃者不影响正确性；</li>
 *   <li>I5 要求的不变量只涉及单个 ISBN 内部状态（不存在跨键不变量），故按 ISBN
 *       细粒度加锁即达到该键上的线性一致性，同时保留不同 ISBN 之间的并行性。</li>
 * </ul>
 *
 * <p>SPEC 未规定 null ISBN 的行为；本实现交由 {@link ConcurrentHashMap} 对 null 键
 * 自然抛出 {@link NullPointerException}（fail-fast）。
 */
public final class Inventory {

    /** ISBN → 馆藏计数器。 */
    private final ConcurrentMap<String, CopyCounter> counters = new ConcurrentHashMap<>();

    /**
     * I1：增加馆藏。
     *
     * @param isbn 图书 ISBN
     * @param n    增加册数，必须 ≥ 1
     * @throws IllegalArgumentException n &lt; 1 时抛出
     * @throws NullPointerException     isbn 为 null 时抛出
     */
    public void addCopies(String isbn, int n) {
        // I1：n ≥ 1，否则 IllegalArgumentException（0 与负数一并拒绝）
        if (n < 1) {
            throw new IllegalArgumentException("增加册数必须 ≥ 1: " + n);
        }
        counters.computeIfAbsent(isbn, k -> new CopyCounter()).add(n);
    }

    /**
     * I2：借出一本；有可借副本时可借数减 1 并返回 true，无可借副本返回 false。
     *
     * @param isbn 图书 ISBN
     * @return 是否借出成功
     * @throws IllegalArgumentException ISBN 未登记时抛出
     * @throws NullPointerException     isbn 为 null 时抛出
     */
    public boolean borrow(String isbn) {
        // I2：ISBN 未登记时抛出 IllegalArgumentException
        CopyCounter counter = counters.get(isbn);
        if (counter == null) {
            throw new IllegalArgumentException("ISBN 未登记: " + isbn);
        }
        return counter.borrow();
    }

    /**
     * I3：归还一本，可借数加 1；可借数已等于馆藏总数时抛出 IllegalStateException
     * （防止"还了没借过的书"）。
     *
     * @param isbn 图书 ISBN
     * @throws IllegalArgumentException ISBN 未登记时抛出
     * @throws IllegalStateException    可借数已等于馆藏总数时抛出
     * @throws NullPointerException     isbn 为 null 时抛出
     */
    public void giveBack(String isbn) {
        // I3：ISBN 未登记时抛出 IllegalArgumentException
        CopyCounter counter = counters.get(isbn);
        if (counter == null) {
            throw new IllegalArgumentException("ISBN 未登记: " + isbn);
        }
        counter.giveBack();
    }

    /**
     * I4：查询当前可借数；未登记的 ISBN 返回 0。
     *
     * @param isbn 图书 ISBN
     * @return 当前可借数
     * @throws NullPointerException isbn 为 null 时抛出
     */
    public int available(String isbn) {
        CopyCounter counter = counters.get(isbn);
        return counter == null ? 0 : counter.available();
    }

    /**
     * 单个 ISBN 的馆藏计数器。所有方法 {@code synchronized}：以本实例为监视器锁，
     * 使 check-then-act（先判断再增减）原子化，从而保证 I5 的不变量。
     */
    private static final class CopyCounter {

        /** 馆藏总数。 */
        private int total;

        /** 当前可借数。 */
        private int available;

        /** I1：总数与可借数同步增加 n（多次登记自然累加）。 */
        synchronized void add(int n) {
            total += n;
            available += n;
        }

        /** I2：有可借副本时减 1 并返回 true，否则返回 false；检查与递减在同一临界区，不超借。 */
        synchronized boolean borrow() {
            if (available == 0) {
                return false;
            }
            available--;
            return true;
        }

        /** I3：可借数加 1；已等于总数时抛出 IllegalStateException。 */
        synchronized void giveBack() {
            if (available == total) {
                throw new IllegalStateException("可借数已等于馆藏总数，不能归还未借出的书");
            }
            available++;
        }

        /** I4：返回当前可借数（锁内读取，保证看到最新一致状态）。 */
        synchronized int available() {
            return available;
        }
    }
}
