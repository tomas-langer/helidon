package io.helidon.examples.di.basics;

import io.helidon.di.Helidon;

import io.micronaut.context.ApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext run = Helidon.run();
    }
}
