/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.common.types;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * An event happening during code gen. This is not a fast solution, it is only to be used when processing code, where
 * we can have a bit of an overhead!
 */
@Prototype.Blueprint
interface CodegenEventBlueprint {
    /**
     * Level can be used directly (command line tools), mapped to Maven level (maven plugins),
     * or mapped to diagnostics kind (annotation processing).
     * <p>
     * Mapping table:
     * <table>
     *     <th>
     *         <td>Level</td>
     *         <td>Maven log level</td>
     *         <td>APT Diagnostic.Kind</td>
     *     </th>
     *     <tr>
     *         <td>ERROR</td>
     *         <td>error</td>
     *         <td>ERROR</td>
     *     </tr>
     *     <tr>
     *         <td>WARNING</td>
     *         <td>warn</td>
     *         <td>WARNING</td>
     *     </tr>
     *     <tr>
     *         <td>INFO</td>
     *         <td>info</td>
     *         <td>NOTE</td>
     *     </tr>
     *     <tr>
     *         <td>DEBUG, TRACE</td>
     *         <td>debug</td>
     *         <td>N/A - only logged to logger</td>
     *     </tr>
     * </table>
     *
     * @return level to use, defaults to INFO
     */
    @Option.DefaultCode("System.Logger.Level.INFO")
    System.Logger.Level level();

    /**
     * Message to be delivered to the user.
     *
     * @return the message
     */
    String message();

    /**
     * Throwable if available.
     *
     * @return throwable
     */
    Optional<Throwable> throwable();

    /**
     * Additional information, such as source elements.
     * These may or may not be ignored by the final log destination.
     * <p>
     * Expected supported types:
     * <ul>
     *     <li>APT: {@code Element}, {@code AnnotationMirror}, {@code AnnotationValue}</li>
     *     <li>Classpath scanning: {@code ClassInfo}, {@code MethodInfo} etc.</li>
     *     <li>Any environment: {@link io.helidon.common.types.TypeName},
     *     {@link io.helidon.common.types.TypeInfo},
     *     or {@link io.helidon.common.types.TypedElementInfo}</li>
     * </ul>
     * @return list of objects causing this event to happen
     */
    @Option.Singular
    List<Object> objects();
}
