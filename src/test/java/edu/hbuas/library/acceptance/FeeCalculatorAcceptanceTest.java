package edu.hbuas.library.acceptance;

import edu.hbuas.library.FeeCalculator;
import edu.hbuas.library.ReaderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验收测试：由人类工程师依据 SPEC.md 编写，智能体不得修改（受钩子保护）。
 * 注意：BigDecimal.equals 同时比较数值与 scale，因此 "0.5" 与 "0.50" 不相等——这正是规格 F6 的要求。
 */
@DisplayName("FeeCalculator 验收测试（SPEC F1～F8）")
class FeeCalculatorAcceptanceTest {

    private final FeeCalculator calculator = new FeeCalculator();
    private static final LocalDate BORROW = LocalDate.of(2026, 1, 1); // 应还日 2026-01-31

    @ParameterizedTest(name = "[{index}] {0} 归还, {2}, 定价 {1} → {3}")
    @CsvSource({
            // 归还日期,   定价,    读者,    期望罚金      覆盖规则
            "2026-01-15, 100.00, STUDENT, 0.00",   // F2 未到期
            "2026-01-31, 100.00, STUDENT, 0.00",   // F2 应还日当天不算逾期（边界）
            "2026-02-01, 100.00, STUDENT, 0.50",   // F4 逾期第 1 天
            "2026-02-07, 100.00, STUDENT, 3.50",   // F4 第 7 天（一档上边界）
            "2026-02-08, 100.00, STUDENT, 4.50",   // F4 第 8 天（二档下边界）
            "2026-02-10, 100.00, STUDENT, 6.50",   // F4 第 10 天
            "2026-05-01, 100.00, STUDENT, 50.00",  // F5 封顶
            "2026-03-01, 30.00,  STUDENT, 15.00",  // F5 低价书封顶（29 天应为 25.50，封顶 15.00）
            "2026-02-10, 12.35,  STUDENT, 6.18",   // F5 封顶 12.35×0.5=6.175 → HALF_UP 6.18
            "2026-02-03, 100.00, TEACHER, 0.00",   // F3 教师宽限第 3 天
            "2026-02-04, 100.00, TEACHER, 0.50",   // F3 教师宽限后第 1 天
            "2026-02-11, 100.00, TEACHER, 4.50",   // F3+F4 教师逾期 11 天 → 计费 8 天
    })
    @DisplayName("SPEC 示例与边界值")
    void specExamples(LocalDate returnDate, BigDecimal price, ReaderType type, BigDecimal expected) {
        assertEquals(expected, calculator.calculate(BORROW, returnDate, price, type));
    }

    @Test
    @DisplayName("F1/F2：跨年与闰年按自然日计算")
    void leapYear() {
        // 2028 是闰年：2028-02-01 借出，应还 2028-03-02；2028-03-03 归还逾期 1 天
        assertEquals(new BigDecimal("0.50"),
                calculator.calculate(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 3, 3),
                        new BigDecimal("100.00"), ReaderType.STUDENT));
    }

    @Test
    @DisplayName("F6：结果 scale 恒为 2")
    void scaleIsTwo() {
        BigDecimal fee = calculator.calculate(BORROW, LocalDate.of(2026, 2, 2),
                new BigDecimal("80"), ReaderType.STUDENT);
        assertEquals(2, fee.scale());
    }

    @Test
    @DisplayName("F7：null 参数抛出 NullPointerException")
    void nullArguments() {
        BigDecimal p = new BigDecimal("10.00");
        LocalDate r = LocalDate.of(2026, 2, 1);
        assertThrows(NullPointerException.class, () -> calculator.calculate(null, r, p, ReaderType.STUDENT));
        assertThrows(NullPointerException.class, () -> calculator.calculate(BORROW, null, p, ReaderType.STUDENT));
        assertThrows(NullPointerException.class, () -> calculator.calculate(BORROW, r, null, ReaderType.STUDENT));
        assertThrows(NullPointerException.class, () -> calculator.calculate(BORROW, r, p, null));
    }

    @Test
    @DisplayName("F8：非法参数抛出 IllegalArgumentException")
    void illegalArguments() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BORROW, BORROW.minusDays(1), new BigDecimal("10.00"), ReaderType.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BORROW, BORROW.plusDays(40), BigDecimal.ZERO, ReaderType.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BORROW, BORROW.plusDays(40), new BigDecimal("-1"), ReaderType.STUDENT));
    }
}
