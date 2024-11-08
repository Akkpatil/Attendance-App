package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance2.Adapter.StudentAdapter;
import com.example.attendance2.Model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class AttendanceActivity extends AppCompatActivity implements StudentAdapter.OnAttendanceListener {

    private RecyclerView recyclerView;
    private List<Student> students;
    private Button addStudentButton, confirmAttendanceButton, generateReportButton, logoutButton;

    private FirebaseFirestore firestore;

    // Store the attendance data temporarily
    private Map<String, Boolean> attendanceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        firestore = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.studentListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        addStudentButton = findViewById(R.id.submitStudentButton);
        confirmAttendanceButton = findViewById(R.id.confirmAttendanceButton);
        generateReportButton = findViewById(R.id.generateReportButton);
        logoutButton = findViewById(R.id.logoutButton); // Initialize logout button

        attendanceMap = new HashMap<>();

        addStudentButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceActivity.this, AddStudent.class);
            startActivityForResult(intent, 1);
        });

        // listener for confirming attendance
        confirmAttendanceButton.setOnClickListener(v -> {
            authenticateAndConfirmAttendance();
        });

        // listener for generating the attendance report
        generateReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceActivity.this, GenerateReportActivity.class);
            startActivity(intent);
        });

        // listener for logout button
        logoutButton.setOnClickListener(v -> {
            // Go back to MainActivity without Firebase sign-out
            Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });

        fetchStudentsFromFirestore();
    }

    @Override
    public void authenticateAndMarkAttendance(String studentId, boolean isPresent, String date) {
        // Validate studentId to prevent NullPointerException
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Student ID is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the attendance map when attendance is marked for a student
        attendanceMap.put(studentId, isPresent);

        // Handle biometric authentication and attendance marking
        Toast.makeText(this, "Attendance marked for student " + studentId + " as " + (isPresent ? "Present" : "Absent") + " on " + date, Toast.LENGTH_SHORT).show();
    }

    private void fetchStudentsFromFirestore() {
        firestore.collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            students = new ArrayList<>();

                            // Loop through each document and add to list with ID set
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Student student = document.toObject(Student.class);
                                if (student != null) {
                                    student.setId(document.getId()); // Set document ID as student ID
                                    students.add(student);
                                }
                            }

                            StudentAdapter adapter = new StudentAdapter(AttendanceActivity.this, students, this);
                            recyclerView.setAdapter(adapter);
                        } else {
                            Toast.makeText(AttendanceActivity.this, "No students found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AttendanceActivity.this, "Error fetching students: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmAttendance() {
        if (attendanceMap.isEmpty()) {
            Toast.makeText(this, "No attendance marked to confirm.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        // Iterate over all students and their attendance
        for (String studentId : attendanceMap.keySet()) {
            boolean isPresent = attendanceMap.get(studentId);

            firestore.collection("students")
                    .document(studentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get student details
                            String studentName = documentSnapshot.getString("name");
                            String rollNo = documentSnapshot.getString("rollNo");
                            String studentClass = documentSnapshot.getString("studentClass");

                            // Check if all student details are available
                            if (studentName != null && rollNo != null && studentClass != null) {
                                // Create the Attendance object and store it in Firestore
                                com.example.attendance2.Model.Attendance attendance = new com.example.attendance2.Model.Attendance(
                                        studentName,
                                        rollNo,
                                        studentClass,
                                        currentDate,  // Use the current date for the attendance record
                                        isPresent
                                );

                                // Store the attendance data in Firestore
                                firestore.collection("attendance")
                                        .document(studentId)
                                        .collection("dates")
                                        .document(currentDate)
                                        .set(attendance)
                                        .addOnSuccessListener(aVoid -> {
                                            // Confirm attendance
                                            Toast.makeText(AttendanceActivity.this, "Attendance successfully recorded.", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(AttendanceActivity.this, "Error recording attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(AttendanceActivity.this, "Student details missing for ID: " + studentId, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AttendanceActivity.this, "Student not found with ID: " + studentId, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AttendanceActivity.this, "Error fetching student details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Biometric authentication function
    private void authenticateAndConfirmAttendance() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(AttendanceActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Call confirmAttendance after successful fingerprint authentication
                confirmAttendance();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AttendanceActivity.this, "Fingerprint authentication failed.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AttendanceActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Confirm your fingerprint to mark attendance")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Student newStudent = (Student) data.getSerializableExtra("newStudent");

            if (newStudent != null) {
                if (students == null) {
                    students = new ArrayList<>();
                }
                students.add(newStudent);
                recyclerView.getAdapter().notifyItemInserted(students.size() - 1);
            } else {
                Toast.makeText(AttendanceActivity.this, "Failed to add student. No data received.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

