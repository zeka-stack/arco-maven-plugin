package dev.dong4j.arco.maven.plugin.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Reflection utility methods that can be used by ClassLoaderHandlers.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.05.01 20:21
 * @since 1.0.0
 */
@SuppressWarnings("all")
public final class ReflectionUtils {

    // In JDK 9+, could use MethodHandles.privateLookupIn
    // And then use getter lookup to get fields (which works even if there is no getter function defined):
    // https://stackoverflow.com/q/19135218/3950982

    /**
     * Constructor.
     *
     * @since 1.0.0
     */
    private ReflectionUtils() {
        // Cannot be constructed
    }

    /**
     * 获取给定对象或其任何超类的类中命名字段的值.
     * 如果在尝试读取字段时引发异常, 且throwException为true, 则将引发IllegalArgumentException并包装原因, 否则将返回null.
     * 如果传递了空对象, 则除非throwException为true, 否则返回null, 然后抛出IllegalArgumentException.
     *
     * @param cls            The class.
     * @param obj            The object, or null to get the value of a static field.
     * @param fieldName      The field name.
     * @param throwException If true, throw an exception if the field value could not be read.
     * @return The field value.
     * @throws IllegalArgumentException If the field value could not be read.
     * @since 1.0.0
     */
    private static Object getFieldVal(Class<?> cls, Object obj, String fieldName,
                                      boolean throwException) throws IllegalArgumentException {
        Field field = null;
        for (Class<?> currClass = cls; currClass != null; currClass = currClass.getSuperclass()) {
            try {
                field = currClass.getDeclaredField(fieldName);
                // Field found
                break;
            } catch (ReflectiveOperationException | SecurityException e) {
                // Try parent
            }
        }
        if (field == null) {
            if (throwException) {
                throw new IllegalArgumentException((obj == null ? "Static field " : "Field ") + "\"" + fieldName
                    + "\" not found or not accessible");
            }
        } else {
            try {
                field.setAccessible(true);
            } catch (RuntimeException e) { // JDK 9+: InaccessibleObjectException | SecurityException
                // Ignore
            }
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                if (throwException) {
                    throw new IllegalArgumentException(
                        "Can't read " + (obj == null ? "static " : "") + " field \"" + fieldName + "\": " + e);
                }
            }
        }
        return null;
    }

    /**
     * 获取给定对象或其任何超类的类中命名字段的值.
     * 如果在尝试读取字段时引发异常, 且throwException为true, 则将引发IllegalArgumentException并包装原因, 否则将返回null.
     * 如果传递了空对象, 则除非throwException为true, 否则返回null, 然后抛出IllegalArgumentException.
     *
     * @param obj            The object.
     * @param fieldName      The field name.
     * @param throwException If true, throw an exception if the field value could not be read.
     * @return The field value.
     * @throws IllegalArgumentException If the field value could not be read.
     * @since 1.0.0
     */
    public static Object getFieldVal(Object obj, String fieldName, boolean throwException)
        throws IllegalArgumentException {
        if (obj == null || fieldName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return getFieldVal(obj.getClass(), obj, fieldName, throwException);
    }

    /**
     * 获取给定类或其任何超类中已命名静态字段的值.
     * 如果在尝试读取字段值时引发异常, 且throwException为true, 则将引发IllegalArgumentException并包装原因, 否则将返回null.
     * 如果传递了一个空类引用, 则除非throwException为true, 否则返回null, 然后抛出IllegalArgumentException.
     *
     * @param cls            The class.
     * @param fieldName      The field name.
     * @param throwException If true, throw an exception if the field value could not be read.
     * @return The field value.
     * @throws IllegalArgumentException If the field value could not be read.
     * @since 1.0.0
     */
    public static Object getStaticFieldVal(Class<?> cls, String fieldName, boolean throwException)
        throws IllegalArgumentException {
        if (cls == null || fieldName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return getFieldVal(cls, null, fieldName, throwException);
    }

    /**
     * Iterate through implemented interfaces, top-down, then superclass to subclasses, top-down (since higher-up
     * superclasses and superinterfaces have the highest chance of being visible).
     *
     * @param cls the class
     * @return the reverse of the order in which method calls would be attempted by the JRE.
     * @since 1.0.0
     */
    private static List<Class<?>> getReverseMethodAttemptOrder(Class<?> cls) {
        List<Class<?>> reverseAttemptOrder = new ArrayList<>();

        // Iterate from class to its superclasses
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            reverseAttemptOrder.add(c);
        }

        // Find interfaces and superinterfaces implemented by this class or its superclasses
        Set<Class<?>> addedIfaces = new HashSet<>();
        LinkedList<Class<?>> ifaceQueue = new LinkedList<>();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            if (c.isInterface() && addedIfaces.add(c)) {
                ifaceQueue.add(c);
            }
            for (Class<?> iface : c.getInterfaces()) {
                if (addedIfaces.add(iface)) {
                    ifaceQueue.add(iface);
                }
            }
        }
        while (!ifaceQueue.isEmpty()) {
            Class<?> iface = ifaceQueue.remove();
            reverseAttemptOrder.add(iface);
            Class<?>[] superIfaces = iface.getInterfaces();
            if (superIfaces.length > 0) {
                for (Class<?> superIface : superIfaces) {
                    if (addedIfaces.add(superIface)) {
                        ifaceQueue.add(superIface);
                    }
                }
            }
        }
        return reverseAttemptOrder;
    }

    /**
     * Invoke the named method in the given object or its superclasses. If an exception is thrown while trying to
     * call the method, and throwException is true, then IllegalArgumentException is thrown wrapping the cause,
     * otherwise this will return null. If passed a null object, returns null unless throwException is true, then
     * throws IllegalArgumentException.
     *
     * @param cls            The class.
     * @param obj            The object, or null to invoke a static method.
     * @param methodName     The method name.
     * @param oneArg         If true, look for a method with one argument of type argType. If false, look for method with no
     *                       arguments.
     * @param argType        The type of the first argument to the method.
     * @param param          The value of the first parameter to invoke the method with.
     * @param throwException If true, throw an exception if the field value could not be read.
     * @return The field value.
     * @throws IllegalArgumentException If the field value could not be read.
     * @since 1.0.0
     */
    private static Object invokeMethod(Class<?> cls, Object obj, String methodName,
                                       boolean oneArg, Class<?> argType, Object param, boolean throwException)
        throws IllegalArgumentException {
        Method method = null;
        List<Class<?>> reverseAttemptOrder = getReverseMethodAttemptOrder(cls);
        for (int i = reverseAttemptOrder.size() - 1; i >= 0; i--) {
            Class<?> classOrInterface = reverseAttemptOrder.get(i);
            try {
                method = oneArg ? classOrInterface.getDeclaredMethod(methodName, argType)
                    : classOrInterface.getDeclaredMethod(methodName);
                // Method found
                break;
            } catch (ReflectiveOperationException | SecurityException e) {
                // Try next interface or superclass
            }
        }
        if (method == null) {
            if (throwException) {
                throw new IllegalArgumentException((obj == null ? "Static method " : "Method ") + "\"" + methodName
                    + "\" not found or not accesible");
            }
        } else {
            try {
                method.setAccessible(true);
            } catch (RuntimeException e) { // JDK 9+: InaccessibleObjectException | SecurityException
                // Ignore
            }
            try {
                return oneArg ? method.invoke(obj, param) : method.invoke(obj);
            } catch (IllegalAccessException e) {
                if (throwException) {
                    throw new IllegalArgumentException(
                        "Can't call " + (obj == null ? "static " : "") + "method \"" + methodName + "\": " + e);
                }
            } catch (InvocationTargetException e) {
                if (throwException) {
                    throw new IllegalArgumentException("Exception while invoking " + (obj == null ? "static " : "")
                        + "method \"" + methodName + "\"", e);
                }
            }
        }
        return null;
    }

    /**
     * Invoke the named method in the given object or its superclasses. If an exception is thrown while trying to
     * call the method, and throwException is true, then IllegalArgumentException is thrown wrapping the cause,
     * otherwise this will return null. If passed a null object, returns null unless throwException is true, then
     * throws IllegalArgumentException.
     *
     * @param obj            The object.
     * @param methodName     The method name.
     * @param throwException If true, throw an exception if the field value could not be read.
     * @return The field value.
     * @throws IllegalArgumentException If the field value could not be read.
     * @since 1.0.0
     */
    public static Object invokeMethod(Object obj, String methodName, boolean throwException)
        throws IllegalArgumentException {
        if (obj == null || methodName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return invokeMethod(obj.getClass(), obj, methodName, false, null, null, throwException);
    }

    /**
     * Invoke the named method in the given object or its superclasses. If an exception is thrown while trying to
     * call the method, and throwException is true, then IllegalArgumentException is thrown wrapping the cause,
     * otherwise this will return null. If passed a null object, returns null unless throwException is true, then
     * throws IllegalArgumentException.
     *
     * @param obj            The object.
     * @param methodName     The method name.
     * @param argType        The type of the method argument.
     * @param param          The parameter value to use when invoking the method.
     * @param throwException Whether to throw an exception on failure.
     * @return The result of the method invocation.
     * @throws IllegalArgumentException If the method could not be invoked.
     * @since 1.0.0
     */
    public static Object invokeMethod(Object obj, String methodName, Class<?> argType,
                                      Object param, boolean throwException) throws IllegalArgumentException {
        if (obj == null || methodName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return invokeMethod(obj.getClass(), obj, methodName, true, argType, param, throwException);
    }

    /**
     * Invoke the named static method. If an exception is thrown while trying to call the method, and throwException
     * is true, then IllegalArgumentException is thrown wrapping the cause, otherwise this will return null. If
     * passed a null class reference, returns null unless throwException is true, then throws
     * IllegalArgumentException.
     *
     * @param cls            The class.
     * @param methodName     The method name.
     * @param throwException Whether to throw an exception on failure.
     * @return The result of the method invocation.
     * @throws IllegalArgumentException If the method could not be invoked.
     * @since 1.0.0
     */
    public static Object invokeStaticMethod(Class<?> cls, String methodName,
                                            boolean throwException) throws IllegalArgumentException {
        if (cls == null || methodName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return invokeMethod(cls, null, methodName, false, null, null, throwException);
    }

    /**
     * Invoke the named static method. If an exception is thrown while trying to call the method, and throwException
     * is true, then IllegalArgumentException is thrown wrapping the cause, otherwise this will return null. If
     * passed a null class reference, returns null unless throwException is true, then throws
     * IllegalArgumentException.
     *
     * @param cls            The class.
     * @param methodName     The method name.
     * @param argType        The type of the method argument.
     * @param param          The parameter value to use when invoking the method.
     * @param throwException Whether to throw an exception on failure.
     * @return The result of the method invocation.
     * @throws IllegalArgumentException If the method could not be invoked.
     * @since 1.0.0
     */
    public static Object invokeStaticMethod(Class<?> cls, String methodName, Class<?> argType,
                                            Object param, boolean throwException) throws IllegalArgumentException {
        if (cls == null || methodName == null) {
            if (throwException) {
                throw new NullPointerException();
            } else {
                return null;
            }
        }
        return invokeMethod(cls, null, methodName, true, argType, param, throwException);
    }

    /**
     * Call Class.forName(className), but return null if any exception is thrown.
     *
     * @param className The class name to load.
     * @return The class of the requested name, or null if an exception was thrown while trying to load the class.
     * @since 1.0.0
     */
    public static Class<?> classForNameOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

}
