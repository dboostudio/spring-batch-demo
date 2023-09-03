package kr.dboo.batchdemo.chunkOriented.jdbc;

import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.entity.Pay2;
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

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PayRepository payRepository;
    private final DataSource dataSource;
    private final PayRowMapper rowMapper;

    private static final int chunkSize = 10;


    @Bean
    public Job jdbcCursorJob(){
        return new JobBuilder("jdbcCursor", jobRepository)
                .start(initPaysCursor())
                .next(jdbcCursorStep())
                .build();
    }

    @Bean
    private Step initPaysCursor(){
        return new StepBuilder("initPays", jobRepository)
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
    @Bean
    private Step jdbcCursorStep() {
        return new StepBuilder("jdbcCursorReadStep", jobRepository)
                .<Pay, Pay>chunk(chunkSize, transactionManager)
                .reader(jdbcCursorItemReader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    private JdbcCursorItemReader<Pay> jdbcCursorItemReader(){
        return new JdbcCursorItemReaderBuilder<Pay>()
                .sql("select * from pay")
                .name("payReader")
                .fetchSize(chunkSize)
//                .rowMapper(rowMapper)
                .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
//                .beanRowMapper(new BeanPropertyRowMapper<>(Pay.class).getMappedClass())
//                .beanRowMapper(Pay.class)
                // beanRowMapper는 매핑할 클래스의 setter를 사용하므로, setter를 만들고 싶지 않으면 직접 rowMapper에 생성자로 매핑해준다.
                .dataSource(dataSource)
                .build();
    }

    @Bean
    private ItemWriter<Pay> jdbcCursorItemWriter() {
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
