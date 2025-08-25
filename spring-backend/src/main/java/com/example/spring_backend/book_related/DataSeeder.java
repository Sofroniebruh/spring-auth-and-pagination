package com.example.spring_backend.book_related;

import com.example.spring_backend.book_related.book.Book;
import com.example.spring_backend.book_related.book.BookRepository;
import com.example.spring_backend.book_related.rating.Rating;
import com.example.spring_backend.book_related.rating.RatingRepository;
import com.example.spring_backend.book_related.rating.RatingService;
import com.example.spring_backend.user.User;
import com.example.spring_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final RatingService ratingService;
    public Map<Long, User> userCache = new HashMap<>();

    @Override
    public void run(String... args) throws Exception {

        if (bookRepository.count() > 0) {
            System.out.println("Data already exists, skipping seeding");
            return;
        }

        System.out.println("Starting dataset import...");

        ClassPathResource booksResource = new ClassPathResource("data/books.csv");
        ClassPathResource ratingsResource = new ClassPathResource("data/ratings.csv");

        if (!booksResource.exists()) {
            System.err.println("Books CSV file not found in classpath: data/books.csv");
            return;
        }
        if (!ratingsResource.exists()) {
            System.err.println("Ratings CSV file not found in classpath: data/ratings.csv");
            return;
        }

        int bookCount = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(booksResource.getInputStream()))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (fields.length > 21) {
                    Book book = Book.builder()
                            .title(fields[10].replace("\"", ""))
                            .authors(fields[7].replace("\"", ""))
                            .isbn(fields[5])
                            .originalPublicationYear(parseYear(fields[8]))
                            .imageUrl(fields[21])
                            .build();
                    bookRepository.save(book);

                    bookCount++;
                    if (bookCount % 1000 == 0) {
                        System.out.println("Processed " + bookCount + " books...");
                    }
                } else {
                    System.err.println("Invalid book record, skipping: " + line);
                }
            }
        }

        int ratingCount = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ratingsResource.getInputStream()))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");

                if (fields.length >= 3) {
                    try {
                        Long csvUserId = Long.parseLong(fields[1]);
                        Long bookId = Long.parseLong(fields[0]);
                        Integer ratingValue = Integer.parseInt(fields[2]);

                        User user = userCache.computeIfAbsent(csvUserId, id ->
                                userRepository.save(User.builder().username("User_" + id).isFromDataset(true).build())
                        );

                        Book book = bookRepository.findById(bookId).orElse(null);
                        if (book != null) {
                            Rating rating = Rating.builder()
                                    .user(user)
                                    .book(book)
                                    .rating(ratingValue)
                                    .build();
                            ratingRepository.save(rating);

                            ratingCount++;
                            if (ratingCount % 5000 == 0) {
                                System.out.println("Processed " + ratingCount + " ratings...");
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid rating record, skipping: " + line);
                    }
                } else {
                    System.err.println("Invalid rating record format, skipping: " + line);
                }
            }
        }

        System.out.println("Dataset import completed - " + bookCount + " books, " + ratingCount + " ratings");
    }

    private Integer parseYear(String yearStr) {
        try {
            return (int) Double.parseDouble(yearStr);
        } catch (Exception e) {
            return null;
        }
    }
}