package me.philcali.db.dynamo.local.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamoDBArgs {
    boolean skipIfUnavailable() default true;
    int validationTimeout() default 1000;
    String[] value() default "";
}
