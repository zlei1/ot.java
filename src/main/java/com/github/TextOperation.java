package com.github;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zlei1
 * @version 1.0.0
 * @description: OT
 * @date 2023/8/8 16:37
 */
public class TextOperation {
    private List<Object> ops;
    private Integer baseLength;
    private Integer targetLength;

    public TextOperation() {
        this.ops = new ArrayList<>();
        this.baseLength = 0;
        this.targetLength = 0;
    }

    public Integer getBaseLength() {
        return this.baseLength;
    }

    public Integer getTargetLength() {
        return this.targetLength;
    }

    public List<Object> getOps() {
        return this.ops;
    }

    private static Boolean isRetainOp(Object object) {
        if (!(object instanceof Integer)) {
            return false;
        }

        return (Integer) object > 0;
    }

    private static Boolean isInsertOp(Object object) {
        return object instanceof String;
    }

    private static Boolean isDeleteOp(Object object) {
        if (!(object instanceof Integer)) {
            return false;
        }

        return (Integer) object < 0;
    }

    public TextOperation retain(Integer n) {
        if (0 == n) {
            return this;
        }

        this.baseLength += n;
        this.targetLength += n;

        Integer lastIndex = this.ops.size() - 1;
        Object last = null;
        if (lastIndex >= 0) {
            last = this.ops.get(lastIndex);
        }
        if (TextOperation.isRetainOp(last)) {
            this.ops.set(lastIndex, (Integer) last + n);
        } else {
            this.ops.add(n);
        }

        return this;
    }

    public TextOperation insert(String str) {
        if (null == str || "".equals(str)) {
            return this;
        }

        this.targetLength += str.length();

        Integer lastIndex = this.ops.size() - 1;
        Object last = null;
        if (lastIndex >= 0) {
            last = this.ops.get(lastIndex);
        }
        if (TextOperation.isInsertOp(last)) {
            str = last + str;
            this.ops.set(lastIndex, str);
        } else if (TextOperation.isDeleteOp(last)) {
            Integer secondToLastIndex = this.ops.size() - 2;
            Object secondToLast = null;
            if (secondToLastIndex >= 0) {
                secondToLast = this.ops.get(secondToLastIndex);
            }

            if (TextOperation.isInsertOp(secondToLast)) {
                str = last + str;
                this.ops.set(secondToLastIndex, str);
            } else {
                this.ops.add(secondToLastIndex, str);
            }
        } else {
            this.ops.add(str);
        }

        return this;
    }

    public TextOperation delete(Integer n) {
        if (0 == n) {
            return this;
        }

        if (n > 0) {
            n = -n;
        }

        this.baseLength -= n;

        Integer lastIndex = this.ops.size() - 1;
        Object last = null;
        if (lastIndex >= 0) {
            last = this.ops.get(lastIndex);
        }
        if (TextOperation.isDeleteOp(last)) {
            this.ops.set(lastIndex, (Integer) last + n);
        } else {
            this.ops.add(n);
        }

        return this;
    }

    @Override
    public String toString() {
        List<String> lines = this.ops.stream().map(object -> {
            if (TextOperation.isRetainOp(object)) {
                return String.format("retain %s", object);
            } else if (TextOperation.isInsertOp(object)) {
                return String.format("insert %s", object);
            } else if (TextOperation.isDeleteOp(object)) {
                return String.format("delete %s", object);
            } else {
                throw new RuntimeException("invalid operation");
            }
        }).collect(Collectors.toList());

        return String.join(",", lines);
    }

    public String apply(String str) {
        if (!this.baseLength.equals(str.length())) {
            throw new RuntimeException("The operation's base length must be equal to the string's length");
        }

        final String[] newStr = {""};
        final Integer[] strIndex = {0};

        this.ops.forEach(op -> {
            if (TextOperation.isRetainOp(op)) {
                if ((strIndex[0] + (Integer) op) > str.length()) {
                    throw new RuntimeException("Operation can't retain more characters than are left in the string");
                }

                newStr[0] += str.substring(strIndex[0], (Integer) op);
                strIndex[0] += (Integer) op;
            } else if (TextOperation.isInsertOp(op)) {
                newStr[0] += op;
            } else if (TextOperation.isDeleteOp(op)) {
                strIndex[0] -= (Integer) op;
            } else {
                throw new RuntimeException("invalid operation");
            }
        });

        if (!strIndex[0].equals(str.length())) {
            throw new RuntimeException("The operation didn't operate on the whole string");
        }

        return newStr[0];
    }

