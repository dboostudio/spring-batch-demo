package kr.dboo.batchdemo.chunkOriented.jpa;

import jakarta.persistence.EntityManagerFactory;
import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.entity.Pay2;
import kr.dboo.batchdemo.chunkOriented.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaPagingJobConfiguration {

    public static final String JOB_NAME = "jpaPaging";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final PayRepository payRepository;
    private static final int chunkSize = 20;
    private static final int pageSize = 5;

    @Bean(JOB_NAME + "_job")
    public Job jpaPagingJob(){
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(init())
                .next(step())
                .build();
    }

    @Bean(JOB_NAME + "_init")
    @JobScope
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

                    return RepeatStatus.FINISHED; }, transactionManager)
                .build();
    }

    @Bean(JOB_NAME + "_paging_step")
    @JobScope
    public Step step(){
        return new StepBuilder("jpaPagingStep", jobRepository)
                .<Pay, Pay2>chunk(chunkSize, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(JOB_NAME + "_reader")
    public JpaPagingItemReader<Pay> reader(){
        return new JpaPagingItemReaderBuilder<Pay>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(pageSize)
                .queryString("SELECT p FROM Pay p WHERE amount >= 30000")
                .build();
    }

    @Bean(JOB_NAME + "_printer")
    ItemWriter<Pay> printer() {
        return list -> {
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }

    @Bean(JOB_NAME + "_processor")
    public ItemProcessor<Pay, Pay2> processor() {
        return pay -> {
            Pay2 pay2 = new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
            log.info("processing..." + pay2);
            return pay2;
        };
    }

    @Bean (JOB_NAME + "_writer_jpa")
    public JpaItemWriter<Pay2> writer() {
        JpaItemWriter<Pay2> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }

}
