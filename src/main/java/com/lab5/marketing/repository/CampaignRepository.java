package com.lab5.marketing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lab5.marketing.domain.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findTop20ByOrderByCreatedAtDesc();
}
