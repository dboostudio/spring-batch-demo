package kr.dboo.batchdemo.chunkOriented.repository;

import kr.dboo.batchdemo.chunkOriented.entity.Pay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayRepository extends JpaRepository<Pay, Long> {
}
