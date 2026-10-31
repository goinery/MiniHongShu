package com.hongshu.common.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * json解析的工具类
 *
 * @author hongshu
 */
public class JsonUtils {

    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * 把对象转换为json数据
     *
     * @return 2018年5月7日  下午5:27:16
     */
    public static String objectToJson(Object obj) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return null;
    }

    /**
     * 将Object转换成Map
     *
     */
    public static Map<String, Object> objectToMap(Object obj) {

        try {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
            String json = gson.toJson(obj);
            return jsonToMap(json);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return null;
    }

    /**
     * 将Object类型的map转换成String类型
     *
     */
    public static Map<String, String> mapToMap(Map<String, Object> map) {
        Map<String, String> returnMap = new HashMap<>();
        for (String key : map.keySet()) {
            returnMap.put(key, String.valueOf(map.get(key)));
        }
        return returnMap;
    }

    /**
     * 任意类型转换成Map
     *
     */
    public static Map<String, String> object2Map(Object obj) {
        Map<String, String> hashMap = new HashMap<>();
        try {
            Class<?> c = obj.getClass();
            Method[] m = c.getDeclaredMethods();
            for (Method javaMethod : m) {
                if (javaMethod.getName().indexOf("get") == 0) {
                    // 得到Map的key
                    String suffixKey = javaMethod.getName().substring(4);
                    String prefixKey = javaMethod.getName().substring(3, 4).toLowerCase();
                    hashMap.put(prefixKey + suffixKey, String.valueOf(javaMethod.invoke(obj)));
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
        return hashMap;
    }

    /**
     * 把json字符串转化为对象
     *
     */
    public static Object jsonToObject(String jsonString, Class<?> clazz) {

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        Object obj = null;
        try {
            obj = gson.fromJson(jsonString, clazz);
        } catch (JsonSyntaxException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return obj;
    }

    /**
     * josn转arrayList
     *
     */
    public static ArrayList<?> jsonArrayToArrayList(String jsonArray) {

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
        ArrayList<?> list = null;
        try {
            Type listType = new TypeToken<ArrayList<?>>() {
            }.getType();

            list = gson.fromJson(jsonArray, listType);
        } catch (JsonSyntaxException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return list;
    }

    /**
     * JSON 转 ArrayList
     */
    public static ArrayList<?> jsonArrayToArrayList(String jsonArray, Class<?> clazz) {

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
        ArrayList<?> list = null;
        try {

            list = (ArrayList<?>) gson.fromJson(jsonArray, clazz);
        } catch (JsonSyntaxException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return list;
    }

    /**
     * 把json转换为map类型的数据
     *
     */
    public static Map<String, Object> jsonToMap(String json) {

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
        Map<String, Object> map = null;
        try {
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();

            map = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return map;
    }

    /**
     * 将Json转换成Map<String, ?>
     *
     */
    public static Map<String, ?> jsonToMap(String json, Class<?> clazz) {

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
        Map<String, ?> map = null;
        try {
            Type type = new TypeToken<Map<String, ?>>() {
            }.getType();

            map = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return map;
    }

    /**
     * 将map转换成pojo
     *
     */
    public static <T> T mapToPojo(Map<String, Object> map, Class<T> beanType) {

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();

        JsonElement jsonElement = gson.toJsonTree(map);
        return gson.fromJson(jsonElement, beanType);
    }

    /**
     * 将json结果集转化为对象
     *
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            return MAPPER.readValue(jsonData, beanType);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return null;
    }

    /**
     * 将json数据转换成pojo对象list
     *
     */
    public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
        JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            return MAPPER.readValue(jsonData, javaType);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return null;
    }

    /**
     * 将任意pojo转化成map
     *
     * @param t pojo对象
     */
    public static <T> Map<String, Object> pojoToMap(T t) {
        Map<String, Object> result = new HashMap<>();
        Method[] methods = t.getClass().getMethods();
        try {
            for (Method method : methods) {
                Class<?>[] paramClass = method.getParameterTypes();
                // 如果方法带参数，则跳过
                if (paramClass.length > 0) {
                    continue;
                }
                String methodName = method.getName();
                if (methodName.startsWith("get")) {
                    Object value = method.invoke(t);
                    result.put(methodName, value);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | SecurityException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).warn("操作失败", e);
        }
        return result;
    }

}
