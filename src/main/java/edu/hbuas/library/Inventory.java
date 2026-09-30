package edu.hbuas.library;

/**
 * 借阅库存（规格 SPEC.md 第 2 节，I1～I5），必须线程安全。
 * TODO：由智能体在人类监督下实现。
 */
public final class Inventory {

    /** I1：增加馆藏。 */
    public void addCopies(String isbn, int n) {
        throw new UnsupportedOperationException("TODO");
    }

    /** I2：借出一本；成功返回 true，无可借副本返回 false。 */
    public boolean borrow(String isbn) {
        throw new UnsupportedOperationException("TODO");
    }

    /** I3：归还一本。 */
    public void giveBack(String isbn) {
        throw new UnsupportedOperationException("TODO");
    }

    /** I4：查询可借数，未登记返回 0。 */
    public int available(String isbn) {
        throw new UnsupportedOperationException("TODO");
    }
}
