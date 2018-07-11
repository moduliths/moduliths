package com.acme.springdata.moduleA.internal;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Tom Hombergs
 */
public interface InternalRepositoryA extends CrudRepository<InternalEntityA, Long> {

}
