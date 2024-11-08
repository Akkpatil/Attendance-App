package com.example.attendance2.Model;

import java.io.Serializable;

public class Student implements Serializable {
    private String id;
    private String name;
    private String rollNo;
    private String studentClass;
    private String email;

    public Student() {}

    public Student(String id, String name, String rollNo, String studentClass, String email) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.studentClass = studentClass;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
