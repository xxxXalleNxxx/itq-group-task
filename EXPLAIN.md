# Анализ поискового запроса

## EXPLAIN ANALYZE

EXPLAIN (ANALYZE, BUFFERS)   
SELECT * FROM documents   
WHERE status = 'APPROVED'  
  AND author = 'Generator'  
  AND created_at BETWEEN '2026-02-10' AND '2026-02-21'  
ORDER BY created_at DESC  
LIMIT 20;

## Результат выполнения

"Limit  (cost=4.26..4.31 rows=20 width=90) (actual time=0.030..0.032 rows=20 loops=1)"  
"  Buffers: shared hit=2"  
"  ->  Sort  (cost=4.26..4.36 rows=40 width=90) (actual time=0.030..0.030 rows=20 loops=1)"  
"        Sort Key: created_at DESC"  
"        Sort Method: top-N heapsort  Memory: 30kB"  
"        Buffers: shared hit=2"  
"        ->  Seq Scan on documents  (cost=0.00..3.20 rows=40 width=90) (actual time=0.008..0.016 rows=60 loops=1)"  
"              Filter: ((created_at >= '2026-02-10 00:00:00'::timestamp without time zone) AND (created_at <= '2026-02-21 00:00:00'::timestamp without time zone) AND ((status)::text = 'APPROVED'::text) AND ((author)::text = 'Generator'::text))"  
"              Buffers: shared hit=2"  
"Planning:"  
"  Buffers: shared hit=6"  
"Planning Time: 0.178 ms"  
"Execution Time: 0.045 ms"  

---

## Анализ производительности

Помимо отдельных индексов для фильтрациии  
-- CREATE INDEX idx_documents_status ON documents(status)  
-- CREATE INDEX idx_documents_author ON documents(author)  
-- CREATE INDEX idx_documents_created_at ON documents(created_at)  

создал композитный  
-- CREATE INDEX idx_documents_status_author_created 
ON documents(status, author, created_at DESC)  

Он для уже более заполненой таблицы, потому что тестировал таблицу при 60 строчках, поэтому он все равно линейно шел через 

Без индексов по дате создания было 0.08ms , щас 0.045 ms
