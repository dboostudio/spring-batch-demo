package kr.dboo.batchdemo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class ParamJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job paramJob() {
        // from program argument
        return new JobBuilder("param", jobRepository)
                .start(paramStep(null)).build();
    }

    @Bean
    @JobScope
    public Step paramStep(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return new StepBuilder("parameterStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is a parameter step");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}