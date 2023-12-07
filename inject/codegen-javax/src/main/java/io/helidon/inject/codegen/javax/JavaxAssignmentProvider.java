package io.helidon.inject.codegen.javax;

import java.util.Optional;

import io.helidon.codegen.CodegenContext;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.inject.codegen.InjectionCodegenContext.Assignment;
import io.helidon.inject.codegen.spi.InjectAssignmentProvider;
import io.helidon.inject.codegen.spi.ProviderSupport;

import static io.helidon.inject.codegen.javax.JavaxTypes.INJECT_PROVIDER;

/**
 * {@link io.helidon.inject.codegen.spi.InjectAssignmentProvider} provider implementation for Jakarta based projects
 * with {@code javax} namespace.
 * This assignment provider adds support for Jakarta javax Provider injection points.
 */
public class JavaxAssignmentProvider implements InjectAssignmentProvider {
    /**
     * This constructor is required by {@link java.util.ServiceLoader}.
     *
     * @deprecated only for service loader
     */
    @Deprecated
    public JavaxAssignmentProvider() {
        super();
    }

    @Override
    public ProviderSupport create(CodegenContext ctx) {
        return new JavaxProviderMapper();
    }

    private static class JavaxProviderMapper implements ProviderSupport {

        @Override
        public Optional<Assignment> assignment(TypeName typeName, String valueSource) {
            // we support Provider<X>, Optional<Provider<X>>, and List<Provider<X>>
            if (typeName.isOptional()) {
                if (!typeName.typeArguments().isEmpty() && typeName.typeArguments().getFirst().equals(INJECT_PROVIDER)) {
                    return Optional.of(optionalProvider(typeName, valueSource));
                }
                return Optional.empty();
            }
            if (typeName.isList()) {
                if (!typeName.typeArguments().isEmpty() && typeName.typeArguments().getFirst().equals(INJECT_PROVIDER)) {
                    return Optional.of(listProvider(typeName, valueSource));
                }
                return Optional.empty();
            }
            if (typeName.genericTypeName().equals(INJECT_PROVIDER)) {
                return Optional.of(provider(typeName, valueSource));
            }
            return Optional.empty();
        }

        private Assignment optionalProvider(TypeName typeName, String valueSource) {
            TypeName actualType = typeName.typeArguments().getFirst() // Provider
                    .typeArguments().getFirst();

            TypeName replacementType = TypeName.builder(TypeNames.OPTIONAL)
                    .addTypeArgument(supplier(actualType))
                    .build();

            // Optional<Provider<NonSingletonService>> optionalProvider = // this code is generated always based on real type
            // ((Optional<Supplier<NonSingletonService>>) ctx.param(IP_PARAM_2))
            //                .map(it -> (Provider<NonSingletonService>) it::get)
            // ctx.param(IP_PARAM_0) is the "valueSource" provided to this method
            return new Assignment(replacementType,
                                  content -> content.addContent("((")
                                          .addContent(replacementType)
                                          .addContent(") ")
                                          .addContent(valueSource)
                                          .addContentLine(")")
                                          .increaseContentPadding()
                                          .increaseContentPadding()
                                          .addContent(".map(it -> (")
                                          .addContent(provider(actualType))
                                          .addContent(") it::get)")
                                          .decreaseContentPadding()
                                          .decreaseContentPadding());
        }

        private Assignment provider(TypeName typeName, String valueSource) {
            TypeName actualType = typeName.typeArguments().getFirst();
            TypeName replacementType = supplier(actualType);

            // Provider<NonSingletonService> provider =  // this code is generated always based on real type
            // ((Supplier<NonSingletonService>) ctx.param(IP_PARAM_0))::get;
            // ctx.param(IP_PARAM_0) is the "valueSource" provided to this method
            return new Assignment(replacementType,
                                  content -> content.addContent("((")
                                          .addContent(replacementType)
                                          .addContent(") ")
                                          .addContent(valueSource)
                                          .addContent(")::get"));
        }

        private Assignment listProvider(TypeName typeName, String valueSource) {
            TypeName actualType = typeName.typeArguments().getFirst() // Provider
                    .typeArguments().getFirst();

            TypeName replacementType = TypeName.builder(TypeNames.LIST)
                    .addTypeArgument(supplier(actualType))
                    .build();

            // List<Provider<NonSingletonService>> listOfProviders = // this code is generated always based on real type
            // ((List<Supplier<NonSingletonService>>) ctx.param(IP_PARAM_1))
            //                .stream()
            //                .map(it -> (Provider<NonSingletonService>) it::get)
            //                .toList();
            // ctx.param(IP_PARAM_1) is the "valueSource" provided to this method
            return new Assignment(replacementType,
                                  content -> content.addContent("((")
                                          .addContent(replacementType)
                                          .addContent(") ")
                                          .addContent(valueSource)
                                          .addContentLine(")")
                                          .increaseContentPadding()
                                          .increaseContentPadding()
                                          .addContentLine(".stream()")
                                          .addContent(".map(it -> (")
                                          .addContent(provider(actualType))
                                          .addContentLine(") it::get)")
                                          .addContent(".toList()")
                                          .decreaseContentPadding()
                                          .decreaseContentPadding());
        }

        private TypeName supplier(TypeName of) {
            return TypeName.builder(TypeNames.SUPPLIER)
                    .addTypeArgument(of)
                    .build();
        }

        private TypeName provider(TypeName of) {
            return TypeName.builder(INJECT_PROVIDER)
                    .addTypeArgument(of)
                    .build();
        }
    }
}
