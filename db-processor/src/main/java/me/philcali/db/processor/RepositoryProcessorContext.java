package me.philcali.db.processor;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import me.philcali.template.api.TemplateEngine;
import me.philcali.zero.lombok.annotation.Builder;
import me.philcali.zero.lombok.annotation.NonNull;

@Builder
public interface RepositoryProcessorContext {
    @NonNull
    RoundEnvironment getRoundEnvironment();
    @NonNull
    TypeElement getElement();
    @NonNull
    Element getAnnotation();
    @NonNull
    TemplateEngine getTemplateEngine();
}
