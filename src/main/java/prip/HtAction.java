package prip;

import org.eclipse.jetty.http.MimeTypes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={FIELD, METHOD})
public @interface HtAction {
    String[] method() default "GET";

    String path();

    MimeTypes.Type mime() default MimeTypes.Type.TEXT_HTML_UTF_8;
}
