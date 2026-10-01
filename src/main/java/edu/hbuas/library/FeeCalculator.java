package edu.hbuas.library;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 逾期罚金计算器（规格 SPEC.md 第 1 节，F1～F8）。
 *
 * <p>实现说明：金额一律使用 {@link BigDecimal}（F6）；分段计费采用闭式公式，
 * 与 SPEC 示例及验收测试参考实现（逐日累加）对所有输入等价。
 * 校验顺序为先 F7 空值检查、后 F8 合法性检查（规格未定义两种违规同时出现时的优先级，
 * 此处按"空值优先"的惯例处理）。
 */
public final class FeeCalculator {

    /** 借期天数（F1）。 */
    private static final int LOAN_DAYS = 30;

    /** 教师宽限天数（F3）。 */
    private static final int TEACHER_GRACE_DAYS = 3;

    /** 一档计费天数上限：第 1～7 天（F4）。 */
    private static final int FIRST_TIER_DAYS = 7;

    /** 一档日罚金 0.50 元（F4）。 */
    private static final BigDecimal FIRST_TIER_DAILY = new BigDecimal("0.50");

    /** 二档日罚金 1.00 元（F4）。 */
    private static final BigDecimal SECOND_TIER_DAILY = new BigDecimal("1.00");

    /** 封顶比例：定价的 50%（F5）。 */
    private static final BigDecimal CAP_RATIO = new BigDecimal("0.5");

    /**
     * 计算逾期罚金。
     *
     * @param borrowDate 借出日期
     * @param returnDate 归还日期
     * @param bookPrice  图书定价（元），必须大于 0
     * @param readerType 读者类型
     * @return 罚金（元），scale = 2
     * @throws NullPointerException     任一参数为 null（F7）
     * @throws IllegalArgumentException 归还日期早于借出日期，或定价 ≤ 0（F8）
     */
    public BigDecimal calculate(LocalDate borrowDate, LocalDate returnDate,
                                BigDecimal bookPrice, ReaderType readerType) {
        // F7：任一参数为 null 时抛出 NullPointerException（先于 F8 检查）
        Objects.requireNonNull(borrowDate, "borrowDate");
        Objects.requireNonNull(returnDate, "returnDate");
        Objects.requireNonNull(bookPrice, "bookPrice");
        Objects.requireNonNull(readerType, "readerType");

        // F8：归还日期早于借出日期，或定价 ≤ 0
        if (returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("归还日期早于借出日期: " + returnDate + " < " + borrowDate);
        }
        if (bookPrice.signum() <= 0) {
            throw new IllegalArgumentException("定价必须大于 0: " + bookPrice);
        }

        // F1：应还日期 = 借出日期 + 30 天（LocalDate 自动处理跨月/跨年/闰年）
        LocalDate dueDate = borrowDate.plusDays(LOAN_DAYS);

        // F2：逾期天数 = 归还日期 − 应还日期（自然日），小于 0 取 0；应还日当天归还差值为 0，不算逾期
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));

        // F3：教师享 3 天宽限，学生无宽限
        long billableDays = readerType == ReaderType.TEACHER
                ? Math.max(0, overdueDays - TEACHER_GRACE_DAYS)
                : overdueDays;

        // F4：分段计费——第 1～7 天每天 0.50 元，第 8 天起每天 1.00 元
        BigDecimal fee = FIRST_TIER_DAILY.multiply(BigDecimal.valueOf(Math.min(billableDays, FIRST_TIER_DAYS)))
                .add(SECOND_TIER_DAILY.multiply(BigDecimal.valueOf(Math.max(0, billableDays - FIRST_TIER_DAYS))));

        // F5：封顶——罚金不超过定价的 50%
        BigDecimal cap = bookPrice.multiply(CAP_RATIO).setScale(2, RoundingMode.HALF_UP);

        // F6：结果 scale 恒为 2
        return fee.min(cap).setScale(2, RoundingMode.HALF_UP);
    }
}
