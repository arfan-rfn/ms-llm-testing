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
    private OrderController orderController;

    @Test
    public void testGetOrdersByCustomerName_Success() throws Exception {
        Order order1 = new Order(1L, "John Doe", "Product A");
        Order order2 = new Order(2L, "John Doe", "Product B");
        List<Order> orders = Arrays.asList(order1, order2);

        when(orderController.getOrdersByCustomerName("John Doe"))
            .thenReturn(new ResponseEntity<>(orders, HttpStatus.OK));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/John Doe"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].customerName", is("John Doe")))
            .andExpect(jsonPath("$[1].customerName", is("John Doe")));
    }

    @Test
    public void testGetOrdersByCustomerName_EmptyResult() throws Exception {
        when(orderController.getOrdersByCustomerName("Unknown"))
            .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/Unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetOrdersByCustomerName_InvalidCustomerName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/ "))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetOrdersByCustomerName_InternalServerError() throws Exception {
        when(orderController.getOrdersByCustomerName("ErrorCase"))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/ErrorCase"))
            .andExpect(status().isInternalServerError());
    }
}

class Order {
    private Long id;
    private String customerName;
    private String productName;

    public Order(Long id, String customerName, String productName) {
        this.id = id;
        this.customerName = customerName;
        this.productName = productName;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }
}
```