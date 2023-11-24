package io.helidon.inject.codegen;

import java.util.Collection;
import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.spi.AnnotationMapper;
import io.helidon.codegen.spi.AnnotationMapperProvider;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

public class MapJavaxProvider implements AnnotationMapperProvider {
    private static final Set<String> PACKAGES = Set.of("javax.annotation",
                                                       "javax.enterprise",
                                                       "javax.inject");

    @Override
    public Set<String> supportedAnnotationPackages() {
        return PACKAGES;
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new JavaxAnnotationMapper();
    }

    private static class JavaxAnnotationMapper implements AnnotationMapper {
        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            for (String aPackage : PACKAGES) {
                if (annotation.typeName().packageName().startsWith(aPackage)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Collection<Annotation> mapAnnotation(CodegenContext ctx, Annotation original, ElementKind elementKind) {
            return Set.of(Annotation.builder(original)
                                  .typeName(mapTypeName(original.typeName()))
                                  .build());
        }

        private TypeName mapTypeName(TypeName typeName) {
            return TypeName.builder(typeName)
                    .packageName(mapPackage(typeName.packageName()))
                    .build();
        }

        private String mapPackage(String packageName) {
            if (packageName.startsWith("javax.")) {
                return "jakarta" + packageName.substring(5);
            }
            return packageName;
        }
    }
}
