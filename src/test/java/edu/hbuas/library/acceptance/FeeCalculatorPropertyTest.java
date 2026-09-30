package edu.hbuas.library.acceptance;

import edu.hbuas.library.FeeCalculator;
import edu.hbuas.library.ReaderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 性质测试 + 差分测试（不依赖第三方库，用固定种子的随机数生成输入，失败可复现）。
 *
 * 差分测试思路：人类写一个"慢但显然正确"的逐日累加版本作为测试预言（oracle），
 * 与智能体写的实现在上万组随机输入上逐一比对。
 */
@DisplayName("FeeCalculator 性质测试与差分测试")
class FeeCalculatorPropertyTest {

    private static final long SEED = 20260928L;
    private static final int CASES = 20_000;
    private final FeeCalculator calculator = new FeeCalculator();

    /** 人类编写的参考实现：逐日累加，直接对照规格条文，不追求效率。 */
    static BigDecimal oracle(LocalDate borrow, LocalDate ret, BigDecimal price, ReaderType type) {
        LocalDate due = borrow.plusDays(30);
        int overdue = 0;
        for (LocalDate d = due.plusDays(1); !d.isAfter(ret); d = d.plusDays(1)) {
            overdue++;
        }
        int grace = type == ReaderType.TEACHER ? 3 : 0;
        BigDecimal fee = BigDecimal.ZERO;
        for (int day = 1; day <= overdue - grace; day++) {
            fee = fee.add(day <= 7 ? new BigDecimal("0.50") : new BigDecimal("1.00"));
        }
        BigDecimal cap = price.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
        return fee.min(cap).setScale(2, RoundingMode.HALF_UP);
    }

    private record Input(LocalDate borrow, LocalDate ret, BigDecimal price, ReaderType type) { }

    private static Input randomInput(Random rnd) {
        LocalDate borrow = LocalDate.of(2024, 1, 1).plusDays(rnd.nextInt(1500));
        LocalDate ret = borrow.plusDays(rnd.nextInt(200));                  // 0～199 天后归还
        BigDecimal price = BigDecimal.valueOf(1 + rnd.nextInt(30000), 2);  // 0.01～300.00 元
        ReaderType type = rnd.nextBoolean() ? ReaderType.STUDENT : ReaderType.TEACHER;
        return new Input(borrow, ret, price, type);
    }

    @Test
    @DisplayName("差分：实现与人类参考实现在 20000 组随机输入上完全一致")
    void matchesOracle() {
        Random rnd = new Random(SEED);
        for (int i = 0; i < CASES; i++) {
            Input in = randomInput(rnd);
            BigDecimal expected = oracle(in.borrow(), in.ret(), in.price(), in.type());
            BigDecimal actual = calculator.calculate(in.borrow(), in.ret(), in.price(), in.type());
            assertEquals(expected, actual, () -> "反例: " + in);
        }
    }

    @Test
    @DisplayName("性质：0 ≤ 罚金 ≤ 定价的一半，且 scale = 2")
    void boundedAndScaled() {
        Random rnd = new Random(SEED + 1);
        for (int i = 0; i < CASES; i++) {
            Input in = randomInput(rnd);
            BigDecimal fee = calculator.calculate(in.borrow(), in.ret(), in.price(), in.type());
            BigDecimal cap = in.price().multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
            assertTrue(fee.signum() >= 0 && fee.compareTo(cap) <= 0, () -> "越界: " + in + " → " + fee);
            assertEquals(2, fee.scale(), () -> "scale 错误: " + in);
        }
    }

    @Test
    @DisplayName("性质：越晚归还罚金不减少（单调性）；教师罚金不高于学生")
    void monotonicAndTeacherNotHigher() {
        Random rnd = new Random(SEED + 2);
        for (int i = 0; i < CASES; i++) {
            Input in = randomInput(rnd);
            BigDecimal today = calculator.calculate(in.borrow(), in.ret(), in.price(), in.type());
            BigDecimal tomorrow = calculator.calculate(in.borrow(), in.ret().plusDays(1), in.price(), in.type());
            assertTrue(tomorrow.compareTo(today) >= 0, () -> "单调性被破坏: " + in);

            BigDecimal student = calculator.calculate(in.borrow(), in.ret(), in.price(), ReaderType.STUDENT);
            BigDecimal teacher = calculator.calculate(in.borrow(), in.ret(), in.price(), ReaderType.TEACHER);
            assertTrue(teacher.compareTo(student) <= 0, () -> "教师罚金高于学生: " + in);
        }
    }
}
