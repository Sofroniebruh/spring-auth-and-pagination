## Spring Backend: Auth, Pagination, and Ratings (Books API)

A production-style Spring Boot 3 REST API demonstrating JWT authentication (access + httpOnly refresh cookie), pagination + sorting, validation, global error handling, and data seeding from CSV. Built with Spring Security, JPA, PostgreSQL, and Maven.

---

### Highlights
- **JWT Auth**: Login/register returns access token (Bearer) and sets secure httpOnly `refresh_token` cookie. `/api/v1/auth/refresh` rotates the access token.
- **Public Books API**: List and search books with pagination and sorting; fetch details by id.
- **Ratings**: Create ratings and view computed average rating per book.
- **Users**: Authenticated profile endpoint and book list management.
- **Global errors**: Consistent error responses and validation messages.
- **Seeding**: Profile `seed` loads `books.csv` and `ratings.csv` on startup.

---

### Tech Stack
- Java 17, Spring Boot 3.5
- Spring Web, Spring Security, Spring Data JPA, Validation
- PostgreSQL, JJWT (`io.jsonwebtoken`)
- Maven
- Docker (multi-stage image)

---

### Project Structure
- `auth/` – controllers, service, records, exceptions for authentication
- `book_related/` – `book/` and `rating/` modules + `DataSeeder`
- `config/` – security and global config (`SecurityConfig`, `ApplicationConfig`, `JwtAuthEntryPoint`, `GlobalExceptionHandler`, `PaginatedResponse`)
- `jwt/` – `JwtService`, `JwtAuthenticationFilter`
- `user/` – domain, service, controller

---

### Environment Variables
Set these for db initialization in the root folder:
- `DB_USER` - DB user
- `DB_PASSWORD` - DB password
- `DB_NAME` - DB name

Set these for local runs and Docker builds in the project folder:
- `DATASOURCE_URL` – e.g. 
- `For docker: jdbc:postgresql://db:5432/<DB_NAME>`
- `For local: jdbc:postgresql://localhost:5432/<DB_NAME>`
- `DATASOURCE_USERNAME` – `DB_USER`
- `DATASOURCE_PASSWORD` – `DB_PASSWORD`
- `JWT_SECRET` – Base64-encoded HMAC key for JJWT (HS256). Example generation:

```bash
openssl rand -base64 64
```

---

### Run with Docker
Multi-stage Dockerfile builds a slim JRE image.

```bash
docker-compose up --build
```

Notes:
- The Dockerfile sets `SPRING_PROFILES_ACTIVE=seed` by default; override if needed.

---

### Authentication Flow
- `POST /api/v1/auth/register` – creates a user, returns `{ token, user }` and sets httpOnly `refresh_token` cookie.
- `POST /api/v1/auth/login` – authenticates, returns `{ token, user }` and sets httpOnly `refresh_token` cookie.
- `POST /api/v1/auth/refresh` – requires `refresh_token` cookie; returns new access token and refresh cookie rotation.

Client usage:
1) Store access token in memory and send `Authorization: Bearer <token>`.
2) Do not handle `refresh_token` manually; browser cookie is httpOnly + secure.
3) On 401/expired access token, call refresh and retry.

---

### API Endpoints

Auth (`/api/v1/auth`):
- `POST /register` – body `{ email, password }` -> `{ token, user }`
- `POST /login` – body `{ email, password }` -> `{ token, user }`
- `POST /refresh` – uses cookie `refresh_token` -> `{ token, user }`

Books (`/api/v1/books`) – public:
- `GET /` – query params: `page` (default 0), `size` (5), `sortBy` (id), `name` (filter by title). Returns `PaginatedResponse<BookResponse>` with average rating.
- `GET /{id}` – by id, returns `BookResponse`.

Ratings (`/api/v1/ratings`) – public create:
- `POST /` – body `{ bookId, rating }` -> `RatingResponse`

Users (`/api/v1/users`) – requires Bearer access token:
- `GET /profile` – returns `UserDto` for authenticated user
- `PUT /{id}/read-books` – body `{ bookId }` -> updated `UserDto`
- `DELETE /{id}/read-books` – body `{ bookId }` -> updated `UserDto`

---

### Pagination Contract
`PaginatedResponse<T>`:
```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 5,
  "totalElements": 100,
  "totalPages": 20,
  "last": false
}
```

---

### Error Handling
`GlobalExceptionHandler` provides consistent responses, e.g. 400 for validation with field errors, 401 for auth errors, 404 for missing entities.

---

### Seed Data
- Activate profile `seed` to import `resources/data/books.csv` and `resources/data/ratings.csv` at startup.
- Large CSVs are streamed; progress is logged. Existing data is not duplicated.


