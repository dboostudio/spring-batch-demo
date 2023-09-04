package kr.dboo.batchdemo.chunkOriented.jdbc;

import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.entity.PayRowMapper;
import kr.dboo.batchdemo.chunkOriented.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class JdbcCursorJobConfiguration {

    public static final String JOB_NAME = "jdbcCursor";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PayRepository payRepository;
    private final DataSource dataSource;
    private final PayRowMapper rowMapper;

    private static final int chunkSize = 10;


    @Bean(JOB_NAME + "_job")
    public Job job(){
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(init())
                .next(step())
                .build();
    }

    @Bean(JOB_NAME + "_init")
    public Step init(){
        return new StepBuilder(JOB_NAME + "_init", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    payRepository.deleteAll();

                    List<Pay> payList = new ArrayList<>();

                    for (int i = 0; i < 100; i++){
                        payList.add(
                                new Pay((long) (1000 * i), "trade" + (Integer) i, LocalDateTime.now())
                        );
                    }

                    payRepository.saveAll(payList);

                    return RepeatStatus.FINISHED;}, transactionManager)
                .build();
    }

    @JobScope
    @Bean(JOB_NAME + "_step")
    public Step step() {
        return new StepBuilder("jdbcCursorReadStep", jobRepository)
                .<Pay, Pay>chunk(chunkSize, transactionManager)
                .reader(reader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean(JOB_NAME + "_reader")
    public JdbcCursorItemReader<Pay> reader(){
        return new JdbcCursorItemReaderBuilder<Pay>()
                .sql("select * from pay")
                .name("payReader")
                .fetchSize(chunkSize)
//                .rowMapper(rowMapper)
                .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
//                .beanRowMapper(Pay.class)
                // beanRowMapper는 매핑할 클래스의 setter를 사용하므로, setter를 만들고 싶지 않으면 직접 rowMapper에 생성자로 매핑해준다.
                .dataSource(dataSource)
                .build();
    }

    @Bean(JOB_NAME + "_writer")
    public ItemWriter<Pay> jdbcCursorItemWriter() {
        return list -> {
            log.info("---chunk---");
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }

    @Bean
    public JdbcBatchItemWriter<Pay> jdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<Pay>()
                .dataSource(dataSource)
                .sql("insert into pay2(amount, tx_name, tx_date_time) values (:amount, :txName, :txDateTime)")
                .beanMapped()
                .build();
    }
}
