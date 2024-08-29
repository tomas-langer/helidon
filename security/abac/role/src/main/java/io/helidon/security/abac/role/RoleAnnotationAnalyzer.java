/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.security.abac.role;

import java.util.List;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.security.providers.common.spi.AnnotationAnalyzer;

import jakarta.annotation.security.PermitAll;

/**
 * Implementation of {@link AnnotationAnalyzer} which checks for {@link PermitAll} annotation if
 * authentication is needed or not.
 */
@Weight(Weighted.DEFAULT_WEIGHT) // Helidon service loader loaded and ordered
public class RoleAnnotationAnalyzer implements AnnotationAnalyzer {
    private static final TypeName PERMIT_ALL = TypeName.create(PermitAll.class);
    private static final TypeName PERMIT_ALL_HELIDON = TypeName.create(RoleValidator.PermitAll.class);
    private static final TypeName ROLES_ALLOWED_JAKARTA = TypeName.create("jakarta.annotation.security.RolesAllowed");
    private static final TypeName ROLES_ALLOWED_JAVAX = TypeName.create("javax.annotation.security.RolesAllowed");
    private static final TypeName ROLE_HELIDON = TypeName.create(RoleValidator.Roles.class);
    private static final TypeName ROLES_HELIDON = TypeName.create(RoleValidator.RolesContainer.class);

    @Override
    public AnalyzerResponse analyze(Class<?> maybeAnnotated) {
        return AnalyzerResponse.abstain();
    }

    @Override
    public AnalyzerResponse analyze(TypeName applicationType, List<Annotation> annotations) {
        return analyze(annotations, AnalyzerResponse.abstain());
    }

    @Override
    public AnalyzerResponse analyze(TypeName endpointType, List<Annotation> annotations, AnalyzerResponse previousResponse) {
        return analyze(annotations, previousResponse);
    }

    @Override
    public AnalyzerResponse analyze(TypeName typeName, TypedElementInfo method, AnalyzerResponse previousResponse) {
        return analyze(method.annotations(), previousResponse);
    }

    private static AnalyzerResponse analyze(List<Annotation> annotations, AnalyzerResponse previousResponse) {
        if (hasAnnotation(annotations, PERMIT_ALL, PERMIT_ALL_HELIDON)) {
            // permit all wins
            return AnalyzerResponse.builder(previousResponse)
                    .authenticationResponse(Flag.OPTIONAL)
                    .authorizeResponse(Flag.OPTIONAL)
                    .build();
        }

        if (hasAnnotation(annotations, ROLES_ALLOWED_JAKARTA, ROLES_ALLOWED_JAVAX, ROLES_HELIDON, ROLE_HELIDON)) {
            // when roles allowed are defined, we require authentication (roles allowed is handled by authentication)
            return AnalyzerResponse.builder(previousResponse)
                    .authenticationResponse(Flag.REQUIRED)
                    .build();
        }

        return previousResponse;
    }

    private static boolean hasAnnotation(List<Annotation> annotations, TypeName... typeNames) {
        for (TypeName typeName : typeNames) {
            if (Annotations.findFirst(typeName, annotations).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
