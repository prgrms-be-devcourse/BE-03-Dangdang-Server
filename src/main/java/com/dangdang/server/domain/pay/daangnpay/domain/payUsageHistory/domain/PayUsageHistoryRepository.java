package com.dangdang.server.domain.pay.daangnpay.domain.payUsageHistory.domain;

import com.dangdang.server.domain.pay.daangnpay.domain.payUsageHistory.domain.entity.PayUsageHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayUsageHistoryRepository extends JpaRepository<PayUsageHistory, Long> {

  Optional<PayUsageHistory> findByTitle(String usageHistoryTitle);
}
