package me.philcali.db.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import me.philcali.db.annotation.Repository;
import me.philcali.template.annotation.Template;
import me.philcali.template.api.TemplateEngineProvider;
import me.philcali.template.api.TemplateEngineProviderSystem;

public abstract class RepositoryProcessor extends AbstractProcessor {
    private Messager log;
    private TemplateEngineProvider engines;

    protected boolean isAcceptableElement(final Element element) {
        return element.getKind() == ElementKind.INTERFACE && containsOutputType(element);
    }

    private boolean containsOutputType(final Element element) {
        return Optional.ofNullable(element.getAnnotation(Repository.class))
                .map(Repository::value)
                .map(Arrays::asList)
                .map(values -> values.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()))
                .map(values -> values.isEmpty() || values.stream().anyMatch(getId()::equalsIgnoreCase))
                .orElse(false);
    }

    protected abstract String getId();

    protected abstract void process(RepositoryProcessorContext context);

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.log = processingEnv.getMessager();
        this.engines = new TemplateEngineProviderSystem(getClass().getClassLoader());
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final TypeElement annotation : annotations) {
            log.printMessage(Kind.NOTE, "Processing annotation: ", annotation);
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            elements.stream().filter(this::isAcceptableElement).forEach(element -> {
                log.printMessage(Kind.NOTE, "Found repository element: ", element);
                process(RepositoryProcessorContextData.builder()
                        .withTemplateEngine(engines.get(element.getAnnotation(Template.class)))
                        .withAnnotation(annotation)
                        .withElement((TypeElement) element)
                        .withRoundEnvironment(roundEnv)
                        .build());
            });
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> supportedAnnotations = new HashSet<>();
        supportedAnnotations.add(Repository.class.getCanonicalName());
        return Collections.unmodifiableSet(supportedAnnotations);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
