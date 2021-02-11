package io.helidon.di.processor;

import java.lang.annotation.Annotation;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

public class ClientVisitor implements TypeElementVisitor<Object, Object> {
    @NonNull
    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of("io.helidon.di.annotation.http.Client");
    }

    @Override
    public void visitConstructor(ConstructorElement element, VisitorContext context) {
        for (ParameterElement parameter : element.getParameters()) {
            validate(parameter, context);
        }

    }

    @Override
    public void visitMethod(MethodElement element, VisitorContext context) {
        for (ParameterElement parameter : element.getParameters()) {
            validate(parameter, context);
        }
    }

    @Override
    public void visitField(FieldElement element, VisitorContext context) {
        validate(element, context);
    }

    private void validate(TypedElement element, VisitorContext context) {
        AnnotationValue<Annotation> client = element.getAnnotation("io.helidon.di.annotation.http.Client");
        if (client != null) {
            if (!element.getType().getCanonicalName().equals("io.helidon.webclient.WebClient")) {
                context.fail("Element "
                                     + element.getName()
                                     + " is annotated with @Client, yet its type is not WebClient",
                             element);
            }
        }
    }

}
