package com.kitehub.branding.lifecycle.repository;

import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BrandingInstanceStateRepository extends JpaRepository<BrandingInstanceState, UUID> {
}
