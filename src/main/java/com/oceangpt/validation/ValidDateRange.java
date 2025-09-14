package com.oceangpt.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 日期范围验证注解
 * 验证开始日期不能晚于结束日期
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    
    String message() default "开始日期不能晚于结束日期";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 开始日期字段名
     */
    String startDateField() default "startDate";
    
    /**
     * 结束日期字段名
     */
    String endDateField() default "endDate";
}