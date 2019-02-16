package me.philcali.db.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ExceptionTranslation {
    Class<? extends RuntimeException>[] source();
    Class<? extends RuntimeException> destination();
    String message() default "Failed to %s entity %s";
}
