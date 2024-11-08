package com.example.attendance2.Model;

public class Attendance {
    private String studentName;
    private String rollNo;
    private String studentClass;
    private String date;
    private boolean isPresent;

    public Attendance(String studentName, String rollNo, String studentClass, String date, boolean isPresent) {
        this.studentName = studentName;
        this.rollNo = rollNo;
        this.studentClass = studentClass;
        this.date = date;
        this.isPresent = isPresent;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getStudentClass() {
        return studentClass;
    }

    public String getDate() {
        return date;
    }

    public boolean isPresent() {
        return isPresent;
    }
}
