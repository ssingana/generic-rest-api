package com.example.dynamicquery.config;

import com.example.dynamicquery.model.*;
import com.example.dynamicquery.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Random;

@Configuration
public class DataLoader {
    @Bean
    CommandLineRunner init(DepartmentRepository deptRepo, EmployeeRepository empRepo) {
        return args -> {
            Department d1 = new Department("HR"); Department d2 = new Department("Engineering"); Department d3 = new Department("Sales");
            deptRepo.save(d1); deptRepo.save(d2); deptRepo.save(d3);
            Random rand = new Random(12345);
            for (int i = 1; i <= 500; i++) {
                String name = (i % 5 == 0) ? "John " + i : "Employee " + i;
                double salary = 25000 + rand.nextInt(90000);
                LocalDate jd = LocalDate.now().minusDays(rand.nextInt(2000));
                Department dept = (i % 3 == 0) ? d1 : (i % 3 == 1 ? d2 : d3);
                boolean active = (i % 2 == 0);
                Employee e = new Employee(name, salary, jd, dept, active);
                empRepo.save(e);
            }
        };
    }
}