    public static List<TextOperation> transform(TextOperation textOperation1, TextOperation textOperation2) {
        if (!textOperation1.getBaseLength().equals(textOperation2.getBaseLength())) {
            throw new RuntimeException("Both operations have to have the same base length");
        }

        TextOperation op1Prime = new TextOperation();
        TextOperation op2Prime = new TextOperation();

        List<Object> opS1 = textOperation1.getOps();
        Integer opS1Size = opS1.size();
        List<Object> opS2 = textOperation2.getOps();
        Integer opS2Size = opS2.size();

        Integer i1 = 0;
        Integer i2 = 0;

        Object op1 = opS1.get(i1);
        Object op2 = opS2.get(i2);

        while (true) {
            if (null == op1 && null == op2) {
                break;
            }

            if (TextOperation.isInsertOp(op1)) {
                op1Prime.insert((String) op1);
                op2Prime.retain(((String) op1).length());
                i1 += 1;
                op1 = i1 >= opS1Size ? null : opS1.get(i1);
                continue;
            }

            if (TextOperation.isInsertOp(op2)) {
                op1Prime.retain(((String) op2).length());
                op2Prime.insert((String) op2);
                i2 += 1;
                op2 = i2 >= opS2Size ? null : opS2.get(i2);
                continue;
            }

            if (null == op1) {
                throw new RuntimeException("Cannot transform operations: first operation is too short");
            }
            if (null == op2) {
                throw new RuntimeException("Cannot transform operations: first operation is too long");
            }

            Object minL;

            if (TextOperation.isRetainOp(op1) && TextOperation.isRetainOp(op2)) {
                if ((Integer) op1 > (Integer) op2) {
                    minL = op2;
                    op1 = (Integer) op1 - (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1.equals(op2)) {
                    minL = op2;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    minL = op1;
                    op2 = (Integer) op2 - (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }

                op1Prime.retain((Integer) minL);
                op2Prime.retain((Integer) minL);
            } else if (TextOperation.isDeleteOp(op1) && TextOperation.isDeleteOp(op2)) {
                if (-(Integer) op1 > -(Integer) op2) {
                    op1 = (Integer) op1 - (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1.equals(op2)) {
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    op2 = (Integer) op2 - (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }
            } else if (TextOperation.isDeleteOp(op1) && TextOperation.isRetainOp(op2)) {
                if (-(Integer) op1 > (Integer) op2) {
                    minL = op2;
                    op1 = (Integer) op1 + (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op2.equals(-(Integer) op1)) {
                    minL = op2;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    minL = -(Integer) op1;
                    op2 = (Integer) op2 + (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }

                op1Prime.delete((Integer) minL);
            } else if (TextOperation.isRetainOp(op1) && TextOperation.isDeleteOp(op2)) {
                if ((Integer) op1 > -(Integer) op2) {
                    minL = -(Integer) op2;
                    op1 = (Integer) op1 + (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1.equals(-(Integer) op2)) {
                    minL = op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    minL = op1;
                    op2 = (Integer) op2 + (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }

                op2Prime.delete((Integer) minL);
            } else {
                throw new RuntimeException("The two operations aren't compatible");
            }
        }

        List<TextOperation> textOperationList = new ArrayList<>();
        textOperationList.add(op1Prime);
        textOperationList.add(op2Prime);
        return textOperationList;
    }

    public TextOperation compose(TextOperation textOperation2) {
        TextOperation textOperation1 = this;
        if (!textOperation1.getTargetLength().equals(textOperation2.getBaseLength())) {
            throw new RuntimeException("The base length of the second operation has to be the target length of the first operation");
        }

        TextOperation textOperation = new TextOperation();

        List<Object> opS1 = textOperation1.getOps();
        Integer opS1Size = opS1.size();
        List<Object> opS2 = textOperation2.getOps();
        Integer opS2Size = opS2.size();

        Integer i1 = 0;
        Integer i2 = 0;

        Object op1 = opS1.get(i1);
        Object op2 = opS2.get(i2);

        while (true) {
            if (null == op1 && null == op2) {
                break;
            }

            if (TextOperation.isDeleteOp(op1)) {
                textOperation.delete((Integer) op1);

                i1 += 1;
                op1 = i1 >= opS1Size ? null : opS1.get(i1);
                continue;
            }

            if (TextOperation.isInsertOp(op2)) {
                textOperation.insert((String) op2);

                i2 += 1;
                op2 = i2 >= opS2Size ? null : opS2.get(i2);
                continue;
            }

            if (null == op1) {
                throw new RuntimeException("Cannot compose operations: first operation is too short");
            }
            if (null == op2) {
                throw new RuntimeException("Cannot compose operations: first operation is too long");
            }

            if (TextOperation.isRetainOp(op1) && TextOperation.isRetainOp(op2)) {
                if ((Integer) op1 > (Integer) op2) {
                    textOperation.retain((Integer) op2);
                    op1 = (Integer) op1 - (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1.equals(op2)) {
                    textOperation.retain((Integer) op1);
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    textOperation.retain((Integer) op1);
                    op2 = (Integer) op2 - (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }
            } else if (TextOperation.isInsertOp(op1) && TextOperation.isDeleteOp(op2)) {
                Integer op1Length = ((String) op1).length();
                if (op1Length > -(Integer) op2) {
                    op1 = ((String) op1).substring(-(Integer) op2, op1Length);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1Length.equals(-(Integer) op2)) {
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    op2 = (Integer) op2 + op1Length;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }
            } else if (TextOperation.isInsertOp(op1) && TextOperation.isRetainOp(op2)) {
                Integer op1Length = ((String) op1).length();
                if (op1Length > (Integer) op2) {
                    textOperation.insert(((String) op1).substring(0, (Integer) op2));
                    op1 = ((String) op1).substring((Integer) op2, op1Length - (Integer) op2);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1Length.equals(op2)) {
                    textOperation.insert((String) op1);
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    textOperation.insert((String) op1);
                    op2 = (Integer) op2 - op1Length;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }
            } else if (TextOperation.isRetainOp(op1) && TextOperation.isDeleteOp(op2)) {
                if ((Integer) op1 > -(Integer) op2) {
                    textOperation.delete((Integer) op2);
                    op1 = (Integer) op1 + (Integer) op2;
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else if (op1.equals(-(Integer) op2)) {
                    textOperation.delete((Integer) op2);
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                    i2 += 1;
                    op2 = i2 >= opS2Size ? null : opS2.get(i2);
                } else {
                    textOperation.delete((Integer) op1);
                    op2 = (Integer) op2 + (Integer) op1;
                    i1 += 1;
                    op1 = i1 >= opS1Size ? null : opS1.get(i1);
                }
            } else {
               throw new RuntimeException(String.format("This shouldn't happen: op1: %s, op2: %s", op1, op2));
            }
        }

        return textOperation;
    }

}
