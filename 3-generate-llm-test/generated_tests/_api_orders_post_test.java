```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.ResponseEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderController orderController;

    @Test
    public void testCreateOrder_Success() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerName("John Doe");
        order.setTotalAmount(100.50);

        ResponseEntity<Order> responseEntity = ResponseEntity.ok(order);
        when(orderController.createOrder(any(Order.class))).thenReturn(responseEntity);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.totalAmount").value(100.50));
    }

    @Test
    public void testCreateOrder_ValidationError() throws Exception {
        Order invalidOrder = new Order();
        invalidOrder.setCustomerName("");
        invalidOrder.setTotalAmount(-10.0);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateOrder_InternalServerError() throws Exception {
        Order order = new Order();
        order.setCustomerName("Jane Smith");
        order.setTotalAmount(200.75);

        when(orderController.createOrder(any(Order.class)))
                .thenThrow(new RuntimeException("Internal Server Error"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isInternalServerError());
    }
}
```