package com.njplastic.njplastic_api.production.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.Machine;

@Repository
public interface MachineRepository extends JpaRepository<Machine, UUID> {

  Optional<Machine> findByCodeAndActiveTrue(String code);

  List<Machine> findAllByActiveTrue();
}