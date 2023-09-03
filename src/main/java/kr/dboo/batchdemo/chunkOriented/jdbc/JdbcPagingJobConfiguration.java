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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class JdbcPagingJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PayRepository payRepository;
    private final DataSource dataSource;
    private final PayRowMapper rowMapper;

    private static final int chunkSize = 10;

    @Bean
    public Job jdbcPagingJob() throws Exception {
        return new JobBuilder("jdbcPaging", jobRepository)
                .start(initPaysPaging())
                .next(jdbcPagingStep())
                .build();
    }

    @Bean
    @JobScope
    private Step initPaysPaging(){
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
    private Step jdbcPagingStep() throws Exception {
        return new StepBuilder("jdbcPagingStep", jobRepository)
                .<Pay, Pay>chunk(10, transactionManager)
                .reader(jdbcPagingItemReader())
                .writer(jdbcPagingItemWriter())
                .build();
    }

    @Bean
    private JdbcPagingItemReader<Pay> jdbcPagingItemReader() throws Exception {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("amount", 2000);

        return new JdbcPagingItemReaderBuilder<Pay>()
                .pageSize(chunkSize)
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .parameterValues(parameterValues)
                .queryProvider(createQueryProvider())
                .name("payPagingReader")
//                .beanRowMapper(Pay.class)
                .rowMapper(rowMapper)
                .build();
    }

    private ItemWriter<Pay> jdbcPagingItemWriter() {
        return list -> {
            log.info("---chunk---");
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }

    public PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource); // Database에 맞는 PagingQueryProvider를 선택하기 위해
        queryProvider.setSelectClause("id, amount, tx_name, tx_date_time");
        queryProvider.setFromClause("from pay");
        queryProvider.setWhereClause("where amount >= 1000");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.ASCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }
}
