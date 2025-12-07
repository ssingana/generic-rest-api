package com.example.dynamicquery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.dynamicquery.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {}
