package kr.dboo.batchdemo.chunkOriented.repository;

import kr.dboo.batchdemo.chunkOriented.entity.Student;
import kr.dboo.batchdemo.chunkOriented.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
