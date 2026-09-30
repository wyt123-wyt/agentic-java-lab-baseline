package edu.hbuas.library;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 逾期罚金计算器（规格 SPEC.md 第 1 节，F1～F8）。
 * TODO：由智能体在人类监督下实现。
 */
public final class FeeCalculator {

    /**
     * 计算逾期罚金。
     *
     * @param borrowDate 借出日期
     * @param returnDate 归还日期
     * @param bookPrice  图书定价（元），必须大于 0
     * @param readerType 读者类型
     * @return 罚金（元），scale = 2
     */
    public BigDecimal calculate(LocalDate borrowDate, LocalDate returnDate,
                                BigDecimal bookPrice, ReaderType readerType) {
        throw new UnsupportedOperationException("TODO");
    }
}
