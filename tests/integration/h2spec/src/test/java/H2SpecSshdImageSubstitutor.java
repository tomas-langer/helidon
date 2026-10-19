/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * Opt-in SSH helper selection for environments that build their own test image.
 */
public final class H2SpecSshdImageSubstitutor extends ImageNameSubstitutor {
    private static final DockerImageName ORIGINAL = DockerImageName.parse("testcontainers/sshd:1.3.0");

    private final DockerImageName replacement;

    /**
     * Creates the substitutor selected by Testcontainers configuration.
     */
    public H2SpecSshdImageSubstitutor() {
        String image = System.getenv("HELIDON_TEST_SSHD_IMAGE");
        if (image == null || image.isBlank()) {
            throw new IllegalStateException("HELIDON_TEST_SSHD_IMAGE is required when selecting the h2spec SSH substitutor");
        }
        replacement = DockerImageName.parse(image).asCompatibleSubstituteFor(ORIGINAL);
    }

    @Override
    public DockerImageName apply(DockerImageName original) {
        if (!original.getRepository().equals(ORIGINAL.getRepository())) {
            return original;
        }
        if (!original.asCanonicalNameString().equals(ORIGINAL.asCanonicalNameString())) {
            throw new IllegalStateException("Validate the SSH helper against the new Testcontainers image before substituting: "
                                                    + original);
        }
        return replacement;
    }

    @Override
    protected String getDescription() {
        return "h2spec SSH helper selected by HELIDON_TEST_SSHD_IMAGE";
    }
}
