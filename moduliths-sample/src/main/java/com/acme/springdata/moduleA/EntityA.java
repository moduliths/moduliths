package com.acme.springdata.moduleA;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

/**
 * @author Tom Hombergs
 */
@Entity
@Data
public class EntityA {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String fieldA;

}
