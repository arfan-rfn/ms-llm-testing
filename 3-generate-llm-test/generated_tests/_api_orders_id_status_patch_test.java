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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    public void updateOrderStatus_Success() throws Exception {
        Long orderId = 1L;
        String newStatus = "SHIPPED";
        Order existingOrder = new Order(orderId, "PENDING");
        Order updatedOrder = new Order(orderId, newStatus);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/orders/{id}/status", orderId)
                .param("status", newStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value(newStatus));
    }

    @Test
    public void updateOrderStatus_OrderNotFound() throws Exception {
        Long orderId = 99L;
        String newStatus = "SHIPPED";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/orders/{id}/status", orderId)
                .param("status", newStatus))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateOrderStatus_MissingStatusParameter() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/orders/{id}/status", orderId))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateOrderStatus_InvalidStatus() throws Exception {
        Long orderId = 1L;
        String invalidStatus = "INVALID_STATUS";
        Order existingOrder = new Order(orderId, "PENDING");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/orders/{id}/status", orderId)
                .param("status", invalidStatus))
                .andExpect(status().isBadRequest());
    }
}
```