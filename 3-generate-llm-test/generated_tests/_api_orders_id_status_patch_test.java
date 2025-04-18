```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.ResponseEntity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import com.yourpackage.model.Order;
import com.yourpackage.controller.OrderController;
import com.yourpackage.service.OrderService;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void updateOrderStatus_Success() throws Exception {
        Long orderId = 1L;
        String newStatus = "SHIPPED";
        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);
        updatedOrder.setStatus(newStatus);

        when(orderService.updateOrderStatus(orderId, newStatus))
            .thenReturn(ResponseEntity.ok(updatedOrder));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .param("status", newStatus)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.status", is(newStatus)));
    }

    @Test
    public void updateOrderStatus_NotFound() throws Exception {
        Long orderId = 999L;
        String newStatus = "SHIPPED";

        when(orderService.updateOrderStatus(orderId, newStatus))
            .thenReturn(ResponseEntity.notFound().build());

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .param("status", newStatus)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateOrderStatus_MissingStatusParam() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateOrderStatus_InvalidStatus() throws Exception {
        Long orderId = 1L;
        String invalidStatus = "INVALID_STATUS";

        when(orderService.updateOrderStatus(orderId, invalidStatus))
            .thenThrow(new IllegalArgumentException("Invalid status"));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .param("status", invalidStatus)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
```