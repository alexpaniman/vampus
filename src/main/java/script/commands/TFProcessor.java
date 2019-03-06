package script.commands;

import script.interpreter.InterpretationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TFProcessor {
    private Object obj;

    public TFProcessor(Object obj) {
        this.obj = obj;
    }

    public Object func(String name, Object... objects) throws NoSuchMethodException, InterpretationException {
        for (Method method : obj.getClass().getDeclaredMethods())
            if (method.isAnnotationPresent(TelescriptFunction.class)) {
                TelescriptFunction tf = method.getAnnotation(TelescriptFunction.class);
                if (tf.name().equals(name)) {
                    Class[] types = tf.types();
                    Class[] args = method.getParameterTypes();
                    if (args.length != objects.length)
                        continue;
                    boolean equals = true;
                    for (int i = 0; i < args.length; i++)
                        if (types.length > 0) {
                            if (objects[i] != null && objects[i] != null && !types[i].isInstance(objects[i]))
                                equals = false;
                                break;
                        } else if (objects[i] != null && objects[i].getClass() != args[i]) {
                            equals = false;
                            break;
                        }
                    if (equals) {
                        method.setAccessible(true);
                        try {
                            return method.invoke(obj, objects);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new InterpretationException("Exception occurred when executing '" + method.getName() + "' function");
                        }
                    }
                }
            }
        throw new NoSuchMethodException();
    }

    public Object func(String name, Map<String, Object> objects) throws NoSuchMethodException, InterpretationException {
        for (Method method : obj.getClass().getDeclaredMethods())
            if (method.isAnnotationPresent(TelescriptFunction.class)) {
                TelescriptFunction tf = method.getAnnotation(TelescriptFunction.class);
                if (tf.name().equals(name) && tf.params().length > 0) {
                    String[] params = tf.params();
                    List<Object> args = new ArrayList<>();
                    int params_count = 0;
                    for (String param : params) {
                        Object value = objects.get(param);
                        if (value != null)
                            params_count++;
                        args.add(value);
                    }
                    if (params_count == objects.size()) {
                        method.setAccessible(true);
                        try {
                            return method.invoke(obj, args.toArray());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new InterpretationException("Exception occurred when executing '" + method.getName() + "' function");
                        }
                    }
                }
            }
        throw new NoSuchMethodException();
    }
}