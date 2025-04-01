```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void testGetOrdersByCustomerName_Success() throws Exception {
        // Arrange
        String customerName = "JohnDoe";
        Order order1 = new Order(1L, customerName, "Product1", 2);
        Order order2 = new Order(2L, customerName, "Product2", 1);
        List<Order> orders = Arrays.asList(order1, order2);

        when(orderService.getOrdersByCustomerName(customerName))
            .thenReturn(new ResponseEntity<>(orders, HttpStatus.OK));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/{customerName}", customerName))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].customerName", is(customerName)))
               .andExpect(jsonPath("$[1].customerName", is(customerName)));
    }

    @Test
    public void testGetOrdersByCustomerName_EmptyResult() throws Exception {
        // Arrange
        String customerName = "NonExistentCustomer";
        
        when(orderService.getOrdersByCustomerName(customerName))
            .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/{customerName}", customerName))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetOrdersByCustomerName_InternalServerError() throws Exception {
        // Arrange
        String customerName = "JohnDoe";
        
        when(orderService.getOrdersByCustomerName(customerName))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/{customerName}", customerName))
               .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetOrdersByCustomerName_InvalidCustomerName() throws Exception {
        // Arrange
        String customerName = " ";

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/{customerName}", customerName))
               .andExpect(status().isBadRequest());
    }
}

class Order {
    private Long id;
    private String customerName;
    private String productName;
    private int quantity;

    public Order(Long id, String customerName, String productName, int quantity) {
        this.id = id;
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
}

// Mock service interface
interface OrderService {
    ResponseEntity<List<Order>> getOrdersByCustomerName(String customerName);
}
```