
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