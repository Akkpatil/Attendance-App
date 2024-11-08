package com.example.attendance2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.attendance2.Model.Student;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerateReportActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private ProgressBar progressBar;
    private TextView generatingText;
    private Button backButton;
    private AtomicInteger pendingTasks;  // Tracks the pending async tasks
    private Set<String> addedStudents = new HashSet<>(); // To track added students and dates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_report);

        firestore = FirebaseFirestore.getInstance();

        progressBar = findViewById(R.id.progressBar);
        generatingText = findViewById(R.id.generatingText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> onBackPressed());

        generateAttendanceReport();
    }

    private void generateAttendanceReport() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        generatingText.setVisibility(TextView.VISIBLE);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Student Name");
        header.createCell(1).setCellValue("Roll No");
        header.createCell(2).setCellValue("Class");
        header.createCell(3).setCellValue("Date");
        header.createCell(4).setCellValue("Attendance");

        firestore.collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> students = task.getResult().getDocuments();
                        pendingTasks = new AtomicInteger(students.size());  // Set number of pending tasks

                        for (DocumentSnapshot studentDoc : students) {
                            Student student = studentDoc.toObject(Student.class);
                            if (student != null) {
                                String studentId = studentDoc.getId();

                                // Fetch attendance for each student
                                firestore.collection("attendance")
                                        .document(studentId)
                                        .collection("dates")
                                        .get()
                                        .addOnCompleteListener(attendanceTask -> {
                                            if (attendanceTask.isSuccessful() && attendanceTask.getResult() != null) {
                                                boolean isRowAddedForStudent = false; // Track if the row is added for this student

                                                for (QueryDocumentSnapshot attendanceDoc : attendanceTask.getResult()) {
                                                    String date = attendanceDoc.getId(); // This is where you fetch the attendance date
                                                    Boolean isPresent = attendanceDoc.getBoolean("present");

                                                    // Only add a new row if the student hasn't been added yet for this attendance date
                                                    if (!isRowAddedForStudent) {
                                                        Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
                                                        row.createCell(0).setCellValue(student.getName());
                                                        row.createCell(1).setCellValue(student.getRollNo());
                                                        row.createCell(2).setCellValue(student.getStudentClass());
                                                        isRowAddedForStudent = true; // Mark that we've added the student row
                                                    }

                                                    // Add attendance details for each date
                                                    Row attendanceRow = sheet.createRow(sheet.getPhysicalNumberOfRows());
                                                    attendanceRow.createCell(4).setCellValue(isPresent != null && isPresent ? "Present" : "Absent");

                                                    // Here is where you need to format the date correctly
                                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                    try {
                                                        // Convert the date string from Firestore to a Date object
                                                        Date attendanceDate = dateFormat.parse(date);
                                                        // If the date is successfully parsed, format it back to a consistent string
                                                        String formattedDate = dateFormat.format(attendanceDate);
                                                        attendanceRow.createCell(3).setCellValue(formattedDate);  // Add the formatted date to the Excel sheet
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        // If parsing fails, just use the raw date (which should not happen in normal cases)
                                                        attendanceRow.createCell(3).setCellValue(date);
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(GenerateReportActivity.this, "Failed to fetch attendance for " + student.getName(), Toast.LENGTH_SHORT).show();
                                            }

                                            // Decrease the pending tasks count and check if all tasks are completed
                                            if (pendingTasks.decrementAndGet() == 0) {
                                                saveExcelFile(workbook);
                                            }
                                        });

                            }
                        }
                    } else {
                        Toast.makeText(GenerateReportActivity.this, "Failed to fetch students data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveExcelFile(Workbook workbook) {
        try {
            File directory = new File(getExternalFilesDir(null), "AttendanceReports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, "AttendanceReport.xlsx");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(ProgressBar.GONE);
                generatingText.setVisibility(TextView.GONE);
                Toast.makeText(GenerateReportActivity.this, "Report generated successfully", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(GenerateReportActivity.this, "Error generating report", Toast.LENGTH_SHORT).show());
        }
    }
}
