INSERT INTO lab.nplus1_authors (id, name)
SELECT author_id, 'Author ' || author_id
FROM generate_series(11, 200) AS author_id
ON CONFLICT (id) DO NOTHING;

INSERT INTO lab.nplus1_books (author_id, title)
SELECT author_id,
       'Author ' || author_id || ' Book ' || book_no
FROM generate_series(1, 200) AS author_id
CROSS JOIN generate_series(1, 20) AS book_no
WHERE NOT EXISTS (
    SELECT 1
    FROM lab.nplus1_books b
    WHERE b.author_id = author_id
      AND b.title = 'Author ' || author_id || ' Book ' || book_no
);

SELECT setval(pg_get_serial_sequence('lab.nplus1_authors', 'id'), COALESCE((SELECT MAX(id) FROM lab.nplus1_authors), 1));
SELECT setval(pg_get_serial_sequence('lab.nplus1_books', 'id'), COALESCE((SELECT MAX(id) FROM lab.nplus1_books), 1));
