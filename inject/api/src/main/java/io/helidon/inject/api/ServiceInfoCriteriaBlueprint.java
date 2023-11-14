/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeName;

/**
 * Criteria to discover services.
 *
 * @see Services
 */
@Prototype.Blueprint
@Prototype.CustomMethods(ServiceInfoCriteriaBlueprint.CustomMethods.class)
interface ServiceInfoCriteriaBlueprint {

    /**
     * The managed service implementation type name.
     *
     * @return the service type name
     */
    Optional<TypeName> serviceTypeName();

    /**
     * The managed service assigned Scope's.
     *
     * @return the service scope type name
     */
    @Option.Singular
    Set<TypeName> scopeTypeNames();

    /**
     * The managed service assigned Qualifier's.
     *
     * @return the service qualifiers
     */
    @Option.Singular
    Set<Qualifier> qualifiers();

    /**
     * The managed services advertised types (i.e., typically its interfaces).
     *
     * @return the service contracts implemented
     * @see ExternalContracts
     */
    @Option.Singular("contractImplemented")
    Set<TypeName> contractsImplemented();

    /**
     * The optional {@link RunLevel} ascribed to the service.
     *
     * @return the service's run level
     */
    Optional<Integer> runLevel();

    /**
     * Weight that was declared on the type itself.
     *
     * @return the declared weight
     */
    Optional<Double> weight();

    /**
     * The managed services external contracts / interfaces. These should also be contained within
     * {@link #contractsImplemented()}. External contracts are from other modules other than the module containing
     * the implementation typically.
     *
     * @see ExternalContracts
     * @return the service external contracts implemented
     */
    @Option.Singular("externalContractImplemented")
    Set<TypeName> externalContractsImplemented();

    /**
     * Determines whether this service info matches the criteria for injection.
     * Matches is a looser form of equality check than {@code equals()}. If a service matches criteria
     * it is generally assumed to be viable for assignability.
     *
     * @param criteria the criteria to compare against
     * @return true if the criteria provided matches this instance
     */
    default boolean matches(ServiceInfoCriteriaBlueprint criteria) {
        return matchesContracts(criteria)
                && scopeTypeNames().containsAll(criteria.scopeTypeNames())
                && Qualifiers.matchesQualifiers(qualifiers(), criteria.qualifiers())
                && matches(runLevel(), criteria.runLevel());
                //                && matchesWeight(this, criteria) -- intentionally not checking weight here!
    }

    /**
     * Determines whether this service info criteria matches the service descriptor.
     * Matches is a looser form of equality check than {@code equals()}. If a service matches criteria
     * it is generally assumed to be viable for assignability.
     *
     * @param descriptor to compare with
     * @return true if this criteria matches the service descriptor
     */
    default boolean matches(ServiceDescriptor<?> descriptor) {
        if (this == InjectionServices.EMPTY_CRITERIA) {
            return true;
        }

        boolean matches = matches(descriptor.serviceType(), this.serviceTypeName());
        if (matches && this.serviceTypeName().isEmpty()) {
            matches = descriptor.contracts().containsAll(this.contractsImplemented())
                    || this.contractsImplemented().contains(descriptor.serviceType());
        }
        return matches
                && descriptor.scopes().containsAll(this.scopeTypeNames())
                && Qualifiers.matchesQualifiers(descriptor.qualifiers(), this.qualifiers())
                && matchesWeight(descriptor, this)
                && matches(descriptor.runLevel(), this.runLevel());
    }

    /**
     * Determines whether the provided criteria match just the contracts portion of the provided criteria. Note that
     * it is expected any external contracts have been consolidated into the regular contract section.
     *
     * @param criteria the criteria to compare against
     * @return true if the criteria provided matches this instance from only the contracts point of view
     */
    default boolean matchesContracts(ServiceInfoCriteriaBlueprint criteria) {
        if (criteria == InjectionServices.EMPTY_CRITERIA) {
            return true;
        }

        boolean matches = matches(serviceTypeName(), criteria.serviceTypeName());
        if (matches && criteria.serviceTypeName().isEmpty()) {
            matches = contractsImplemented().containsAll(criteria.contractsImplemented());
        }
        return matches;
    }

    private static boolean matchesWeight(ServiceDescriptor<?> src,
                                         ServiceInfoCriteriaBlueprint criteria) {
        if (criteria.weight().isEmpty()) {
            return true;
        }

        Double srcWeight = src.weight();
        return (srcWeight.compareTo(criteria.weight().get()) <= 0);
    }

    private static boolean matches(Object src,
                                   Optional<?> criteria) {

        return criteria.map(o -> Objects.equals(src, o)).orElse(true);

    }

    final class CustomMethods {
        private CustomMethods() {
        }

        /**
         * The managed services advertised types (i.e., typically its interfaces).
         *
         * @param builder builder instance
         * @param contract the service contracts implemented
         * @see #contractsImplemented()
         */
        @Prototype.BuilderMethod
        static void addContractImplemented(ServiceInfoCriteria.BuilderBase<?, ?> builder, Class<?> contract) {
            builder.addContractImplemented(TypeName.create(contract));
        }
    }
}
