# Attendance Management System

An Android-based Attendance Management System that leverages biometric (fingerprint) authentication for secure and efficient attendance tracking. Designed to streamline attendance processes in educational or organizational settings.

## Features

### Core Features
- **User Authentication**: Secure login with email and password using Firebase Authentication.
- **Student Data Management**: Add and manage student details, stored persistently in Firebase Firestore.
- **Attendance Marking**: Mark students as present or absent via a simple and intuitive interface.
- **Biometric Confirmation**: Validate attendance marking through fingerprint authentication for added security.
- **Session Management**: Stay logged in for convenience or securely log out after use.

### Additional Features
- **Attendance Report Generation**: Generate and save detailed attendance reports in Excel format.
- **Offline Access**: Mark attendance offline, with data synchronized to the cloud once connected.
- **Real-Time Updates**: Instant synchronization of attendance records across devices.

## Technical Stack
- **Frontend**: Android Studio (Java/Kotlin)
- **Backend**: Firebase Firestore for data storage, Firebase Authentication for user management
- **Security**: Fingerprint authentication via Android Biometric API
- **Report Generation**: Excel/CSV file generation for attendance records

## Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/Akkpatil/Attendance-App.git
