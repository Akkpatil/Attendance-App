package com.example.attendance2.Adapter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance2.Model.Student;
import com.example.attendance2.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final Activity context;
    private final List<Student> students;
    private final OnAttendanceListener attendanceListener;
    private final FirebaseFirestore db;

    // Define the interface for attendance actions
    public interface OnAttendanceListener {
        void authenticateAndMarkAttendance(String studentId, boolean isPresent, String date);
    }

    public StudentAdapter(Activity context, List<Student> students, OnAttendanceListener attendanceListener) {
        this.context = context;
        this.students = students;
        this.attendanceListener = attendanceListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public StudentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View listItemView = inflater.inflate(R.layout.student_list_item, parent, false);
        return new StudentViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.studentName.setText(student.getName());
        holder.studentRollNo.setText(student.getRollNo());
        holder.studentClass.setText(student.getStudentClass());
        holder.studentEmail.setText(student.getEmail());

        // Check if student ID is valid
        String studentId = student.getId();
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(context, "Invalid student ID for " + student.getName(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle the "Present" button click with date picker
        holder.markPresentButton.setOnClickListener(v -> {
            showDatePickerDialog(date -> {
                attendanceListener.authenticateAndMarkAttendance(studentId, true, date);
                markAttendanceInFirestore(studentId, date, true, student.getName());
            });
        });

        // Handle the "Absent" button click with date picker
        holder.markAbsentButton.setOnClickListener(v -> {
            showDatePickerDialog(date -> {
                attendanceListener.authenticateAndMarkAttendance(studentId, false, date);
                markAttendanceInFirestore(studentId, date, false, student.getName());
            });
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    // Method to show date picker dialog
    private void showDatePickerDialog(OnDateSelectedListener onDateSelectedListener) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    // Format selected date as a string
                    String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    onDateSelectedListener.onDateSelected(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    // Helper method to mark attendance in Firestore
    private void markAttendanceInFirestore(String studentId, String date, boolean isPresent, String studentName) {
        db.collection("attendance")
                .document(studentId)
                .collection("dates")
                .document(date)
                .set(new AttendanceRecord(isPresent))
                .addOnSuccessListener(aVoid -> {
                    String status = isPresent ? "Present" : "Absent";
                    Toast.makeText(context, "Marked " + status + " for " + studentName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error marking attendance", Toast.LENGTH_SHORT).show();
                });
    }

    // ViewHolder to hold the views for each student item
    public static class StudentViewHolder extends RecyclerView.ViewHolder {

        TextView studentName, studentRollNo, studentClass, studentEmail;
        Button markPresentButton, markAbsentButton;

        public StudentViewHolder(View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.studentName);
            studentRollNo = itemView.findViewById(R.id.studentRollNo);
            studentClass = itemView.findViewById(R.id.studentClass);
            studentEmail = itemView.findViewById(R.id.studentEmail);
            markPresentButton = itemView.findViewById(R.id.markPresentButton);
            markAbsentButton = itemView.findViewById(R.id.markAbsentButton);
        }
    }

    // AttendanceRecord class to represent attendance status in Firestore
    public static class AttendanceRecord {
        private boolean isPresent;

        public AttendanceRecord(boolean isPresent) {
            this.isPresent = isPresent;
        }

        public boolean isPresent() {
            return isPresent;
        }

        public void setPresent(boolean present) {
            isPresent = present;
        }
    }

    // Interface for handling date selection in DatePickerDialog
    private interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
}
