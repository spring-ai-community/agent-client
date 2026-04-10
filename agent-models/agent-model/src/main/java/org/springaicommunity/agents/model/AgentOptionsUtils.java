/*
 * Copyright 2024 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agents.model;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for manipulating options objects.
 * <p>
 * Inspired by Spring AI's ModelOptionsUtils, provides methods to merge options where
 * higher-priority options override lower-priority ones using JSON serialization for
 * flexible merging.
 *
 * @author Spring AI Community
 * @since 0.12.0
 */
public final class AgentOptionsUtils {

	private AgentOptionsUtils() {
		// Utility class, prevent instantiation
	}

	public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		.addModule(new JavaTimeModule())
		.addModule(createPathModule())
		.build();

	private static SimpleModule createPathModule() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(Path.class, new ToStringSerializer());
		module.addDeserializer(Path.class, new PathDeserializer());
		return module;
	}

	/**
	 * Custom deserializer for {@link Path} that converts string values to Path objects.
	 */
	private static class PathDeserializer extends StdDeserializer<Path> {

		PathDeserializer() {
			super(Path.class);
		}

		@Override
		public Path deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt)
				throws java.io.IOException {
			String value = p.getValueAsString();
			return value != null ? Path.of(value) : null;
		}

	}

	private static final List<String> BEAN_MERGE_FIELD_EXCISIONS = List.of("class");

	private static final ConcurrentHashMap<Class<?>, List<String>> REQUEST_FIELD_NAMES_PER_CLASS = new ConcurrentHashMap<>();

	/**
	 * Merges the source object into the target object and returns an object represented
	 * by the given class. The JSON property names are used to match the fields to merge.
	 * The source non-null values override the target values with the same field name. The
	 * source null values are ignored. If the acceptedFieldNames is not empty, only the
	 * fields with the given names are merged and returned. If the acceptedFieldNames is
	 * empty, use the {@code @JsonProperty} names, inferred from the provided clazz.
	 * @param <T> the type of the class to return.
	 * @param source the source object to merge (higher priority).
	 * @param target the target object to merge into (lower priority).
	 * @param clazz the class to return.
	 * @param acceptedFieldNames the list of field names accepted for the target object.
	 * @return the merged object represented by the given class.
	 */
	public static <T> T merge(Object source, Object target, Class<T> clazz, List<String> acceptedFieldNames) {
		if (source == null) {
			source = Map.of();
		}

		List<String> requestFieldNames = CollectionUtils.isEmpty(acceptedFieldNames)
				? REQUEST_FIELD_NAMES_PER_CLASS.computeIfAbsent(clazz, AgentOptionsUtils::getJsonPropertyValues)
				: acceptedFieldNames;

		// If no @JsonProperty annotations found, fall back to bean-based merge
		if (CollectionUtils.isEmpty(requestFieldNames)) {
			return mergeByBeans(source, target, clazz);
		}

		Map<String, Object> sourceMap = AgentOptionsUtils.objectToMap(source);
		Map<String, Object> targetMap = AgentOptionsUtils.objectToMap(target);

		targetMap.putAll(sourceMap.entrySet()
			.stream()
			.filter(e -> e.getValue() != null)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		targetMap = targetMap.entrySet()
			.stream()
			.filter(e -> requestFieldNames.contains(e.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return AgentOptionsUtils.mapToClass(targetMap, clazz);
	}

	/**
	 * Merges the source object into the target object and returns an object represented
	 * by the given class. The JSON property names are used to match the fields to merge.
	 * The source non-null values override the target values with the same field name. The
	 * source null values are ignored. Returns the only field names that match the
	 * {@code @JsonProperty} names, inferred from the provided clazz.
	 * @param <T> the type of the class to return.
	 * @param source the source object to merge (higher priority).
	 * @param target the target object to merge into (lower priority).
	 * @param clazz the class to return.
	 * @return the merged object represented by the given class.
	 */
	public static <T> T merge(Object source, Object target, Class<T> clazz) {
		return AgentOptionsUtils.merge(source, target, clazz, null);
	}

	/**
	 * Fallback merge method using BeanWrapper when @JsonProperty annotations are not
	 * present.
	 */
	private static <T> T mergeByBeans(Object source, Object target, Class<T> clazz) {
		try {
			T result = clazz.getConstructor().newInstance();
			if (target != null) {
				copyBeanProperties(target, result);
			}
			if (source != null && !source.equals(Map.of())) {
				copyBeanProperties(source, result);
			}
			return result;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to merge objects using bean properties", e);
		}
	}

	/**
	 * Copy non-null properties from source to target using BeanWrapper.
	 */
	private static void copyBeanProperties(Object source, Object target) {
		BeanWrapper sourceWrap = new BeanWrapperImpl(source);
		BeanWrapper targetWrap = new BeanWrapperImpl(target);

		for (PropertyDescriptor descriptor : sourceWrap.getPropertyDescriptors()) {
			String propertyName = descriptor.getName();
			if (!BEAN_MERGE_FIELD_EXCISIONS.contains(propertyName) && descriptor.getReadMethod() != null) {
				Object value = sourceWrap.getPropertyValue(propertyName);
				if (value != null && targetWrap.isWritableProperty(propertyName)) {
					targetWrap.setPropertyValue(propertyName, value);
				}
			}
		}
	}

	/**
	 * Converts the given object to a Map.
	 * @param source the object to convert to a Map.
	 * @return the converted Map.
	 */
	public static Map<String, Object> objectToMap(Object source) {
		if (source == null) {
			return new HashMap<>();
		}
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			})
				.entrySet()
				.stream()
				.filter(e -> e.getValue() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the given Map to the given class.
	 * @param <T> the type of the class to return.
	 * @param source the Map to convert to the given class.
	 * @param clazz the class to convert the Map to.
	 * @return the converted class.
	 */
	public static <T> T mapToClass(Map<String, Object> source, Class<T> clazz) {
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			return OBJECT_MAPPER.readValue(json, clazz);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the list of name values of the {@link JsonProperty} annotations.
	 * @param clazz the class that contains fields annotated with {@link JsonProperty}.
	 * @return the list of values of the {@link JsonProperty} annotations.
	 */
	public static List<String> getJsonPropertyValues(Class<?> clazz) {
		List<String> values = new ArrayList<>();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			JsonProperty jsonPropertyAnnotation = field.getAnnotation(JsonProperty.class);
			if (jsonPropertyAnnotation != null) {
				values.add(jsonPropertyAnnotation.value());
			}
		}
		return values;
	}

	private static String toGetName(String name) {
		return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
