package com.example.app.repository;

import com.example.app.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {}
