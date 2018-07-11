package com.acme.springdata.moduleA.internal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class InternalEntityA {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String field;

}
