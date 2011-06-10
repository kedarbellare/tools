package cc.refectorie.user.kedarb.tools.opts;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Opt {
    String name() default "";

    String gloss() default "";

    boolean required() default false;
}
