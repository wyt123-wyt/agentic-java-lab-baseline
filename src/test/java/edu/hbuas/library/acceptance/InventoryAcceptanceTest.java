package edu.hbuas.library.acceptance;

import edu.hbuas.library.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验收测试：Inventory（SPEC I1～I5）。由人类编写，智能体不得修改。 */
@DisplayName("Inventory 验收测试（SPEC I1～I5）")
class InventoryAcceptanceTest {

    private static final String ISBN = "978-7-111-54742-6";

    @Test
    @DisplayName("I1/I2/I4：借完即止，未登记返回 0")
    void borrowUntilEmpty() {
        Inventory inv = new Inventory();
        assertEquals(0, inv.available(ISBN));
        inv.addCopies(ISBN, 2);
        assertTrue(inv.borrow(ISBN));
        assertTrue(inv.borrow(ISBN));
        assertFalse(inv.borrow(ISBN));
        assertEquals(0, inv.available(ISBN));
    }

    @Test
    @DisplayName("I1：多次登记累加；非法册数被拒绝")
    void addCopies() {
        Inventory inv = new Inventory();
        inv.addCopies(ISBN, 1);
        inv.addCopies(ISBN, 2);
        assertEquals(3, inv.available(ISBN));
        assertThrows(IllegalArgumentException.class, () -> inv.addCopies(ISBN, 0));
    }

    @Test
    @DisplayName("I2/I3：未登记 ISBN 借还均抛 IllegalArgumentException")
    void unknownIsbn() {
        Inventory inv = new Inventory();
        assertThrows(IllegalArgumentException.class, () -> inv.borrow("nope"));
        assertThrows(IllegalArgumentException.class, () -> inv.giveBack("nope"));
    }

    @Test
    @DisplayName("I3：不能归还未借出的书")
    void cannotReturnMoreThanBorrowed() {
        Inventory inv = new Inventory();
        inv.addCopies(ISBN, 1);
        assertThrows(IllegalStateException.class, () -> inv.giveBack(ISBN));
        assertTrue(inv.borrow(ISBN));
        inv.giveBack(ISBN);
        assertEquals(1, inv.available(ISBN));
    }

    @Test
    @DisplayName("I5：64 线程争抢 100 本，重复 50 轮，任何一轮都不得超借")
    void noOverBorrowUnderContention() throws Exception {
        for (int round = 0; round < 50; round++) {
            Inventory inv = new Inventory();
            inv.addCopies(ISBN, 100);
            AtomicInteger success = new AtomicInteger();
            runConcurrently(64, () -> {
                for (int i = 0; i < 10; i++) {           // 共 640 次借阅请求
                    if (inv.borrow(ISBN)) {
                        success.incrementAndGet();
                    }
                }
            });
            assertEquals(100, success.get(), "第 " + round + " 轮成功借出次数");
            assertEquals(0, inv.available(ISBN), "第 " + round + " 轮剩余可借数");
        }
    }

    @Test
    @DisplayName("I5：并发借还交错后，可借数回到馆藏总数")
    void borrowAndReturnInterleaved() throws Exception {
        Inventory inv = new Inventory();
        inv.addCopies(ISBN, 5);
        AtomicInteger violations = new AtomicInteger();
        runConcurrently(32, () -> {
            for (int i = 0; i < 2_000; i++) {
                if (inv.borrow(ISBN)) {
                    int a = inv.available(ISBN);
                    if (a < 0 || a > 5) {
                        violations.incrementAndGet();
                    }
                    inv.giveBack(ISBN);
                }
            }
        });
        assertEquals(0, violations.get(), "观察到越界的可借数");
        assertEquals(5, inv.available(ISBN));
    }

    /** 用 CountDownLatch 让所有线程在同一时刻起跑，最大化竞争窗口。 */
    private static void runConcurrently(int threads, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                task.run();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);   // 任务内的异常会在这里重新抛出
        }
        pool.shutdownNow();
    }
}
