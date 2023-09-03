
# How to Use

## How to run batch jobs

- set job name to application.yml
  ~~~
  # application.yml
  spring.batch.job.name: ${JOB_NAME:decider}
  ~~~
  or you can set job name through arguments or env variable.
- run postgresql.yml to run database
  `docker-compose -f postgresql.yml up -d`
- run application
  `./gradlew bootRun`

# Chunk Oriented Batch

### chunk

- chunk : 각 커밋 사이에 처리되는 row 수
- chunk 지향 처리
  - 데이터를 하나씩 읽고, chunk라는 덩어리 단위로 트랜잭션을 묶는다.
  - 실패 시, chunk만큼 롤백된다.

- Reader : 1건씩 데이터를 읽어와 chunk를 구성한다.
- Processor : 구성한 chunk내 데이터를 한개씩 가공한다.
- Writer : Reader일고, Processor가 가공한 데이터를 모아둔 chunk를 일괄 저장한다.

하나의 tasklet은 reader + processor + writer 를 포함한 단계이다.

Job = Steps  
Step = Tasklets  
tasklet = reader + processor + writer

### ItemReader

- Database Reader : CursorItemReader, PagingItemReader
- Cursor 기반 ItemReader 구현체
  - JdbcCursorItemReader
  - HibernateCursorItemReader
  - StoredProcedureItemReader
- Paging 기반 ItemReader 구현체
  - JdbcPagingItemReader
  - HibernatePagingItemReader
  - JpaPagingItemReader

- Hibernate는 cursor기반 database접근이 가능, JPA는 Cursor기반의 Database접근을 지원하지 않는다.
