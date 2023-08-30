package kr.dboo.batchdemo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
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
public class ConditionJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job conditionJob() {
        return new JobBuilder("condition", jobRepository)
                .start(startConditionStep())
                    .on("FAILED")
                    .to(conditionStep2())
                    .on("*")
                    .end()
                .from(startConditionStep())
                    .on("*")
                    .to(conditionStep1())
                    .next(conditionStep2())
                    .on("*")
                    .end()
                .end()
                .build();
    }


    @Bean
    public Step startConditionStep() {
        return new StepBuilder("startStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>>> Job Start");

                    contribution.setExitStatus(ExitStatus.COMPLETED);
                    contribution.setExitStatus(ExitStatus.EXECUTING);
                    contribution.setExitStatus(ExitStatus.STOPPED);
                    contribution.setExitStatus(ExitStatus.FAILED);
                    contribution.setExitStatus(ExitStatus.NOOP);
                    contribution.setExitStatus(ExitStatus.UNKNOWN);

                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public Step conditionStep1() {
        return new StepBuilder("conditionStep1", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>> 1 <<<");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public Step conditionStep2() {
        return new StepBuilder("conditionStep2", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>> 2 <<<");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }
}