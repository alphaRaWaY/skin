package org.mypetstore.mypetstore.anno;

import jakarta.validation.Constraint;
import org.mypetstore.mypetstore.validation.RoleValidation;
import jakarta.validation.Payload;
import java.lang.annotation.*;

//springboot的原注解
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {RoleValidation.class}//指定提供校验规则的类
)
public @interface Role {
    String message() default "用户身份只能是admin或者customer";
    //指定分组
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
