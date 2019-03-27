package script.commands;

import javafx.util.Pair;
import script.interpreter.InterpretationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

public class CommandInterpreter {
    @TelescriptFunction(name = "+")
    public Long sum(Long first, Long second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(String first, String second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(String first, Long second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(Long first, String second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(String first, Boolean second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(Boolean first, String second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(Double first, String second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public String sum(String first, Double second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public Double sum(Double first, Double second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public Double sum(Double first, Long second) {
        return first + second;
    }

    @TelescriptFunction(name = "+")
    public Double sum(Long first, Double second) {
        return first + second;
    }

    @TelescriptFunction(name = "-")
    public Long subtract(Long first, Long second) {
        return first - second;
    }

    @TelescriptFunction(name = "*")
    public Long multiply(Long first, Long second) {
        return first * second;
    }

    @TelescriptFunction(name = "/")
    public Long divide(Long first, Long second) {
        return first / second;
    }

    @TelescriptFunction(name = "-")
    public Double subtract(Double first, Double second) {
        return first - second;
    }

    @TelescriptFunction(name = "*")
    public Double multiply(Double first, Double second) {
        return first * second;
    }

    @TelescriptFunction(name = "/")
    public Double divide(Double first, Double second) {
        return first / second;
    }

    @TelescriptFunction(name = "-")
    public Double subtract(Long first, Double second) {
        return first - second;
    }

    @TelescriptFunction(name = "*")
    public Double multiply(Long first, Double second) {
        return first * second;
    }

    @TelescriptFunction(name = "/")
    public Double divide(Long first, Double second) {
        return first / second;
    }

    @TelescriptFunction(name = "-")
    public Double subtract(Double first, Long second) {
        return first - second;
    }

    @TelescriptFunction(name = "*")
    public Double multiply(Double first, Long second) {
        return first * second;
    }

    @TelescriptFunction(name = "/")
    public Double divide(Double first, Long second) {
        return first / second;
    }

    @TelescriptFunction(name = "and")
    public Boolean and(Boolean first, Boolean second) {
        return first && second;
    }

    @TelescriptFunction(name = "or")
    public Boolean or(Boolean first, Boolean second) {
        return first || second;
    }

    @TelescriptFunction(name = "&")
    public Boolean logical_and(Boolean first, Boolean second) {
        return first & second;
    }

    @TelescriptFunction(name = "|")
    public Boolean logical_or(Boolean first, Boolean second) {
        return first | second;
    }

    @TelescriptFunction(name = "&")
    public Long logical_and(Long first, Long second) {
        return (long) ((int) (long) first & (int) (long) second);
    }

    @TelescriptFunction(name = "|")
    public Long logical_or(Double first, Double second) {
        return (long) ((int) (double) first | (int) (double) second);
    }

    @TelescriptFunction(name = ">")
    public Boolean more(Double first, Double second) {
        return first > second;
    }

    @TelescriptFunction(name = ">")
    public Boolean more(Long first, Long second) {
        return first > second;
    }

    @TelescriptFunction(name = ">")
    public Boolean more(Double first, Long second) {
        return first > second;
    }

    @TelescriptFunction(name = ">")
    public Boolean more(Long first, Double second) {
        return first > second;
    }

    @TelescriptFunction(name = "<")
    public Boolean less(Double first, Double second) {
        return first < second;
    }

    @TelescriptFunction(name = "<")
    public Boolean less(Long first, Long second) {
        return first < second;
    }

    @TelescriptFunction(name = "<")
    public Boolean less(Double first, Long second) {
        return first < second;
    }

    @TelescriptFunction(name = "<")
    public Boolean less(Long first, Double second) {
        return first < second;
    }

    @TelescriptFunction(name = ">=")
    public Boolean more_or_equal(Double first, Double second) {
        return first >= second;
    }

    @TelescriptFunction(name = ">=")
    public Boolean more_or_equal(Long first, Long second) {
        return first >= second;
    }

    @TelescriptFunction(name = ">=")
    public Boolean more_or_equal(Double first, Long second) {
        return first >= second;
    }

    @TelescriptFunction(name = ">=")
    public Boolean more_or_equal(Long first, Double second) {
        return first >= second;
    }

    @TelescriptFunction(name = "<=")
    public Boolean less_or_equal(Double first, Double second) {
        return first <= second;
    }

    @TelescriptFunction(name = "<=")
    public Boolean less_or_equal(Long first, Long second) {
        return first <= second;
    }

    @TelescriptFunction(name = "<=")
    public Boolean less_or_equal(Double first, Long second) {
        return first <= second;
    }

    @TelescriptFunction(name = "<=")
    public Boolean less_or_equal(Long first, Double second) {
        return first <= second;
    }

    @TelescriptFunction(name = "!=")
    public Boolean not_equal(Long first, Double second) {
        return !first.equals((long) (double) second);
    }

    @TelescriptFunction(name = "!=")
    public Boolean not_equal(Double first, Long second) {
        return !first.equals((double) second);
    }

    @TelescriptFunction(name = "!=")
    public Boolean not_equal(Long first, Long second) {
        return !first.equals(second);
    }

    @TelescriptFunction(name = "!=")
    public Boolean not_equal(Double first, Double second) {
        return !first.equals(second);
    }

    @TelescriptFunction(name = "!=")
    public Boolean not_equal(String first, String second) {
        return !first.equals(second);
    }

    @TelescriptFunction(name = "==")
    public Boolean equal(Long first, Double second) {
        return first.equals((long) (double) second);
    }

    @TelescriptFunction(name = "==")
    public Boolean equal(Double first, Long second) {
        return first.equals((double) second);
    }

    @TelescriptFunction(name = "==")
    public Boolean equal(Long first, Long second) {
        return first.equals(second);
    }

    @TelescriptFunction(name = "==")
    public Boolean equal(Double first, Double second) {
        return first.equals(second);
    }

    @TelescriptFunction(name = "==")
    public Boolean equal(String first, String second) {
        return first.equals(second);
    }

    @TelescriptFunction(name = "%")
    public Long mod(Long first, Long second) {
        return first % second;
    }

    @TelescriptFunction(name = "^")
    public Long xor(Long first, Long second) {
        return first ^ second;
    }

    @TelescriptFunction(name = ":", types = {Object.class, Object.class})
    public Pair<Object, Object> pair(Object key, Object value) {
        return new Pair<>(key, value);
    }

    @TelescriptFunction(name = "not")
    public Boolean not(Boolean condition) {
        return !condition;
    }

    @TelescriptFunction(name = "number")
    public Boolean number(String text) {
        return text.matches("^\\d+$");
    }

    @TelescriptFunction(name = "cast")
    public Object cast(Object obj, String cast_to) throws InterpretationException {
        if (cast_to.equalsIgnoreCase("long")) {
            if (obj == null)
                return 0L;
            else if (obj.getClass() == Long.class)
                return obj;
            else if (obj.getClass() == String.class)
                return Long.valueOf((String) obj);
            else if (obj.getClass() == Double.class)
                return (long) (double) obj;
            else if (obj.getClass() == Boolean.class)
                return (boolean) obj ? 1L : 0L;
            else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to Long");
        } else if (cast_to.equalsIgnoreCase("double")) {
            if (obj == null)
                return 0D;
            else if (obj.getClass() == Double.class)
                return obj;
            else if (obj.getClass() == Long.class)
                return (double) (long) obj;
            else if (obj.getClass() == String.class)
                return Double.valueOf((String) obj);
            else if (obj.getClass() == Boolean.class)
                return (boolean) obj ? 1D : 0D;
            else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to Double");
        } else if (cast_to.equalsIgnoreCase("string")) {
            if (obj == null)
                return "null";
            else if (obj.getClass() == Double.class || obj.getClass() == Long.class || obj.getClass() == Boolean.class)
                return String.valueOf(obj);
            else if (obj.getClass() == String.class)
                return obj;
            else if (obj.getClass() == Pair.class)
                return "'" + ((Pair) obj).getKey() + "':'" + ((Pair) obj).getValue() + "'";
            else if (obj instanceof List) {
                StringJoiner list = new StringJoiner(", ");
                for (Object object : (List) obj)
                    list.add((String) cast(object, "string"));
                return "[" + list.toString() + "]";
            } else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to String");
        } else if (cast_to.equalsIgnoreCase("boolean")) {
            if (obj == null)
                return false;
            else if (obj.getClass() == Boolean.class)
                return obj;
            else if (obj.getClass() == String.class)
                return Boolean.valueOf((String) obj);
            else if (obj.getClass() == Long.class) {
                if ((Long) obj == 0 || (Long) obj == 1)
                    return (Long) obj == 1 ? 1 : 0;
                else
                    throw new InterpretationException("Cannot cast " + obj + " (Long) type to Boolean");
            } else if (obj.getClass() == Double.class) {
                if ((Double) obj == 0 || (Double) obj == 1)
                    return (Double) obj == 1 ? 1 : 0;
                else
                    throw new InterpretationException("Cannot cast " + obj + " (Double) type to Boolean");
            } else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to Boolean");
        } else if (cast_to.equalsIgnoreCase("list")) {
            if (obj == null)
                 return null;
            else if (obj instanceof List)
                return obj;
            else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to List");
        } else if (cast_to.equalsIgnoreCase("pair")) {
            if (obj == null)
                return null;
            else if (obj.getClass() == Pair.class)
                return obj;
            else
                throw new InterpretationException("Cannot cast " + obj.getClass() + " type to Pair");
        } else
            throw new InterpretationException("Unknown type: " + cast_to);
    }

    @TelescriptFunction(name = "read")
    public Object read() {
        Scanner scanner = new Scanner(System.in);
        String text = scanner.next();
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException nfe) {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException exc) {
                try {
                    return Double.valueOf(text);
                } catch (NumberFormatException e) {
                    return text;
                }
            }
        }
    }

    @TelescriptFunction(name = "read")
    public Object read(String type) throws InterpretationException {
        return cast(new Scanner(System.in).next(), type);
    }

    @TelescriptFunction(name = "item")
    public Object item(ArrayList<Object> list, Long index) {
        return list.get((int) (long) index);
    }

    @TelescriptFunction(name = "length", types = {List.class})
    public Object length(List<Object> list) {
        return (long) list.size();
    }

    @TelescriptFunction(name = "not_null")
    public Object not_null(Object obj) {
        return obj != null;
    }

    @TelescriptFunction(name = "print", types = {Object.class})
    public void print(Object obj) throws InterpretationException {
        System.out.print(cast(obj, "string"));
    }

    @TelescriptFunction(name = "println", types = {Object.class})
    public void println(Object obj) throws InterpretationException {
        System.out.println(cast(obj, "string"));
    }

    @TelescriptFunction(name = "wait")
    public void wait(Long time) throws InterruptedException {
        Thread.sleep(time);
    }
}
