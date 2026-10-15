package com.sgm.backend.repository;

import com.sgm.backend.entity.MachineTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineTerminalRepository extends JpaRepository<MachineTerminal, Long> {

    Optional<MachineTerminal> findByMachineId(String machineId);
}