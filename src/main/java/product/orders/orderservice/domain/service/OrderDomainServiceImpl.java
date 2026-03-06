package product.orders.orderservice.domain.service;

import org.springframework.stereotype.Service;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderStatus;

@Service
public class OrderDomainServiceImpl implements OrderDomainService {


    @Override
    public boolean orderIsConfirmed(Order order) {
        return order.getStatus() == OrderStatus.CONFIRMED;
    }
}
