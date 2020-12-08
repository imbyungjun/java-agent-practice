package com.example;

import junit.framework.TestCase;
import org.junit.Test;

public class PersonTest extends TestCase {
    @Test
    public void testPersonToString() {
        new Person("imb", 29).sayHello();
    }
}