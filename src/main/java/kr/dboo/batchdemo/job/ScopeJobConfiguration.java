package kr.dboo.batchdemo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class ScopeJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ScopeTasklet scopeTasklet;

    /**
     * 1. 파라미터 사용
     * @JobScope, @StepScope -> parameter 사용을 위하여 스코프 선언
     *
     * SpEL 로 다음과 같이 선언하여 사용한다.
     * @Value("#{jobParameters[파라미터명]}")
     *
     * jobParameters 외에도 jobExecutionContext, stepExecutionContext 등도 SpEL로 사용할 수 있습니다.
     * @JobScope에선 stepExecutionContext는 사용할 수 없고, jobParameters와 jobExecutionContext만 사용할 수 있습니다.
     *
     * 2. Scope의 생명주기
     *
     * @JobScope, @StepScope를 선언하면 Bean의 생성 시점이 해당 Job, Step이 실행되는 시점으로 지연된다.
     * 1) JobParameter 의 late binding이 가능
     * 2) 동일한 컴포넌트 병렬 혹은 동시 사용에 유용
     *
     * @StepScope를 선언한 Tasklet은 서로 다른 Step에서 별도의 빈으로 생성되어 사용된다.
     *
     *
     */

    @Bean
    public Job scopeJob() {
        return new JobBuilder("scope", jobRepository)
                .start(startScopeStep())
                .next(paramStep1(null))
                .next(paramStep2())
                .next(paramStep3())
                .build();
    }


    @Bean
    @JobScope
    public Step startScopeStep() {
        return new StepBuilder("startStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info(" >>>> Job Start");

                    return RepeatStatus.FINISHED;
                }), transactionManager).build();
    }

    @Bean
    @JobScope
    public Step paramStep1(
            // SpEL
            @Value("#{jobParameters[param1]}") String param1
    ) {
        return new StepBuilder("parameterStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is a parameter step");
                    log.info(">>>>> param1 = {}", param1);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step paramStep2() {
        return new StepBuilder("parameterStep", jobRepository)
                .tasklet(scopeStep2Tasklet("param2"), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet scopeStep2Tasklet(
            @Value("#{jobParameters[param2]}") String param2
    ) {
        return (contribution, chunkContext) -> {
            log.info(">>>>> This is a parameter step");
            log.info(">>>>> param2 = {}", param2);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @JobScope
    public Step paramStep3() {
        return new StepBuilder("parameterStep", jobRepository)
                .tasklet(scopeTasklet, transactionManager)
                .build();
    }

}