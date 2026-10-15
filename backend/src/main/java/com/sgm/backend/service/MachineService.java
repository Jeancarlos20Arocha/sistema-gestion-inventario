package com.sgm.backend.service;

import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.repository.MachineTerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineService {

    private final MachineTerminalRepository machineTerminalRepository;

    public MachineService(MachineTerminalRepository machineTerminalRepository) {
        this.machineTerminalRepository = machineTerminalRepository;
    }

    @Transactional
    public MachineTerminal registerIfAbsent(String machineId) {
        return machineTerminalRepository.findByMachineId(machineId)
                .orElseGet(() -> machineTerminalRepository.save(
                        new MachineTerminal(machineId, "Terminal " + machineId)));
    }
}