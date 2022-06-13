package com.aminadav.genericmodeltesting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aminadav.util.EqualsMatcher;
import com.aminadav.util.ThrowingConsumer;
import com.aminadav.util.ThrowingPredicate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Id;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@SpringBootTest
@AutoConfigureMockMvc
abstract class GenericRestMethods<ENTITY, ID> {

  private static ObjectMapper mapper;
  protected final Class<ENTITY> clazz;
  protected final String className, urlTemplate, urlTemplateId;
  private final HashMap<Class<?>, EqualsMatcher> classEqualsMatcherMap;
  protected Field id;
  @Autowired
  private MockMvc mvc;

  protected GenericRestMethods(Class<ENTITY> clazz) {
    this.clazz = clazz;
    this.className = clazz.getSimpleName();
    this.urlTemplate = "/" + className.toLowerCase() + "s";
    this.urlTemplateId = urlTemplate + "/{Id}";
    this.classEqualsMatcherMap = new HashMap<>();
    setObjectMapper();
  }

  private static void setObjectMapper() {
    if (mapper == null) {
      mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      // StdDateFormat is ISO8601 since jackson 2.9
      mapper.setDateFormat(new StdDateFormat());
    }
  }

  protected <T> void addEqualsMatcher(Class<T> clazz, EqualsMatcher equalsMatcher) {
    classEqualsMatcherMap.put(clazz, equalsMatcher);
  }

  private Field getRandomField() {
    Field[] fields = this.clazz.getDeclaredFields();
    Field field;
    do {
      field = fields[(int) (Math.random() * fields.length)];
    } while (field.equals(id));
    field.setAccessible(true);
    return field;
  }

  private Object getRandomValue(Class<?> type) {
    return switch (type.getSimpleName()) {
      case "String" -> UUID.randomUUID().toString();
      case "Integer", "int" -> (int) (Math.random() * 100);
      case "Long", "long" -> (long) (Math.random() * 100);
      case "Boolean", "boolean" -> Math.random() < 0.5;
      case "Double", "double" -> Math.random() * 100;
      case "Float", "float" -> (float) (Math.random() * 100);
      case "Short", "short" -> (short) (Math.random() * 100);
      case "Byte", "byte" -> (byte) (Math.random() * 100);
      case "Character", "char" -> (char) (Math.random() * 100);
      case "UUID" -> UUID.randomUUID();
      case "Object" -> new Object();
      default -> null;
    };
  }

  protected SimpleEntry<String, ?> changeRandomField(ENTITY entity) throws IllegalAccessException {
    Field field = getRandomField();
    field.set(entity, getRandomValue(field.getType()));
    return new SimpleEntry<>(field.getName(), field.get(entity));
  }

