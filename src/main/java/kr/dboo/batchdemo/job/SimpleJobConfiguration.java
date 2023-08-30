package kr.dboo.batchdemo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class SimpleJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job simpleJob() {
        return new JobBuilder("simple", jobRepository).start(finishedStep()).next(continuableStep()).build();
    }


    @Bean
    public Step finishedStep() {
        return new StepBuilder("FINISH_STEP", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>>> Finish Step");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public Step continuableStep() {
        return new StepBuilder("COUNTINUABLE_STEP", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>>> Continuable step <<<");
                    return RepeatStatus.CONTINUABLE;
                }), transactionManager).build();
    }

}