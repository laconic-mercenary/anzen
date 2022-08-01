/**
 * 
 */
package anzen.configuration.properties;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author mlcs05
 *
 */
public abstract class PropertiesAnnotations {

	public static void loadConfiguration(Object object) {
		Class<?> objectClass = object.getClass();
		if (objectClass.isAnnotationPresent(PropertiesBound.class)) {
			PropertiesBound pbound = objectClass.getAnnotation(PropertiesBound.class);
			String props = pbound.propertiesName();
			Properties config = new Properties();
			try (InputStream in = object.getClass().getClassLoader().getResourceAsStream(props + ".properties")) {
				config.loadFromXML(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			for (Field field : objectClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(PropertiesField.class)) {
					PropertiesField pfield = field.getAnnotation(PropertiesField.class);
					String value = config.getProperty(pfield.key());
					Class<?> classType = pfield.type();
					Object resultValue = value;
					if (classType.equals(Integer.class)) {
						Integer numValue = Integer.valueOf(value);
						resultValue = classType.cast(numValue);
					} else if (classType.equals(Double.class)) {
						Double numValue = Double.valueOf(value);
						resultValue = classType.cast(numValue);
					} else if (classType.equals(Float.class)) {
						Float numValue = Float.valueOf(value);
						resultValue = classType.cast(numValue);
					} else if (classType.equals(Byte.class)) {
						Byte numValue = Byte.valueOf(value);
						resultValue = classType.cast(numValue);
					} else if (classType.equals(Boolean.class)) {
						Boolean numValue = Boolean.valueOf(value);
						resultValue = classType.cast(numValue);
					} else if (classType.equals(String[].class)) {
						String[] result = value.split(",");
						resultValue = classType.cast(result);
					}
					try {
						field.set(object, resultValue);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		} else {
			throw new RuntimeException("Class: " + objectClass + " not annotated");
		}
	}
}
