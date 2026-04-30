INSERT INTO lab.nplus1_books (author_id, title)
SELECT author_seq.author_id,
       'Author ' || author_seq.author_id || ' Book ' || book_seq.book_no
FROM generate_series(1, 200) AS author_seq(author_id)
CROSS JOIN generate_series(1, 20) AS book_seq(book_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM lab.nplus1_books existing
    WHERE existing.author_id = author_seq.author_id
      AND existing.title = 'Author ' || author_seq.author_id || ' Book ' || book_seq.book_no
);

SELECT setval(pg_get_serial_sequence('lab.nplus1_books', 'id'), COALESCE((SELECT MAX(id) FROM lab.nplus1_books), 1));

