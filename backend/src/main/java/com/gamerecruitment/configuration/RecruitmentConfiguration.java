package com.gamerecruitment.configuration;

import com.gamerecruitment.recruitment.adapter.persistence.TransactionalRecruitmentCommands;
import com.gamerecruitment.recruitment.application.*;
import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RecruitmentConfiguration {
  @Bean
  CreateRecruitment createRecruitment(RecruitmentRepository repository, Clock clock) {
    return new CreateRecruitment(repository, clock);
  }

  @Bean
  JoinRecruitment joinRecruitment(RecruitmentRepository repository, Clock clock) {
    return new JoinRecruitment(repository, clock);
  }

  @Bean
  CancelParticipation cancelParticipation(RecruitmentRepository repository, Clock clock) {
    return new CancelParticipation(repository, clock);
  }

  @Bean
  ManageRecruitment manageRecruitment(RecruitmentRepository repository, Clock clock) {
    return new ManageRecruitment(repository, clock);
  }

  @Bean
  GetMeetingInfo getMeetingInfo(RecruitmentRepository repository, Clock clock) {
    return new GetMeetingInfo(repository, clock);
  }

  @Bean
  TransactionalRecruitmentCommands recruitmentTransactions(PlatformTransactionManager manager) {
    return new TransactionalRecruitmentCommands(new TransactionTemplate(manager));
  }
}
