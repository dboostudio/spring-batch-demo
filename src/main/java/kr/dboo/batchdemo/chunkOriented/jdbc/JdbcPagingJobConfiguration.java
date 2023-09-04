package kr.dboo.batchdemo.chunkOriented.jdbc;

import jakarta.annotation.PostConstruct;
import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import kr.dboo.batchdemo.chunkOriented.entity.PayRowMapper;
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
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class JdbcPagingJobConfiguration {

    public static final String JOB_NAME = "jdbcPaging";

    private final JobRepository jobRepository;
    private PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;
    private final PayRowMapper rowMapper;

    private static final int chunkSize = 10;

    @PostConstruct
    public void postConstruct(){
        this.platformTransactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Bean(JOB_NAME + "_job")
    public Job job() throws Exception {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(step())
                .build();
    }

    @JobScope
    @Bean(JOB_NAME + "_step")
    public Step step() throws Exception {
        return new StepBuilder("jdbcPagingStep", jobRepository)
                .<Pay, Pay>chunk(10, platformTransactionManager)
                .reader(reader())
                .writer(writer())
                .build();
    }

    @Bean(JOB_NAME + "_reader")
    public JdbcPagingItemReader<Pay> reader() throws Exception {
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

    @Bean(JOB_NAME + "_writer")
    public ItemWriter<Pay> writer() {
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
