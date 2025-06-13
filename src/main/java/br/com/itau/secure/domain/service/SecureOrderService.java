package br.com.itau.secure.domain.service;

import br.com.itau.secure.api.model.SecureOrderResponse;
import br.com.itau.secure.domain.exception.ResourceNotFoundException;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.repository.SecureOrderRepository;
import br.com.itau.secure.domain.service.status.SecureOrderStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class SecureOrderService {
    private final SecureOrderRepository secureOrderRepository;
    private final SecureOrderStatusService secureOrderStatusService;

    public SecureOrderService(SecureOrderRepository secureOrderRepository,SecureOrderStatusService secureOrderStatusService) {
        this.secureOrderRepository = secureOrderRepository;
        this.secureOrderStatusService = secureOrderStatusService;
    }

    public SecureOrder findById(String id) {
        return secureOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SecureOrder not found with id: " + id));
    }

    
    public List<SecureOrderResponse> findByCustomerId(String customerId) {
        return secureOrderRepository.findByCustomerId(customerId)
                .stream()
                .map(SecureOrderResponse::fromEntity)
                .toList();
    }


    public SecureOrder createUpdateSecureOrder(SecureOrder secureOrder) {
        SecureOrder secureOrderSaved = secureOrderRepository.save(secureOrder);
        secureOrderStatusService.sendStatusUpdateToQueueProcessing(secureOrder, null);
        return secureOrderSaved;
    }


    public SecureOrder cancelOrder(String id) {
        SecureOrder securerOrder = this.findById(id);
        securerOrder.getStatus().moveToCancel(securerOrder);
        return this.createUpdateSecureOrder(securerOrder);
    }
}
