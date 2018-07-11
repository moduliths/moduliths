package com.acme.springdata.moduleB;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Tom Hombergs
 */
@Entity
@Data
public class EntityB {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String fieldB;

}
