package br.com.itau.secure.domain.repository;


import br.com.itau.secure.domain.model.SecureOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SecureOrderRepository extends MongoRepository<SecureOrder, String> {

    List<SecureOrder> findByCustomerId(String customerId);
}

