package eu.fx3.plugins.totaldeathmessages.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ReflectionUtil {
    private static String versionString;
    private static Map<String, Class<?>> loadedNMSClasses = new HashMap();
    private static Map<String, Class<?>> loadedOBCClasses = new HashMap();
    private static Map<Class<?>, Map<String, Method>> loadedMethods = new HashMap();
    private static Map<Class<?>, Map<String, Field>> loadedFields = new HashMap();

    public ReflectionUtil() {
    }

    public static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf(46) + 1) + ".";
        }

        return versionString;
    }

    public static Class<?> getNMSClass(String nmsClassName) {
        if (loadedNMSClasses.containsKey(nmsClassName)) {
            return (Class)loadedNMSClasses.get(nmsClassName);
        } else {
            String clazzName = "net.minecraft.server." + getVersion() + nmsClassName;

            Class clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (Throwable var4) {
                var4.printStackTrace();
                return (Class)loadedNMSClasses.put(nmsClassName, null);
            }

            loadedNMSClasses.put(nmsClassName, clazz);
            return clazz;
        }
    }

    public static synchronized Class<?> getOBCClass(String obcClassName) {
        if (loadedOBCClasses.containsKey(obcClassName)) {
            return (Class)loadedOBCClasses.get(obcClassName);
        } else {
            String clazzName = "org.bukkit.craftbukkit." + getVersion() + obcClassName;

            Class clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (Throwable var4) {
                var4.printStackTrace();
                loadedOBCClasses.put(obcClassName, null);
                return null;
            }

            loadedOBCClasses.put(obcClassName, clazz);
            return clazz;
        }
    }

    public static Object getConnection(Player player) {
        Method getHandleMethod = getMethod(player.getClass(), "getHandle");
        if (getHandleMethod != null) {
            try {
                Object nmsPlayer = getHandleMethod.invoke(player);
                Field playerConField = getField(nmsPlayer.getClass(), "playerConnection");
                return playerConField.get(nmsPlayer);
            } catch (Exception var4) {
                var4.printStackTrace();
            }
        }

        return null;
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            return clazz.getConstructor(params);
        } catch (NoSuchMethodException var3) {
            return null;
        }
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedMethods.containsKey(clazz)) {
            loadedMethods.put(clazz, new HashMap());
        }

        Map<String, Method> methods = (Map)loadedMethods.get(clazz);
        if (methods.containsKey(methodName)) {
            return (Method)methods.get(methodName);
        } else {
            try {
                Method method = clazz.getMethod(methodName, params);
                methods.put(methodName, method);
                loadedMethods.put(clazz, methods);
                return method;
            } catch (Exception var5) {
                var5.printStackTrace();
                methods.put(methodName, null);
                loadedMethods.put(clazz, methods);
                return null;
            }
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        if (!loadedFields.containsKey(clazz)) {
            loadedFields.put(clazz, new HashMap());
        }

        Map<String, Field> fields = (Map)loadedFields.get(clazz);
        if (fields.containsKey(fieldName)) {
            return (Field)fields.get(fieldName);
        } else {
            try {
                Field field = clazz.getField(fieldName);
                fields.put(fieldName, field);
                loadedFields.put(clazz, fields);
                return field;
            } catch (Exception var4) {
                var4.printStackTrace();
                fields.put(fieldName, null);
                loadedFields.put(clazz, fields);
                return null;
            }
        }
    }
}