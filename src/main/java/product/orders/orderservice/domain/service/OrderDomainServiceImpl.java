package product.orders.orderservice.domain.service;

import org.springframework.stereotype.Service;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.repository.OrderRepository;

@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    private OrderRepository orderRepository;

    public OrderDomainServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean orderIsConfirmed(Order order) {
        return order.getStatus() == OrderStatus.CONFIRMED;
    }
}
