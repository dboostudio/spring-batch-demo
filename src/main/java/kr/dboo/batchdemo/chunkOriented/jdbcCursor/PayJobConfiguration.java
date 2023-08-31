package kr.dboo.batchdemo.chunkOriented.jdbcCursor;

import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PayJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PayRepository payRepository;
    private final DataSource dataSource;
    private final PayRowMapper rowMapper;

    @Bean
    public Job jdbcCursorItemReaderJob(){
        return new JobBuilder("jdbcCursor", jobRepository)
                .start(initPays())
                .next(jdbcCursorItemReaderStep())
                .build();
    }

    @Bean
    private Step initPays(){
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
    private Step jdbcCursorItemReaderStep() {
        return new StepBuilder("jdbcCursorReadStep", jobRepository)
                .<Pay, Pay>chunk(10, transactionManager)
                .reader(jdbcCursorItemReader())
                .writer(jdbcCursorItemWriter())
                .build();
    }

    private ItemReader<Pay> jdbcCursorItemReader(){
        return new JdbcCursorItemReaderBuilder<Pay>()
                .sql("select * from pay")
                .name("payReader")
                .rowMapper(rowMapper)
//                .beanRowMapper(new BeanPropertyRowMapper<>(Pay.class).getMappedClass())
                .dataSource(dataSource)
                .build();
    }

    private ItemWriter<Pay> jdbcCursorItemWriter() {
        return list -> {
            log.info("---chunk---");
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }
}
