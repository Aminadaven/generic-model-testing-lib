package com.aminadav.genericmodeltesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

public abstract class GenericIntegrationTests<ENTITY, ID> extends
    GenericRepositoryMethods<ENTITY, ID> {

  public GenericIntegrationTests(Class<ENTITY> clazz, CrudRepository<ENTITY, ID> repository) {
    super(clazz, repository);
  }

  @BeforeEach
  public void clean() {
    repository.deleteAll();
  }

  @Test
  void contextLoads() {
  }

  @Test
  public void should_find_no_entities() {
    assertThat(repository.findAll()).isEmpty();
  }

  @Test
  public void should_post_entity() throws Exception {
    ENTITY entity = createEntity();
    assertTrue(repository.existsById(postEntity(entity)));
//    assertTrue(entityExists(postEntity(entity), entity));
  }

  @Test
  public void should_post_all_entities() throws Exception {
    assertTrue(allEntitiesExists(createAndPostEntities(3)));
  }

  @Test
  public void should_get_entity_by_id() throws Exception {
    ENTITY postEntity = createEntity();
    ENTITY getEntity = getEntity(postEntity(postEntity));
    assertTrue(entityEquals(getEntity, postEntity));
  }

  @Test
  public void should_get_same_data() throws Exception {
    ENTITY postEntity = createEntity();
    ENTITY getEntity = getEntity(postEntity(postEntity));
    assertTrue(entityEquals(getEntity, postEntity));
  }

  @Test
  public void should_get_all_entities() throws Exception {
    assertThat(createAndPostEntities(3).keySet()).allSatisfy(this::getEntity);
//    Old implementations:
//    HashMap<ID, ENTITY> entities = createAndPostEntities(3);
//    for (ID id : entities.keySet()) {
//      ENTITY entity = entities.get(id);
//      ENTITY getEntity = getEntity(id);
//      assertTrue(entityEquals(getEntity, entity));
//    }
  }

  @Test
  public void should_put_entity() throws Exception {
    ID id = createAndPostEntities(2).keySet().iterator().next();

    ENTITY updatedEntity = createEntity();
    putEntity(id, updatedEntity);

    assertTrue(entityEquals(updatedEntity, repository.findById(id).get()));
  }

  @Test
  public void should_patch_entity() throws Exception {
    HashMap<ID, ENTITY> entities = createAndPostEntities(2);
    ID id = entities.keySet().iterator().next();

    ENTITY updatedEntity = entities.get(id);

    String jsonString = new ObjectMapper().writeValueAsString(changeRandomField(updatedEntity));
    System.out.println("jsonString: " + jsonString);
    patchEntity(id, jsonString);

    assertTrue(entityEquals(updatedEntity, repository.findById(id).get()));
  }

  @Test
  public void should_delete_entity_by_id() throws Exception {
    ID id = postEntity(createEntity());
    deleteEntity(id);
    assertFalse(repository.existsById(id));
    assertTrue(repository.count() == 0);

    HashMap<ID, ENTITY> entities = createAndPostEntities(3);
    ID idToDelete = entities.keySet().iterator().next();
    deleteEntity(idToDelete);
    entities.remove(idToDelete);
    assertFalse(repository.existsById(idToDelete));
    assertThat(repository.findAll()).hasSize(2);
    assertTrue(allEntitiesExists(entities));
  }
}

/*
  // Cool snippet from GitHub Copilot, need to make some changes
  @Test
  public void postgresTest() throws Exception {
    ENTITY entity = createEntity();
    ID id = postEntity(entity);
    ENTITY entityFromResponse = getEntity(id);
    // Changes goes here
    patchEntity(id, asJsonString(entityFromResponse));
    putEntity(id, entityFromResponse);
    deleteEntity(id);
  }
 */
