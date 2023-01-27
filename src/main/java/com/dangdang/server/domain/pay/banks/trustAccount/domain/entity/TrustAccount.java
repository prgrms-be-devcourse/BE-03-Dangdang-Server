package com.dangdang.server.domain.pay.banks.trustAccount.domain.entity;

import com.dangdang.server.domain.common.BaseEntity;
import com.dangdang.server.domain.common.StatusType;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingDepositRequest;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingWithdrawRequest;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
public class TrustAccount extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trust_account_id")
  private Long id;

  @Column(length = 100)
  private String accountNumber;

  @Column(columnDefinition = "INT UNSIGNED")
  @ColumnDefault("0")
  private Integer balance;

  @Column(length = 100)
  private String customer;

  protected TrustAccount() {
  }

  public TrustAccount(String accountNumber, Integer balance, String customer) {
    this.accountNumber = accountNumber;
    this.balance = balance;
    this.customer = customer;
  }

  public TrustAccount(String accountNumber, Integer balance, String customer,
      StatusType statusType) {
    this.accountNumber = accountNumber;
    this.balance = balance;
    this.customer = customer;
    this.status = statusType;
  }

  public void deposit(OpenBankingWithdrawRequest openBankingWithdrawRequest) {
    balance += openBankingWithdrawRequest.amount();
  }

  public void withdraw(OpenBankingDepositRequest openBankingDepositRequest) {
    balance -= openBankingDepositRequest.amount();
  }
}
