package com.example.dynamicquery.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double salary;
    private LocalDate joiningDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    private Boolean active;

    public Employee() {}
    public Employee(String name, Double salary, LocalDate joiningDate, Department department, Boolean active) {
        this.name = name; this.salary = salary; this.joiningDate = joiningDate; this.department = department; this.active = active;
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Double getSalary() { return salary; } public void setSalary(Double salary) { this.salary = salary; }
    public LocalDate getJoiningDate() { return joiningDate; } public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public Department getDepartment() { return department; } public void setDepartment(Department department) { this.department = department; }
    public Boolean getActive() { return active; } public void setActive(Boolean active) { this.active = active; }
}
