package uz.example.payment.errorcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.example.payment.errorcode.entity.ErrorCategory;
import uz.example.payment.errorcode.entity.ErrorCode;

import java.util.List;

@Repository
public interface ErrorCodeRepository extends JpaRepository<ErrorCode, Integer> {
    List<ErrorCode> findAllByOrderByCodeAsc();
    List<ErrorCode> findAllByCategoryOrderByCodeAsc(ErrorCategory category);
}
