package kr.dboo.batchdemo.chunkOriented.queryDsl.supplier;

import kr.dboo.batchdemo.chunkOriented.entity.QTeacher;
import kr.dboo.batchdemo.chunkOriented.entity.Teacher;
import kr.dboo.batchdemo.chunkOriented.entity.Student;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@RequiredArgsConstructor
@Repository
public class QuerydslSupplierRepository {

    private final JPAQueryFactory queryFactory;

    public List<Teacher> findAllByPaging(String name, int offset, int limit) {
        QTeacher teacher = QTeacher.teacher;
        return queryFactory
                .selectFrom(teacher)
//                .where(teacher.name.eq(name))
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Teacher> findAllByNewTxPaging(String name, int offset, int limit) {
        QTeacher teacher = QTeacher.teacher;
        return queryFactory
                .selectFrom(teacher)
                .where(teacher.name.eq(name))
                .offset(offset)
                .limit(limit)
                .fetch();
    }
}

