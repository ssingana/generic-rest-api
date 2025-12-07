package com.example.dynamicquery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.dynamicquery.model.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {}
