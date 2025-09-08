# Expense Tracker - Backend 🚀

[![Made with Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)

This repository contains the backend server for the [Expense Tracker Android Application](https://github.com/KevalMGajjar/ExpenseTrackerAndroid). It is a robust REST API built with Spring Boot and Kotlin, handling all business logic, data storage, and user authentication.

---

## ✨ Features

- **Secure Authentication:** User registration and login powered by JWT (JSON Web Tokens) for secure, stateless authentication.
- **RESTful API:** A well-structured API for all client-side operations.
- **Expense Management:** Full CRUD (Create, Read, Update, Delete) operations for user expenses.
- **Group Management:** Functionality to create expense groups, add/remove members, and manage shared balances.
- **User Profiles:** Endpoints to manage user data.

---

## 🛠️ Tech Stack & Tools

- **Framework:** Spring Boot
- **Language:** Kotlin
- **Database:** MongoDB
- **Authentication:** Spring Security with JWT
- **Build Tool:** Gradle

---

## API Overview

The API provides a set of RESTful endpoints for interacting with the application's resources.

| Resource | Endpoints | Description |
| :--- | :--- | :--- |
| **Authentication** | `/api/auth/register`, `/api/auth/login` | Handles user registration and generates JWT tokens upon successful login. |
| **Users** | `/api/users/...` | Manages user profiles and data. |
| **Expenses** | `/api/expenses/...` | Allows for creating, viewing, updating, and deleting expenses. |
| **Groups** | `/api/groups/...` | Manages group creation, memberships, and group-related expenses. |

---

## 🚀 Getting Started

Follow these instructions to get the backend server up and running on your local machine.

### Prerequisites

- **Java JDK 17** or higher.
- **MongoDB:** Ensure you have a running instance of MongoDB. You can use a local installation or a cloud service like MongoDB Atlas.
- **IntelliJ IDEA** or another IDE with Kotlin support.

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/KevalMGajjar/ExpenseTrackerBackend.git](https://github.com/KevalMGajjar/ExpenseTrackerBackend.git)
    cd ExpenseTrackerBackend
    ```

2.  **Configure the application:**
    - Navigate to `src/main/resources/`.
    - Rename the `application-example.properties` file to `application.properties`.
    - Open the new `application.properties` file and fill in the required values:

    ```properties
    # Server port
    server.port=8080

    # MongoDB Connection URI
    spring.data.mongodb.uri=mongodb://localhost:27017/expense_tracker_db

    # JWT Configuration
    jwt.secret=YOUR_SUPER_SECRET_KEY_FOR_JWT # Replace with a strong, secret key
    jwt.expiration=86400000 # Token expiration time in milliseconds (e.g., 24 hours)
    ```

3.  **Build the project:**
    Open a terminal in the root directory and run the Gradle wrapper to build the project and download dependencies.
    ```bash
    ./gradlew build
    ```

4.  **Run the application:**
    You can run the application in two ways:
    
    - **Using Gradle:**
      ```bash
      ./gradlew bootRun
      ```
    - **From your IDE:**
      Open the project in IntelliJ IDEA and run the `ExpenseTrackerBackendApplication` main function.

The server should now be running on `http://localhost:8080`.

---

<p align="center">
  This backend powers the Expense Tracker Android App
</p>
