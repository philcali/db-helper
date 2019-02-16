package me.philcali.db.dynamo.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Processor;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

import me.philcali.db.annotation.ExceptionTranslation;
import me.philcali.db.annotation.ExceptionTranslations;
import me.philcali.db.annotation.Repository;
import me.philcali.db.processor.RepositoryProcessor;
import me.philcali.db.processor.RepositoryProcessorContext;

@AutoService(Processor.class)
public class DynamoRepositoryProcessor extends RepositoryProcessor {
    private static final String TEMPLATE_NAME = "Repository";
    private static final String DYNAMO = "Dynamo";

    @Override
    protected String getId() {
        return DYNAMO;
    }

    @Override
    protected void process(final RepositoryProcessorContext context) {
        try {
            final Map<String, Object> templateContext = generateTemplateContext(context);
            final String className = String.format("%s.%s",
                    templateContext.get("packageName"),
                    templateContext.get("simpleName"));
            final JavaFileObject file = processingEnv.getFiler().createSourceFile(className, context.getElement());
            try (final Writer writer = new BufferedWriter(file.openWriter())) {
                writer.write(context.getTemplateEngine().apply(TEMPLATE_NAME, templateContext));
            }
        } catch (IOException ie) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "Failed to process dynamo repo: " + ie.getMessage(),
                    context.getElement());
        }
    }

    private Map<String, Object> createKeyMap(final Repository repository) {
        final Map<String, Object> keyMap = new HashMap<>();
        Arrays.stream(repository.keys()).forEach(key -> {
            Arrays.asList(key.partition(), key.sort()).stream().filter(s -> !s.isEmpty()).forEach(keyName -> {
                final Map<String, Object> keyContext = new HashMap<>();
                keyContext.put("name", keyName);
                keyMap.put(keyName, keyContext);
            });
        });
        return keyMap;
    }

    private Map<String, Object> generateTemplateContext(final RepositoryProcessorContext context) {
        final Repository repository = context.getElement().getAnnotation(Repository.class);
        final String packageName = processingEnv.getElementUtils().getPackageOf(context.getElement()).toString();
        final List<ExecutableElement> abstractMethods = abstractMethods(context);
        final Map<String, Object> keys = createKeyMap(repository);
        final Map<Repository.Action, ExecutableElement> methods = separateMethods(abstractMethods);
        final boolean isAbstract;
        if (abstractMethods.size() > methods.size() && repository.partiallyDefined()) {
            isAbstract = true;
        } else if (abstractMethods.size() > methods.size() && !repository.partiallyDefined()) {
            throw new RuntimeException("Repository is partially defined! Set the partiallyDefined to true to make"
                    + " an abstract repository " + abstractMethods + " defined " + methods);
        } else {
            isAbstract = false;
        }
        final Map<String, Object> templateContext = new HashMap<>();
        templateContext.put("abstract", isAbstract);
        templateContext.put("packageName", packageName);
        templateContext.put("simpleName", getSimpleName(isAbstract, context));
        templateContext.put("elementName", context.getElement().getSimpleName().toString());
        templateContext.put("keys", keys.values());
        methods.forEach((action, method) -> {
            final Map<String, Object> methodContext = new HashMap<>();
            TypeMirror type = method.getReturnType();
            if (method.getReturnType().getKind() == TypeKind.DECLARED) {
                final DeclaredType declared = (DeclaredType) method.getReturnType();
                type = declared.getTypeArguments().stream().findFirst()
                        .map(mapper -> (TypeMirror) mapper)
                        .orElseGet(method::getReturnType);
            }

            final List<Map<String, Object>> parameters = new ArrayList<>();
            method.getParameters().forEach(parameter -> {
                final Map<String, Object> parameterContext = new HashMap<>();
                parameterContext.put("name", parameter.getSimpleName());
                parameterContext.put("inputType", parameter.asType().toString());
                parameters.add(parameterContext);
            });
            methodContext.put("method", method.getSimpleName());
            methodContext.put("type", type.toString());
            methodContext.put("parameters", parameters);
            methodContext.put("exceptions", generateExceptions(context.getElement(), method));
            templateContext.put(action.name().toLowerCase(), methodContext);
        });
        return templateContext;
    }

    private List<Map<String, Object>> generateExceptions(final TypeElement element, final ExecutableElement method) {
        final List<Map<String, Object>> exceptions = new ArrayList<>();
        final Function<Element, Stream<AnnotationMirror>> generateTranslationMirrors = elem -> elem.getAnnotationMirrors().stream()
                .filter(mirror -> mirror.getAnnotationType().toString().equals(ExceptionTranslations.class.getCanonicalName()))
                .flatMap(mirror -> mirror.getElementValues().values().stream()
                        .map(value -> (List<AnnotationValue>) value.getValue())
                        .flatMap(values -> values.stream())
                        .map(value -> (AnnotationMirror) value.getValue()));
        final Function<Element, Stream<AnnotationMirror>> generateMirrors = elem -> elem.getAnnotationMirrors().stream()
                .filter(mirror -> mirror.getAnnotationType().toString().equals(ExceptionTranslation.class.getCanonicalName()))
                .map(mirror -> (AnnotationMirror) mirror);
        final Stream<AnnotationMirror> translations = Stream.concat(
                Stream.concat(generateMirrors.apply(method), generateTranslationMirrors.apply(method)),
                Stream.concat(generateMirrors.apply(element), generateTranslationMirrors.apply(element)));
        translations.forEach(translation -> {
            final Map<String, Object> exception = new HashMap<>();
            translation.getElementValues().forEach((exec, value) -> {
                if (value.getValue() instanceof List) {
                    exception.put(exec.getSimpleName().toString(), ((List) value.getValue()).stream()
                            .map(v -> v.toString().replace(".class", ""))
                            .collect(Collectors.toList()));
                } else {
                    exception.put(exec.getSimpleName().toString(), value.getValue().toString().replace(".class", ""));
                }
            });
            exception.putIfAbsent("message", "Failed to %s entity %s");
            exceptions.add(exception);
        });
        return exceptions;
    }

    private String getSimpleName(final boolean isAbstract, final RepositoryProcessorContext context) {
        final StringBuilder builder = new StringBuilder();
        if (isAbstract) {
            builder.append("Abstract");
        }
        return builder.append(context.getElement().getSimpleName().toString())
                .append(getId())
                .toString();
    }

    private List<ExecutableElement> abstractMethods(final RepositoryProcessorContext context) {
        return context.getElement().getEnclosedElements().stream()
                .filter(method -> !method.getModifiers().contains(Modifier.DEFAULT))
                .map(method -> (ExecutableElement) method)
                .sorted((methodA, methodB) -> Boolean.compare(
                        Objects.nonNull(methodA.getAnnotation(Repository.Method.class)),
                        Objects.nonNull(methodB.getAnnotation(Repository.Method.class))))
                .collect(Collectors.toList());
    }

    private Map<Repository.Action, ExecutableElement> separateMethods(final List<ExecutableElement> methods) {
        final Map<Repository.Action, ExecutableElement> separatedMethods = new HashMap<>();
        methods.forEach(method -> {
            final Repository.Action annotation = Optional.ofNullable(method.getAnnotation(Repository.Method.class))
                    .map(Repository.Method::value)
                    .orElseGet(() -> {
                        for (final Repository.Action action : Repository.Action.values()) {
                            if (method.getSimpleName().toString().startsWith(action.name().toLowerCase())) {
                                return action;
                            }
                        }
                        return null;
                    });
            if (Objects.nonNull(annotation)) {
                if (separatedMethods.containsKey(annotation)) {
                    processingEnv.getMessager().printMessage(Kind.WARNING, "Repository." + annotation
                            + " was already defined, skipping method: ", method);
                }
                separatedMethods.putIfAbsent(annotation, method);
            }
        });
        return separatedMethods;
    }
}
