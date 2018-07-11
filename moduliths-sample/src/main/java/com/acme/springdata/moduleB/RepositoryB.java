package com.acme.springdata.moduleB;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Tom Hombergs
 */
public interface RepositoryB extends CrudRepository<EntityB, Long> {

}
