package com.github;

import org.junit.Test;

import java.util.List;

/**
 * @author zlei1
 * @version 1.0.0
 * @description:TODO
 * @date 2023/8/8 18:12
 */
public class TextOperationTests {
    @Test
    public void TextOperationTest() {
        TextOperation op1 = new TextOperation();
        op1.retain(5);
        op1.insert("world");

        TextOperation op2 = new TextOperation();
        op2.retain(5);
        op2.insert("!");

        List<TextOperation> textOperationPrime = TextOperation.transform(op1, op2);
        TextOperation op1Prime = textOperationPrime.get(0);
        TextOperation op2Prime = textOperationPrime.get(1);

        TextOperation op12Prime = op1.compose(op2Prime);
        TextOperation op21Prime = op2.compose(op1Prime);

        System.out.println(op12Prime.apply("hello"));
        System.out.println(op21Prime.apply("hello"));
    }
}
