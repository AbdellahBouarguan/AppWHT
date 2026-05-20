# AppWHT (WhatsApp Clone)

AppWHT is a multi-device Java messenger application supporting text messages, real-time audio/video calls, and cross-platform execution on Windows, macOS, and Linux.

## Requirements

Before you begin, ensure you have the following installed on your operating system:
* **Java Development Kit (JDK) 17** or higher
* **Apache Maven** (for building and dependency management)
* **PostgreSQL** (version 12 or higher recommended)

---

## 1. Database Setup (PostgreSQL)

This project uses PostgreSQL for robust and scalable data persistence. SQLite is no longer used or supported. The easiest way to run the database on any OS is using Docker.

### Option A: Using Docker (Recommended)
If you have [Docker](https://www.docker.com/get-started) installed, you can spin up the required PostgreSQL database in a single command. This handles creating the database, setting the credentials, and exposing the port automatically.

Open your terminal and run:
```bash
docker run --name messenger-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=messenger \
  -p 5432:5432 \
  -d postgres:15
```
*(You can stop the database anytime with `docker stop messenger-db` and restart it with `docker start messenger-db`)*

### Option B: Native Installation
1. **Install PostgreSQL** for your specific OS:
   * **Windows:** Download the installer from the [EnterpriseDB website](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads).
   * **macOS:** Use Homebrew: `brew install postgresql` and start it with `brew services start postgresql`.
   * **Linux (Ubuntu/Debian):** `sudo apt update && sudo apt install postgresql postgresql-contrib`
   * **Linux (Arch/Manjaro):** `sudo pacman -S postgresql`

2. **Configure the Database:**
   Open the PostgreSQL command line (`psql` or pgAdmin) and create the required database and user credentials that the server expects:

   ```sql
   -- Optional: Only if you need to create the postgres user and set its password
   -- ALTER USER postgres WITH PASSWORD 'postgres';

   -- Create the database
   CREATE DATABASE messenger;
   ```

   *Note: The server application automatically connects using the URL `jdbc:postgresql://localhost:5432/messenger?user=postgres&password=postgres`. Ensure your local PostgreSQL instance matches these credentials, or update `src/main/java/com/messenger/server/DatabaseManager.java` to reflect your specific configuration.*

---

## 2. Building the Project

Open your terminal or command prompt, navigate to the root directory of the project, and run the following Maven command to build the project and download all necessary dependencies (including JavaFX and OpenCV/JavaCV):

```bash
mvn clean install
```
*This command compiles the code and packages the application into a standalone JAR file located in the `target` directory.*

---

## 3. Running the Server

The centralized server handles user authentication, message routing, and offline message storage. You must start the server before any clients can connect.

To run the server:
```bash
# Using Maven:
mvn exec:java -Dexec.mainClass="com.messenger.server.MessagingServer"

# OR using the compiled JAR directly:
java -cp target/whatsapp-clone-1.0-SNAPSHOT.jar com.messenger.server.MessagingServer
```
*The server will automatically initialize the database schema (creating `users`, `contacts`, and `messages` tables) and listen on port `1234`.*

---

## 4. Running the Client

You can run multiple instances of the client across different machines on your local network.

To run the client application:
```bash
# Using Maven (JavaFX plugin):
mvn javafx:run

# OR using the compiled JAR directly:
java -jar target/whatsapp-clone-1.0-SNAPSHOT.jar
```

### Usage Instructions
1. **Login/Register:** When the client opens, register a new account or log in with existing credentials. (A default `admin` user with password `admin` is automatically seeded for testing purposes).
2. **Add Contacts:** You must add other registered users to your contact list to send them messages.
3. **Chat & Media:** Click on a contact to start chatting. You can send text messages and initiate real-time audio/video calls.

---

## Notes on Cross-OS Compatibility

The project uses JavaFX and JavaCV. To ensure seamless operation across all operating systems:
* The `pom.xml` explicitly defines dependencies with `<classifier>` tags for `win`, `mac`, and `linux`. Maven will dynamically resolve and pull the native UI and media libraries required for your specific OS architecture.
* When copying the built `target/whatsapp-clone-1.0-SNAPSHOT.jar` to another computer, ensure that device also has JDK 17+ installed. No further dependency installation is required as it is packaged as a "fat JAR".
