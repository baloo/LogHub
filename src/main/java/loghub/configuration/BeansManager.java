package loghub.configuration;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BeansManager {

    private BeansManager() {
    }
    /**
     * Given an object, a bean name and a bean value, try to set the bean.
     * 
     * The bean type is expect to have a constructor taking a String argument
     * @param o the object to set
     * @param beanName the bean to set
     * @param beanValue the bean value
     * @throws InvocationTargetException
     */
    static public void beanSetter(Object o, String beanName, String beanValue) throws InvocationTargetException{
        try {
            PropertyDescriptor bean = new PropertyDescriptor(beanName, o.getClass());
            Method setMethod = bean.getWriteMethod();
            if(setMethod == null) {
                throw new InvocationTargetException(new NullPointerException(), String.format("Unknown bean %s", beanName));
            }
            Class<?> setArgType = bean.getPropertyType();
            Object argInstance = ConstructFromString(setArgType, beanValue);
            setMethod.invoke(o, argInstance);       
        } catch (Exception e) {
            throw new InvocationTargetException(e, "invalid bean '" + beanName + "' for " + o);
        }
    }

    /**
     * Extract a map of the beans of an class. Only the beans listed in the ProbeBean class will be return
     * @param c a class to extract beans from
     * @return
     * @throws InvocationTargetException
     */
    static public Map<String, PropertyDescriptor> getBeanPropertiesMap(Class<?> c, Class<?> topClass) throws InvocationTargetException {
        Set<Beans> beansAnnotations = enumerateAnnotation(c, Beans.class, topClass);
        if(beansAnnotations.isEmpty())
            return Collections.emptyMap();
        Map<String, PropertyDescriptor> beanProperties = new HashMap<String, PropertyDescriptor>();
        for(Beans annotation: beansAnnotations) {
            for(String beanName: annotation.value()) {
                //Bean already found, don't work on it again
                if( beanProperties.containsKey(beanName)) {
                    continue;
                }
                try {
                    PropertyDescriptor bean = new PropertyDescriptor(beanName, c);
                    beanProperties.put(bean.getName(), bean);
                } catch (IntrospectionException e) {
                    throw new InvocationTargetException(e, "invalid bean " + beanName + " for " + c.getName());
                }

            }
        }
        return beanProperties;
    }

    /**
     * Enumerate the hierarchy of annotation for a class, until a certain class type is reached
     * @param searched the Class where the annotation is searched
     * @param annontationClass the annotation class
     * @param stop a class that will stop (included) the search 
     * @return
     */
    static public <T extends Annotation> Set<T> enumerateAnnotation(Class<?> searched, Class<T> annontationClass, Class<?> stop) {
        Set<T> annotations =  new LinkedHashSet<T>();
        while(searched != null && stop.isAssignableFrom(searched)) {
            if(searched.isAnnotationPresent(annontationClass)) {
                T annotation = searched.getAnnotation(annontationClass);
                annotations.add(annotation);
            }
            for(Class<?> i: searched.getInterfaces()) {
                if(i.isAnnotationPresent(annontationClass)) {
                    T annotation = i.getAnnotation(annontationClass);
                    annotations.add(annotation);
                }
            }
            searched = searched.getSuperclass();
        }
        return annotations;
    }

    /**
     * Create an object providing the class and a String argument. So the class must have
     * a constructor taking only a string as an argument.
     * 
     * It can manage native type and return an boxed object 
     * @param <T>
     * @param clazz
     * @param value
     * @return
     * @throws InvocationTargetException 
     */
    @SuppressWarnings("unchecked")
    public static <T> T ConstructFromString(Class<T> clazz, String value) throws InvocationTargetException {
        try {
            Constructor<T> c = null;
            if(! clazz.isPrimitive() ) {
                c = clazz.getConstructor(String.class);
            }
            else if(clazz == Integer.TYPE) {
                c = (Constructor<T>) Integer.class.getConstructor(String.class);
            }
            else if(clazz == Double.TYPE) {
                c = (Constructor<T>) Double.class.getConstructor(String.class);
            }
            else if(clazz == Float.TYPE) {
                c = (Constructor<T>)Float.class.getConstructor(String.class);
            }
            else if(clazz == Byte.TYPE) {
                c = (Constructor<T>) Byte.class.getConstructor(String.class);
            }
            else if(clazz == Long.TYPE) {
                c = (Constructor<T>)Long.class.getConstructor(String.class);
            }
            else if(clazz == Short.TYPE) {
                c = (Constructor<T>)Short.class.getConstructor(String.class);
            }
            else if(clazz == Boolean.TYPE) {
                c = (Constructor<T>)Boolean.class.getConstructor(String.class);
            }
            else if(clazz == Character.TYPE) {
                c = (Constructor<T>) Character.class.getConstructor(String.class);
            }
            return c.newInstance(value);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetException(e, clazz.getName());
        }
    }

    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
    }


}