  protected void printEntity(ENTITY entity) {
    System.out.println("Printing " + className);
    for (Field field : this.clazz.getDeclaredFields()) {
      // Skip id field
      if (field.equals(id)) {
        continue;
      }
      // Print all fields values
      try {
        field.setAccessible(true);
        System.out.format("%s: %s\n", field.getName(), field.get(entity));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  protected ENTITY createEntity() {
    PodamFactory factory = new PodamFactoryImpl();
    ENTITY entity = factory.manufacturePojo(this.clazz);

    for (Field field : this.clazz.getDeclaredFields()) {
      // Find the @Id field
      if (field.isAnnotationPresent(Id.class)) {
        this.id = field;
        continue;
      }
      // Log all fields values
      try {
        field.setAccessible(true);
        System.out.format("%s %s: %s\n", className, field.getName(), field.get(entity));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return entity;
  }

  protected String asJsonString(ENTITY entity, String... ignore) {
    try {
      HashSet<String> ignoreSet = new HashSet<>(Arrays.asList(ignore));
      ignoreSet.add(id.getName());
      ObjectNode node = ((ObjectNode) mapper.readTree(mapper.writeValueAsString(entity)));
      node.remove(ignoreSet);
      String jsonString = node.toString();
      System.out.println("JSON: " + jsonString);
//    Used to verify the JSON string:
      System.out.print("Object from JSON: ");
      printEntity(mapper.readValue(jsonString, clazz));
      return jsonString;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected ENTITY getEntity(ID id) throws Exception {
    MvcResult result = mvc.perform(get(urlTemplateId, id))
        .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
        .andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    ObjectNode node = (ObjectNode) mapper.readTree(content);
    node.remove("_links");
    String jsonString = node.toString();
    System.out.println("JSON to Convert: " + jsonString);
    return mapper.readValue(jsonString, clazz);
  }

  protected void patchEntity(ID id, String jsonString) throws Exception {
    mvc.perform(
            patch(urlTemplateId, id).contentType(MediaType.APPLICATION_JSON).content(jsonString))
        .andExpect(status().isNoContent());
  }

  protected void putEntity(ID id, ENTITY entity) throws Exception {
    mvc.perform(put(urlTemplateId, id).contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(entity))).andExpect(status().isNoContent());
  }

  protected void deleteEntity(ID id) throws Exception {
    mvc.perform(delete(urlTemplateId, id)).andExpect(status().isNoContent());
  }

  @SuppressWarnings("unchecked")
  protected ID postEntity(ENTITY entity) throws Exception {
    MvcResult result = mvc.perform(
            post(urlTemplate).contentType(MediaType.APPLICATION_JSON).content(asJsonString(entity)))
        .andExpect(status().isCreated()).andReturn();
    String idFromResponse = Objects.requireNonNull(result.getResponse().getHeader("Location"))
        .replace("http://localhost" + urlTemplate + "/", "");
    System.out.println("ID from response: " + idFromResponse);
    return switch (id.getType().getSimpleName()) {
      case "Integer", "int" -> (ID) Integer.valueOf(idFromResponse);
      case "Long", "long" -> (ID) Long.valueOf(idFromResponse);
      case "String" -> (ID) idFromResponse;
      case "UUID" -> (ID) UUID.fromString(idFromResponse);
      default -> throw new RuntimeException("Unsupported type: " + id.getType().getSimpleName());
    };
  }

  protected HashMap<ID, ENTITY> createAndPostEntities(int numEntities) throws Exception {
    HashMap<ID, ENTITY> entities = new HashMap<>();
    for (int i = 0; i < numEntities; i++) {
      ENTITY entity = createEntity();
      entities.put(postEntity(entity), entity);
    }
    return entities;
  }

  protected boolean entityEquals(ENTITY entity1, ENTITY entity2) {
    return Arrays.stream(this.clazz.getDeclaredFields()).filter(field -> !field.equals(id))
        .peek(field -> field.setAccessible(true)).filter(
            (ThrowingPredicate<Field>) field -> !classEqualsMatcherMap.getOrDefault(field.getType(),
                    field.getType().isArray()
                        ? (o1, o2) -> Arrays.deepEquals((Object[]) o1, (Object[]) o2)
                        : Objects::equals)
                .equals(field.get(entity1), field.get(entity2))).peek(
            (ThrowingConsumer<Field>) field -> System.err.format("%s: %ss do not match, %s != %s\n",
                className, field.getName(), field.get(entity1), field.get(entity2))).count() == 0;
    /*
//    Old implementation:
    for (Field field : this.clazz.getDeclaredFields()) {
      // Skip id field
      if (field.equals(id)) {
        continue;
      }
      try {
        field.setAccessible(true);
        EqualsMatcher equalsMatcher = classEqualsMatcherMap.getOrDefault(field.getType(), Objects::equals);
        if (!equalsMatcher.equals(field.get(entity1), field.get(entity2))) {
          System.err.format("%s: %ss do not match, %s != %s\n", className, field.getName(),
              field.get(entity1), field.get(entity2));
          return false;
        }

//        if (!Objects.equals(field.get(entity1), field.get(entity2))) {
//          System.err.format("%s: %ss do not match, %s != %s\n", className, field.getName(),
//              field.get(entity1), field.get(entity2));
//          return false;
//        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return true;
    */
  }
}
