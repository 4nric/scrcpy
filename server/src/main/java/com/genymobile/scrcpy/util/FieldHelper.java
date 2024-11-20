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
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            } catch (IllegalAccessException e) {
                //Field not found
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
}
