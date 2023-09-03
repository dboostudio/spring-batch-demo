package kr.dboo.batchdemo.chunkOriented.queryDsl;

import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.entity.Student;
import kr.dboo.batchdemo.chunkOriented.entity.Teacher;
import kr.dboo.batchdemo.chunkOriented.queryDsl.supplier.QuerydslSupplierPagingItemReader;
import kr.dboo.batchdemo.chunkOriented.queryDsl.supplier.QuerydslSupplierRepository;
import kr.dboo.batchdemo.chunkOriented.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class QuerydslSupplierJobConfiguration {
    public static final String JOB_NAME = "querydslSupplierPagingItemReaderJob";

    private final QuerydslSupplierRepository querydslSupplierRepository;

    private final PlatformTransactionManager transactionManager;

    private final JobRepository jobRepository;

    private final TeacherRepository teacherRepository;

    private int chunkSize;

    @Value("${chunkSize:5}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(initQueryDslDatas())
                .next(step())
                .build();
    }

    @Bean
    public Step initQueryDslDatas(){
        return new StepBuilder("initTeacherAndStudent", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    teacherRepository.deleteAll();

                    List<Teacher> teacherList = new ArrayList<>();

                    for (int i = 0; i < 100; i++){
                        String teacherName = "teacher" + i;
                        Teacher teacher = new Teacher(teacherName, "수업 00" + i);
                        for (int j = 0 ; j < 100; j++){
                            Student student = new Student(teacherName + "의 제자" + j);
                            teacher.addStudent(student);
                        }
                        teacherList.add(teacher);
                    }

                    teacherRepository.saveAll(teacherList);

                    return RepeatStatus.FINISHED; }, transactionManager)
                .build();
    }

    @Bean(name = JOB_NAME +"_step")
    public Step step() {
        return new StepBuilder(JOB_NAME +"_step", jobRepository)
                .<Teacher, Teacher>chunk(chunkSize, transactionManager)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public QuerydslSupplierPagingItemReader<Teacher> reader(@Value("#{jobParameters[name]}") String name) {
        return QuerydslSupplierPagingItemReader.<Teacher>builder()
                .pageSize(chunkSize)
                .repositorySupplier((offset, limit) -> querydslSupplierRepository.findAllByPaging(name, offset, limit))
                .build();
    }

    @Bean
    public ItemProcessor<Teacher, Teacher> processor() {
        return teacher -> {
            log.info("students count={}", teacher.getStudents().size());
            return teacher;
        };
    }

    @Bean
    public ItemWriter<Teacher> writer() {
        return list -> {
            for (Teacher teacher: list) {
                log.info("Current Teacher={}", teacher);
            }
        };
    }
}
