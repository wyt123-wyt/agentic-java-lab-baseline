package edu.hbuas.library;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 借阅库存（规格 SPEC.md 第 2 节，I1～I5），必须线程安全。
 */
public final class Inventory {

    private final ConcurrentMap<String, CopyCounter> counters = new ConcurrentHashMap<>();

    /** I1：增加馆藏。 */
    public void addCopies(String isbn, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("增加册数必须 ≥ 1: " + n);
        }
        counters.computeIfAbsent(isbn, k -> new CopyCounter()).add(n);
    }

    /** I2：借出一本；成功返回 true，无可借副本返回 false。 */
    public boolean borrow(String isbn) {
        CopyCounter counter = counters.get(isbn);
        if (counter == null) {
            throw new IllegalArgumentException("ISBN 未登记: " + isbn);
        }
        return counter.borrow();
    }

    /** I3：归还一本。 */
    public void giveBack(String isbn) {
        CopyCounter counter = counters.get(isbn);
        if (counter == null) {
            throw new IllegalArgumentException("ISBN 未登记: " + isbn);
        }
        counter.giveBack();
    }

    /** I4：查询可借数，未登记返回 0。 */
    public int available(String isbn) {
        CopyCounter counter = counters.get(isbn);
        return counter == null ? 0 : counter.available();
    }

    private static final class CopyCounter {
        private int total;
        private int available;

        synchronized void add(int n) {
            total += n;
            available += n;
        }

        synchronized boolean borrow() {
            if (available == 0) {
                return false;
            }
            available--;
            return true;
        }

        synchronized void giveBack() {
            if (available == total) {
                throw new IllegalStateException("可借数已等于馆藏总数，不能归还未借出的书");
            }
            available++;
        }

        synchronized int available() {
            return available;
        }
    }
}

