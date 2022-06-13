package com.aminadav.genericmodeltesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

abstract class GenericRepositoryMethods<ENTITY, ID> extends GenericRestMethods<ENTITY, ID> {

  CrudRepository<ENTITY, ID> repository;

  protected GenericRepositoryMethods(Class<ENTITY> clazz, CrudRepository<ENTITY, ID> repository) {
    super(clazz);
    this.repository = repository;
  }

//  protected boolean entityExists(ID id, ENTITY entity) {
//    Optional<ENTITY> optional = repository.findById(id);
//    return optional.isPresent() && entityEquals(optional.get(), entity);
//  }

  protected boolean allEntitiesExists(HashMap<ID, ENTITY> OriginalEntities) {
    if (repository.count() < OriginalEntities.size()) {
      System.err.format("Number of %ss in database is not at least as the number of posted %ss", className,
          className);
      return false;
    }
//    return OriginalEntities.entrySet().stream()
//        .allMatch(entry -> entityExists(entry.getKey(), entry.getValue()));
//    We no longer verify that the object is the same.
//    This should be verified by another test.
    return OriginalEntities.entrySet().stream()
        .allMatch(entry -> repository.existsById(entry.getKey()));
  }

  protected boolean allEntitiesExists(Set<ID> Ids, int originalEntitiesSize) {
    if (repository.count() < originalEntitiesSize) {
      System.err.format("Number of %ss in database is not at least as the number of posted %ss", className,
          className);
      return false;
    }
    return Ids.stream().allMatch(repository::existsById);
  }
}
