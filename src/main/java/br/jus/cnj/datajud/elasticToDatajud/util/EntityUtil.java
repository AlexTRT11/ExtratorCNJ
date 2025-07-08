package br.jus.cnj.datajud.elasticToDatajud.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.proxy.HibernateProxy;

public class EntityUtil {
	public static <E> void copyValues(E from, E to) throws InstantiationException, IllegalAccessException {
		PropertyDescriptor[] pds = getPropertyDescriptors(from.getClass());
		for (PropertyDescriptor pd : pds) {
			if (!isId(pd) && isAceptable(pd)) {
				Method rm = pd.getReadMethod();
				Method wm = pd.getWriteMethod();
				if (wm != null) {
					Object value = invokeAndWrap(rm, from, new Object[0]);
					invokeAndWrap(wm, to, removeProxy(value));
				}
			}
		}
	}

	public static List<PropertyDescriptor> getProperties(Class<?> component) {
		List<PropertyDescriptor> resp = new ArrayList<PropertyDescriptor>();
		try {
			PropertyDescriptor[] pds = getPropertyDescriptors(component);
			for (int i = 0; i < pds.length; i++) {
				PropertyDescriptor pd = pds[i];
				if (!pd.getName().equals("class") && !pd.getName().equals("bytes")) {
					resp.add(pd);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resp;
	}
	
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
		try {
			return Introspector.getBeanInfo(clazz).getPropertyDescriptors();
		} catch (IntrospectionException e) {
		}
		return new PropertyDescriptor[0];
	}

	public static boolean hasAnnotation(PropertyDescriptor pd, Class<? extends Annotation> annotation) {
		Method readMethod = pd.getReadMethod();
		if (readMethod != null) {
			if (readMethod.isAnnotationPresent(annotation)) {
				return true;
			}

			Class<?> declaringClass = readMethod.getDeclaringClass();
			try {
				Field field = declaringClass.getDeclaredField(pd.getName());
				return field.isAnnotationPresent(annotation);
			} catch (NoSuchFieldException ex) {
				return false;
			}

		}
		return false;
	}

	private static boolean isId(final PropertyDescriptor pd) {
		return pd != null && (hasAnnotation(pd, Id.class) || hasAnnotation(pd, EmbeddedId.class));
	}

	private static boolean isAceptable(final PropertyDescriptor pd) {
		return pd != null && !hasAnnotation(pd, Transient.class) && 
			(hasAnnotation(pd, Column.class) || hasAnnotation(pd, ManyToOne.class));
	}
	
	public static Object removeProxy(Object object) {
		if (object instanceof HibernateProxy) {
			object = ((HibernateProxy) object).getHibernateLazyInitializer().getImplementation();
		}
		return object;
	}

	private static Object invokeAndWrap(Method method, Object target, Object... args)
	{
		try {
			return invoke(method, target, args);
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			else {
				throw new RuntimeException("exception invoking: " + method.getName(), e);
			}
		}
   }
	
	private static Object invoke(Method method, Object target, Object... args) throws Exception
	{
		try {
			return method.invoke( target, args );
		}
		catch (IllegalArgumentException iae) {
			String message = "Could not invoke method by reflection on: "+ target.getClass().getName();
			throw new IllegalArgumentException(message, iae);
		}
		catch (InvocationTargetException ite) {
			if ( ite.getCause() instanceof Exception ) {
				throw (Exception) ite.getCause();
			}
			else {
				throw ite;
			}
		}
	}
}
