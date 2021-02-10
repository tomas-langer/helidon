package io.helidon.examples.di.basics;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class Consumer {
    public Consumer(List<MyFactory.Produced> produced) {
        System.out.println(produced);
    }
}
