package com.acme.springdata.moduleB;

import com.acme.springdata.moduleA.RepositoryA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceB {

	private RepositoryB repositoryB;

	private RepositoryA repositoryA;

	@Autowired
	public ServiceB(RepositoryB repositoryB, RepositoryA repositoryA) {
		this.repositoryB = repositoryB;
		this.repositoryA = repositoryA;
	}
}
