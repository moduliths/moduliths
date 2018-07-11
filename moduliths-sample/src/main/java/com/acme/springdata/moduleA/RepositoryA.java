package com.acme.springdata.moduleA;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Tom Hombergs
 */
public interface RepositoryA extends CrudRepository<EntityA, Long> {

}
