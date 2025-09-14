package com.oceangpt.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.temporal.Temporal;

/**
 * 日期范围验证器实现
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startDateFieldName;
    private String endDateFieldName;
    private String message;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateFieldName = constraintAnnotation.startDateField();
        this.endDateFieldName = constraintAnnotation.endDateField();
        this.message = constraintAnnotation.message();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null值由其他验证器处理
        }
        
        try {
            Field startDateField = value.getClass().getDeclaredField(startDateFieldName);
            Field endDateField = value.getClass().getDeclaredField(endDateFieldName);
            
            startDateField.setAccessible(true);
            endDateField.setAccessible(true);
            
            Object startDateValue = startDateField.get(value);
            Object endDateValue = endDateField.get(value);
            
            // 如果任一字段为null，跳过验证
            if (startDateValue == null || endDateValue == null) {
                return true;
            }
            
            // 支持LocalDate类型
            if (startDateValue instanceof LocalDate && endDateValue instanceof LocalDate) {
                LocalDate startDate = (LocalDate) startDateValue;
                LocalDate endDate = (LocalDate) endDateValue;
                
                boolean isValid = !startDate.isAfter(endDate);
                
                if (!isValid) {
                    // 自定义错误消息
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message)
                           .addPropertyNode(startDateFieldName)
                           .addConstraintViolation();
                }
                
                return isValid;
            }
            
            // 支持其他Temporal类型
            if (startDateValue instanceof Temporal && endDateValue instanceof Temporal) {
                Temporal startTemporal = (Temporal) startDateValue;
                Temporal endTemporal = (Temporal) endDateValue;
                
                // 简化比较，假设都是同类型
                if (startDateValue instanceof Comparable && endDateValue instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> startComparable = (Comparable<Object>) startDateValue;
                    boolean isValid = startComparable.compareTo(endDateValue) <= 0;
                    
                    if (!isValid) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(message)
                               .addPropertyNode(startDateFieldName)
                               .addConstraintViolation();
                    }
                    
                    return isValid;
                }
            }
            
            return true; // 不支持的类型，跳过验证
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在或无法访问，跳过验证
            return true;
        }
    }
}