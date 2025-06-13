package br.com.itau.secure.api.controller;


import br.com.itau.secure.api.mapper.SecureOrderMapper;
import br.com.itau.secure.api.model.SecureOrderInput;
import br.com.itau.secure.api.model.SecureOrderResponse;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.SecureOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/secure-orders")
public class SecureOrderController {

    private final SecureOrderService secureOrderService;

    public SecureOrderController(SecureOrderService secureOrderService) {
        this.secureOrderService = secureOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecureOrderResponse createOrder(@RequestBody SecureOrderInput input) {
        return SecureOrderResponse.fromEntity(secureOrderService.createUpdateSecureOrder(SecureOrderMapper.toEntity(input)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecureOrderResponse> getOrderById(@PathVariable String id) {
        SecureOrder order = secureOrderService.findById(id);
        return ResponseEntity.ok(SecureOrderResponse.fromEntity(order));
    }

    @GetMapping
    public ResponseEntity<List<SecureOrderResponse>> findOrders(
            @RequestParam(name = "customerId", required = true) String customerId) {

        List<SecureOrderResponse> orders = secureOrderService.findByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SecureOrderResponse> cancelOrder(@PathVariable String id) {

        SecureOrder cancelledOrder = secureOrderService.cancelOrder(id);
        return ResponseEntity.ok(SecureOrderResponse.fromEntity(cancelledOrder));
    }

}
