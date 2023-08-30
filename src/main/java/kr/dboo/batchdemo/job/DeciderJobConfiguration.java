package kr.dboo.batchdemo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Random;


@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class DeciderJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job deciderJob() {
        return new JobBuilder("decider", jobRepository)
                .start(startStep())
                .next(decider()) // 홀수 | 짝수 구분
                .from(decider()) // decider의 상태가
                .on("ODD") // ODD라면
                .to(oddStep()) // oddStep로 간다.
                .from(decider()) // decider의 상태가
                .on("EVEN") // ODD라면
                .to(evenStep()) // evenStep로 간다.
                .end() // builder 종료
                .build();
    }

    @Bean
    public Step startStep() {
        return new StepBuilder("startStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>>> Job Start");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public Step evenStep() {
        return new StepBuilder("evenStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>> EVEN <<<");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public Step oddStep() {
        return new StepBuilder("oddStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>> ODD <<<");
                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new OddDecider();
    }

    public static class OddDecider implements JobExecutionDecider {

        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            Random rand = new Random();

            int randomNumber = rand.nextInt(50) + 1;
            log.info("랜덤숫자: {}", randomNumber);

            if(randomNumber % 2 == 0) {
                return new FlowExecutionStatus("EVEN");
            } else {
                return new FlowExecutionStatus("ODD");
            }
        }
    }
}