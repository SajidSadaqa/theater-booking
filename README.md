# Theater Booking System

A simple, extensible Java console application for managing theater layouts and seat bookings without a persistent database. This project demonstrates:

* **Customizable Theaters**: Define multiple theaters, sections, rows, and seat counts via CSV files.
* **Asynchronous Upload**: Load and process multiple layout files concurrently for faster startup.
* **Validation & Error Handling**: Enforce CSV schema rules and business constraints (e.g., no duplicate seats, valid ranges) using custom exceptions.
* **Exception Types**:

  * **BusinessException**: Thrown when CSV data violates application rules (e.g., duplicate seat definitions).
  * **DatabaseException**: Simulated to demonstrate handling of SQL errors (e.g., unique constraint violations, missing columns).

---

## Features

* **CSV-Driven Configuration**: Easily add new theaters by dropping layout files into a resources folder.
* **Flexible Layout Definitions**: Specify contiguous seat ranges or individual bookings in the same CSV.
* **In-Memory Management**: All data is processed and stored in memory; no external DB setup required.
* **Clear Error Reporting**: Detailed messages help you pinpoint issues in your CSV files or schema.

## Prerequisites

* Java 17 (or later)
* Maven (for build and dependency management)
* PostgreSQL if you want to enable the simulated DB module

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/SajidSadaqa/theater-booking.git
   cd theater-booking
   ```

2. **Build the project**

   ```bash
   mvn clean package
   ```

3. **Run the application**

   ```bash
   java -jar target/theater-booking.jar
   ```

4. **Enter CSV paths** when prompted (one per line), then press Enter on a blank line to start processing.

## CSV File Format

Each layout file must be a CSV with the following columns (in order):

```csv
section,row,seat_start,seat_end,status
```

* **section**: Name of the theater section (e.g., Orchestra, Balcony).
* **row**: Numeric or alphanumeric row identifier.
* **seat\_start**: Starting seat number for this range.
* **seat\_end**: Ending seat number for this range.
* **status**: `available` or `booked`.

### Example

```csv
Orchestra,1,1,5,available
Orchestra,2,1,1,available
Orchestra,2,3,4,available
Orchestra,2,2,2,booked
Balcony,1,1,10,available
```

## Error Handling

* **Duplicate Seats**: If a seat is defined more than once, a `BusinessException` is thrown with details.
* **Missing Columns**: Schema mismatches trigger a `DatabaseException` with the failing SQL statement.
* **Constraint Violations**: Attempting to insert a section or seat that already exists will simulate a database error.

## Customization

* **Add New Sections**: Create additional CSV files under `src/main/resources/data/` following the naming convention `theater-layout-<name>.csv`.
* **Extend Validation**: Modify `CsvParser` and exception classes to add further business rules (e.g., row-naming conventions).

## Contributing

1. Fork this repository.
2. Create a new feature branch (`git checkout -b feature/Name`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature/Name`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

*Happy booking!*
