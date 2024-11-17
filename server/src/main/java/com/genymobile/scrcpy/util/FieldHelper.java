package com.genymobile.scrcpy.util;

import java.lang.reflect.Field;

public final class FieldHelper {

    private FieldHelper() {
        // not instantiable
    }

    public static Object getField(Object object, String fieldName){
        Class<?> cls = object.getClass();
        while (cls != null) {
            try {
                // Try to get the declared field from the current class
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                // Field not found in this class, continue to superclass
                cls = cls.getSuperclass();
            } catch (IllegalAccessException e) {
                Ln.w("Field not found: " + fieldName, e);
                return null;
            }
        }
        return null;
    }

    public static void listFields(Object object) {
        Class<?> cls = object.getClass();
        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Ln.d("Field name: " + field.getName() + ", value: " + field.get(object));
                } catch (IllegalAccessException e) {
                    Ln.e("Error accessing field: " + field.getName());
                }
            }
            cls = cls.getSuperclass();
        }
    }

    public static void listClasses(Object object) {
        Class<?> cls = object.getClass();
        while (cls != null) {
            Class<?>[] classes = cls.getDeclaredClasses();
            for (Class<?> class1 : classes) {
                Ln.d("Class name: " + class1.getName() + ", CLASSES:");
                listFields(class1);
                listClasses(class1);
            }
            cls = cls.getSuperclass();
        }
    }
}
