package org.crythic.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.crythic.anno.Role;

public class RoleValidation implements ConstraintValidator<Role,String> {
    /**
     * 提供校验规则
     * @param s 要校验的字符串
     * @param constraintValidatorContext
     * @return 返回false校验不通过，返回true校验通过
     */
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(s==null)return false;
        if(s.equals("admin")||s.equals("customer"))return true;
        return false;
    }
}
