package me.philcali.db.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Repository {
    enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        PUT,
        LIST;
    }

    String[] value() default {};
    String prefix() default "";
    Key[] keys() default {};
    boolean partiallyDefined() default false;

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    @interface Method {
        Action value();
    }
}
